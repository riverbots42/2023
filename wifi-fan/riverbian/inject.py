#!/usr/bin/env python3

"""
	Script to inject the Riverbots code into a standard Armbian image.

	(C) 2022 by BJ Black <bj@wjblack.com>
"""

import argparse
import glob
import os
import os.path
import re
import shutil
import subprocess
import sys
import time

EXPANSION_PCT = 200
MIN_SIZE_MiB = 2048

def usage(msg=None):
	"""
		Print a simple usage message and exit, maybe with a preceeding error.
	"""
	if msg is not None:
		print(msg)
	print("Usage: %s <Armbian...img.xz>" % sys.argv[0])
	sys.exit(0)

def expandXzFile(inname):
	"""
		Expand an Armbian image so we can work on it.  The new file
		will be named Riverbian_* instead of Armbian_*.
	"""
	matches = re.search(r'^Armbian_(.*)\.xz$', inname)
	if matches is None:
		return None

	outname = "Riverbian_%s" % matches.group(1)
	with open(outname, "wb") as fd:
		proc = subprocess.Popen(["xzcat", inname], stdout=fd, stderr=subprocess.PIPE)
		print("Expanding...", flush=True, end="")
		_, err = proc.communicate()
		if proc.returncode != 0:
			print(err.decode('utf-8'))
			raise ValueError("Got return code %d when decompressing %s" % (proc.returncode, inname))
		print("  Done!")
		oldsize = fd.tell()
		newsize = int(oldsize * EXPANSION_PCT / 100)
		newsize -= newsize % 1048576
		if newsize < MIN_SIZE_MiB:
			newsize = MIN_SIZE_MiB
		print("Image is %0.1f MiB, expanding to %0.1f MiB..." % (oldsize/1048576, newsize/1048576))
		fd.seek(newsize-1024)
		fd.write(bytearray(1024))
	return outname

