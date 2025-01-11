#!/usr/bin/python3

"""
    Script to generate the contents of
    /etc/udev/rules.d/70-persistent.net.rules, to set up whichever interface.

    Notes:
    - We REALLY REALLY want there to be a WAN on DHCP+has_internet, a WiFi, and
      a LAN interface.  If we don't have all three, we barf.
    - We reboot 5 mins after invocation if there are changes to be made, which
      generally there shouldn't be.
    - We set our WAN MAC to the OUI of Lockheed Martin Tactical Systems for
      funsies.
"""

import glob
import os
import random
import re
import subprocess

MY_MAC_OUI = "00:07:ef:be:ef"

def make_mac():
    return "%s:%02x" % (MY_MAC_OUI, 2+random.randrange(250))

def get_device(intf):
    """
        Fetch the PCI ID for KERNELS== to put in the udev rule.  Returns
        the DEVICE info and True/False depending on whether it's wifi or not.
        Or None if no KERNELS== should exist.
    """
    proc = subprocess.Popen(
        ["udevadm", "info", "-a", "-p", "/sys/class/net/%s" % intf],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )
    sout, serr = proc.communicate()
    kernels = None
    drivers = None
    kernre = re.compile(r'^\s*KERNELS\s*==\s*(\S+)')
    driverre = re.compile(r'^\s*DRIVERS\s*==\s*(\S+)')
    for line in sout.decode("utf-8").split("\n"):
        matches = kernre.search(line)
        if matches is not None:
            kernels = matches.group(1)
        matches = driverre.search(line)
        if matches is not None:
            drivers = matches.group(1)
        if kernels is not None and drivers is not None:
            break
    is_wifi = False
    try:
        os.stat("/sys/class/net/%s/phy80211" % intf)
        is_wifi = True
    except:
        # Not a wifi adapter.
        is_wifi = False

    if kernels is None or drivers is None:
        return None, None, False
    return kernels.replace("\"", ""), drivers.replace("\"", ""), is_wifi

def get_wan_intf():
    """
        Use the routing table to figure out who our default router is.
        Returns None or (interface, ip, default_gateway)
    """
    proc = subprocess.Popen(
        ["ip", "route", "get", "1.1.1.1"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )
    sout, serr = proc.communicate()
    # Looks like: 1.1.1.1 via 192.168.1.1 dev enp1s0 src 192.168.1.178
    routere = re.compile(r'\s*1\.1\.1\.1\s+via\s+(\S+)\s+dev\s+(\S+)\s+src\s+(\S+)')
    for line in sout.decode("utf-8").split("\n"):
        matches = routere.search(line)
        if matches is not None:
            return matches.group(2), matches.group(3), matches.group(1)
    return None

def get_interface_map():
    wanintf, wanip, wangw = get_wan_intf() or (None, None, None)
    if wanintf is None:
        raise ValueError("Couldn't determine WAN interface!")
    print("WAN detected on %s (%s)!" % (wanintf, wanip))
    interfaces = {
        "wan": None,
    }
    for d in glob.glob("/sys/class/net/*"):
        intf = d.split("/")[-1]
        print("Considering interface %s..." % intf, end="")
        kernels, drivers, is_wifi = get_device(intf) or (None, None, False)
        if kernels is None:
            print("  Not an ethernet or WiFi interface.")
            continue
        if is_wifi:
            print("  Is a WiFi interface: ", end="")
            if drivers == "ath9k":
                print("AP")
                interfaces["wifi-ap"] = drivers
            else:
                print("Station")
                interfaces["wifi-bot"] = drivers
        elif wanintf == intf:
            print("  Is a WAN interface.")
            interfaces["wan"] = kernels
        else:
            print("  Is a LAN interface.")
            interfaces["lan"] = kernels
    if interfaces["wan"] is not None:
        return interfaces
    else:
        return None

def get_interface_rules():
    interfaces = get_interface_map()
    if interfaces is None:
        raise ValueError("Couldn't get all interface info!")
    rules = []
    rules.append("# Ruleset autocreated by generate_udev_rules.py")
    rules.append("# DO NOT MODIFY.  Your changes will be overwritten by")
    rules.append("# the ansible playbook 'network.yml'.  See the Riverbots")
    rules.append("# 2023/love repo for more info.")
    rules.append("")
    print("has_wan")
    rules.append("SUBSYSTEM==\"net\", ACTION==\"add\", KERNELS==\"%s\", NAME=\"wan\"" % interfaces["wan"])
    rules.append("SUBSYSTEM==\"net\", ACTION==\"add\", KERNELS==\"%s\", PROGRAM=\"/sbin/ip link set %%k address %s\"" % (interfaces["wan"], make_mac()))
    if "lan" in interfaces:
        print("has_lan")
        rules.append("SUBSYSTEM==\"net\", ACTION==\"add\", KERNELS==\"%s\", NAME=\"lan\"" % interfaces["lan"])
    if "wifi-ap" in interfaces:
        print("has_wifi_ap")
        rules.append("SUBSYSTEM==\"net\", ACTION==\"add\", DRIVERS==\"%s\", NAME=\"wifi-ap\"" % interfaces["wifi-ap"])
    if "wifi-bot" in interfaces:
        print("has_wifi_bot")
        rules.append("SUBSYSTEM==\"net\", ACTION==\"add\", DRIVERS==\"%s\", NAME=\"wifi-bot\"" % interfaces["wifi-bot"])
    return "\n".join(rules)

if __name__ == "__main__":
    rules = get_interface_rules()
    with open("/etc/udev/rules.d/70-persistent-net.rules", "w") as fd:
        fd.write(rules)
        fd.write("\n")
    try:
        os.stat("/sys/class/net/wan")
        os.stat("/sys/class/net/lan")
        os.stat("/sys/class/net/wifi-ap")
        os.stat("/sys/class/net/wifi-bot")
    except:
        print("/etc/udev/rules.d/70-persistent-net.rules created.  Rebooting to commit in 5 mins...")
        proc = subprocess.Popen(["systemd-run", "--on-active=5min", "/sbin/reboot"], stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()

    # If we got here, there's nothing to do.
    print("All interfaces already exist.  Leaving it alone...")
