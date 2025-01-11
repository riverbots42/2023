#!/usr/bin/python3

"""
    Script to monitor the VPN connection and reboot if it's down long enough.
"""

import subprocess
import sys
import time

def ping(ip):
    proc = subprocess.Popen(["ping", "-c", "2", ip], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    sout, serr = proc.communicate()
    return proc.returncode == 0

def log(msg):
    if msg[-1] != "\n":
        msg += "\n"
    sys.stdout.write(msg)
    sys.stdout.flush()

MIN_FAIL = 2
MAX_FAIL = 5
failures = 0
log("Starting up...")
while True:
    if ping("10.68.0.1"):
        log("Got a response from the cloud VPN server.  Going to sleep...")
        failures = 0
    else:
        failures += 1
        log("No response.  Fail count now %d." % failures)
        if failures >= MAX_FAIL:
            log("Rebooting...")
            subprocess.Popen(["/sbin/reboot"], stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
        elif failures >= MIN_FAIL:
            log("Restarting wan interface...")
            subprocess.run(["/usr/sbin/ifdown", "wan"])
            sys.stdout.flush()
            time.sleep(2)
            subprocess.run(["/usr/sbin/ifup", "wan"])
            sys.stdout.flush()
    time.sleep(60)
    print("Waking up...")