def disableAutomount():
	"""
		Turn off the disk automounter, which can get in the way of us mounting/unmounting images.
	"""
	proc = subprocess.Popen(["systemctl", "mask", "udisks2"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	proc.communicate()
	proc = subprocess.Popen(["systemctl", "stop", "udisks2"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	proc.communicate()

def expandAndMount(fname):
	"""
		Make the partition/filesystem bigger to match the size of the image file.
	"""

	# First up, disable any automounter that might be running (assuming debianish distros like Ubuntu).
	disableAutomount()

	# Now resize the partition to fill the image space.
	proc = subprocess.Popen(["parted", fname, "unit s", "print"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	out, err = proc.communicate()
	partre = re.compile(r'^\s*(\d+)\s+(\d+)s\s+(\d+)s\s+')
	(partno, starts, ends) = (0, 0, 0)
	for line in out.decode('utf-8').split("\n"):
		matches = partre.search(line)
		if matches is not None:
			partno = int(matches.group(1))
			starts = int(matches.group(2))
			ends = int(matches.group(3))
			break
	if partno == 0:
		raise ValueError("Couldn't determine partition 1 start!")
	proc = subprocess.Popen(["parted", fname, "resizepart 1 -1s"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	out, err = proc.communicate()
	if proc.returncode != 0:
		print(err.decode('utf-8'))
		raise ValueError("Couldn't resize partition 1 for replacement!")

	# Now resize the filesystem to match the new partition size.
	proc = subprocess.Popen(["kpartx", "-av", fname], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	out, err = proc.communicate()
	devre = re.compile(r'add map (\S+)')
	loopdev = None
	for line in out.decode('utf-8').split("\n"):
		matches = devre.search(line)
		if matches is not None:
			loopdev = matches.group(1)
	if loopdev is None:
		print(err.decode('utf-8'))
		raise ValueError("Couldn't mount partition table via kpartx!")
	proc = subprocess.Popen(["resize2fs", "/dev/mapper/%s" % loopdev], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	print("Expanding filesystem to fill the additional space...", flush=True, end="")
	out, err = proc.communicate()
	print("  Done!")

	# Now mount it on dst/ so we can put stuff in there.
	print("Mounting...", flush=True, end="")
	os.makedirs("dst", exist_ok=True)
	proc = subprocess.Popen(["mount", "/dev/mapper/%s" % loopdev, "dst/"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
	out, err = proc.communicate()
	if proc.returncode != 0:
		print(err.decode('utf-8'))
		raise ValueError("Got an error when mounting %s on %s.  This is BAD: You'll need to clean up the loopbacks manually!" % (fname, loopdev))

	# Now mount proc and friends so our little script has all it needs to succeed.
	print("Mounting helper dirs...", flush=True, end="")
	dirs = { "proc": "proc", "sysfs": "sys", "devtmpfs": "dev", "devpts": "dev/pts" }
	for key in dirs:
		print(" %s" % key, flush=True, end="")
		subprocess.run(["mount", "-t", key, "none", "dst/%s" % dirs[key]])
	print(".  Done!")
	return loopdev

def cleanup():
	"""
		Clean up any dmsetup/losetup either from a previous run or at the end of this one.
	"""

	# Step 1:  Unmount dst/ if it's currently mounted.  Assume that it is if there's a dst/bin directory.
	# List of paths to umount and what files to test to see if they're still mounted.
	dirs_to_clean = {
		"/dev/pts": "/dev/pts/ptmx",
		"/dev": "/dev/tty",
		"/sys": "/sys/class",
		"/proc": "/proc/cmdline",
		"/": "/bin",
	}
	print("Unmounting directories...")
	attempt = 1
	while attempt < 10:
		cleaned = 0
		for d in dirs_to_clean:
			target = d
			while len(target) > 1 and target[0] == "/":
				target = target[1:]
			target = "dst/" + target
			tester = dirs_to_clean[d]
			while len(tester) > 1 and tester[0] == "/":
				tester = tester[1:]
			tester = "dst/" + tester
			try:
				os.stat(tester)
				print("-> %s" % d)
				subprocess.run(["umount", target])
				time.sleep(0.5)
				cleaned += 1
			except:
				pass
		if cleaned == 0:
			break
		time.sleep(1.0)
		attempt += 1
	if attempt >= 10:
		print("Couldn't unmount all subdirs under dst/.  You'll need to manually clean up.")
		return False
	print("-> Done!")

	# Step 2:  If dst/ exists, try to remove it.
	has_dst = False
	try:
		os.stat("dst")
		has_dst = True
	except:
		pass

	if has_dst:
		print("Removing dst/...")
		try:
			os.rmdir("dst")
		except:
			print("Couldn't remove dst/")
			return False
		print("-> Done!")

	# Step 3: Clean up dmsetup to make sure there are no loop* entries in it.
	print("Attempting to remove anything in dmsetup...")
	attempt = 1
	while attempt < 10:
		ents = glob.glob("/dev/mapper/loop*")
		if len(ents) == 0:
			break
		for ent in ents:
			loopdev = ent.split("/")[-1]
			subprocess.run(["dmsetup", "remove", loopdev])
		time.sleep(1.0)
		attempt += 1
	if attempt >= 10:
		print("Couldn't remove all the dmsetup entries.  Maybe something's mounted?")
		return False
	print("-> Done!")

	# Step 4: Now clean up losetup to make sure no Riverbian entries are in it.
	print("Attempting to remove any /dev/loop entries pointing to something with 'Riverbian' in it...")
	loopre = re.compile(r'^\s*(/dev/loop\d+)\s.*Riverbian')
	attempt = 1
	while attempt < 10:
		proc = subprocess.Popen(["losetup"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
		out, err = proc.communicate()
		ents = []
		for line in out.decode('utf-8').split("\n"):
			matches = loopre.search(line)
			if matches is not None:
				ents.append(matches.group(1))
		if len(ents) == 0:
			break
		for ent in ents:
			print("-> Trying to clean up %s." % ent)
			subprocess.run(["losetup", "-d", ent])
		time.sleep(1.0)
		attempt += 1
	if attempt >= 10:
		print("Couldn't totally clean up loopback entries.  Maybe something's mounted?")
		return False
	print("-> Done!")

	print("Cleanup complete!")
	return True

# BEGIN MAIN ROUTINE

parser = argparse.ArgumentParser(description="Create a Riverbian OS image from an Armbian one.")
parser.add_argument('filename', metavar='filename', type=str, nargs=1, help="Your Armbian .xz file from armbian.com")
args = parser.parse_args()

if not cleanup():
	print("*************************************************************************************")
	print("ERROR: Couldn't preclean.  You'll need to either reboot (if things are really broken)")
	print("or use umount/dmsetup/losetup to clean extraneous mounts/maps.")
	print("*************************************************************************************")
	sys.exit(-1)

imagefile = expandXzFile(args.filename[0])
print("Resulting file was %s." % imagefile)
loopdev = expandAndMount(imagefile)

# Files to copy before we have stuff installed.
files_to_copy = {
	"/usr/bin/qemu-arm-static": "/usr/bin/qemu-arm",
	"riverbian.sh": "/riverbian.sh",
	"gotty.service": "/etc/systemd/system/gotty.service",
	"filebrowser.service": "/etc/systemd/system/filebrowser.service",
	"nginx-riverbots.conf": "/etc/nginx/sites-available/riverbots",
	"interfaces": "/etc/network/interfaces",
	"hostapd.conf": "/etc/hostapd/hostapd.conf",
	"dnsmasq-wifi.conf": "/etc/dnsmasq.d/wifi.conf",
	"wifi.py": "/usr/bin/wifi.py",
	"index.jsp": "/var/lib/tomcat9/webapps/ROOT/index.jsp",
}

print("Copying files...")
try:
	for src in files_to_copy:
		dst = files_to_copy[src]
		while dst[0] == "/":
			dst = dst[1:]
		dst = "dst/" + dst
		print("    %s -> %s" % (src, dst), flush=True, end="")
		os.makedirs(os.path.dirname(dst), exist_ok=True)
		shutil.copyfile(src, dst)
		print(". OK!")
	print("Adjusting permissions...")
	os.chmod("dst/usr/bin/qemu-arm", 0o755)
	os.chmod("dst/riverbian.sh", 0o755)
	os.chmod("dst/usr/bin/wifi.py", 0o755)
except:
	cleanup()
	print("**********************************************************************************************")
	print("ERROR:  Couldn't copy files to target.  Can't continue (maybe 'apt install qemu-user-static'?)")
	print("**********************************************************************************************")
	sys.exit(-1)

print("Running the Riverbian installer inside the image.")
try:
	cp = subprocess.run(["chroot", "dst", "/usr/bin/qemu-arm", "-cpu", "cortex-a7", "/bin/bash", "riverbian.sh"])
except:
	cleanup()
	print("***************************************************")
	print("ERROR:  When running riverbian.sh.  Can't continue.")
	print("***************************************************")
	sys.exit(-1)
if cp.returncode != 0:
	cleanup()
	print("********************************************************")
	print("ERROR:  Status %d running riverbian.sh.  Can't continue." % cp.returncode)
	print("********************************************************")
	sys.exit(-1)

cleanup()
print("Inject process complete!")
