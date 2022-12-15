#!/usr/bin/env python3

"""
	Script to prompt the user for Wifi information and set hostapd.conf and friends accordingly.

	(C)2022 by BJ Black <bj@wjblack.com>
"""

import getpass
import re
import subprocess
import sys

print("This script sets the AP name and password for this device.")
print("Press Ctrl-C if you don't intend to do this.")
print("")

# Get the input from the user.
validre = re.compile(r'^\S\S+$')
ap = input("   Enter AP name you wish to use: ")
if not validre.match(ap):
	print("No spaces, at least 2 chars.")
	raise ValueError("Invalid AP name: " + ap)
pw1 = getpass.getpass(prompt="   Enter the password for this AP: ")
pw2 = getpass.getpass(prompt="Re-Enter the password for this AP: ")
if len(pw1) < 8:
	raise ValueError("Invalid password.  Must be >= 8 chars for WPA.")
if pw1!=pw2:
	raise ValueError("Passwords don't match!")

# Spit out what were about to do.
print("")
print("Configuration:")
print("")
print("    AP:   " + ap)
print("    Pass: " + ("*" * len(pw1)))
print("")
print("This script will reboot the machine after setting things up.  OK? [Y/n]", flush=True, end="")

if input().strip().lower() == "n":
	print("Aborting per user.")
	sys.exit(0)

# Suck in the existing hostap configuration for the wifi
ssidpassre = re.compile(r'^\s*(ssid|wpa_passphrase)')
lines = []
with open("/etc/hostapd/hostapd.conf") as fd:
	for l in fd:
		line = l.rstrip()
		matches = ssidpassre.search(line)
		if matches is None:
			lines.append(line)

# Spit the config back out with the new settings.
lines.append("ssid=" + ap)
lines.append("wpa_passphrase=" + pw1)
with open("/etc/hostapd/hostapd.conf", "w") as fd:
	fd.write("\n".join(lines))

subprocess.run(["/sbin/reboot"])
