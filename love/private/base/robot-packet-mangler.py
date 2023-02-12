#!/usr/bin/python3

"""
    Script to monitor connection tracking and set up reverse tunnel DNATs for
    the primary connection to a robot.

    The bit example is when, say, 10.68.0.117 wants to talk to the robot on
    10.68.45.2.  Two connections are established:

    1. 10.68.0.117:whatever -> 10.68.45.2:1110/UDP, and then the robot tries
       to talk back on:
    2. 10.68.45.2:whatever -> 10.68.0.117:1150/UDP.

    Because we use IP masquerade to make the route work, we need something that
    will poll connection tracking and make a 10.68.45.2->10.68.0.117:1150
    static DNAT whenever the 1110 connection is up.

    This script does that, polling connection tracking and looking for what
    map it needs to modify.
"""

import logging
import re
import subprocess
import sys
import time

ROUTER_IP = "10.68.45.3"
ROBOT_IP = "10.68.45.2"
ROBOT_NAME = "6845_Lovekesh"

logger = None

def get_connection():
    """
        Scan the nf_conntrack file for the first machine connection to the
        robot on UDP/1110.
    """
    connre = re.compile(r'^ipv4\s+\S+\s+udp.*src=(\S+)\s+dst=10\.68\.45\.2\s.*dport=1110')
    with open("/proc/net/nf_conntrack") as fd:
        for line in fd:
            matches = connre.search(line)
            if matches is not None:
                return matches.group(1)
    return None

def get_iptables():
    """
        Scan iptables to see what (if anything) we have currently set in
        the iptables PREROUTING table for the robot->DUT connection on UDP/1150.

        Returns the IP for the egress and ingress rules, None is possible on
        either/both.
    """
    # Get rules related to robot->gateway
    ingressre = re.compile(r'DNAT\s+udp\s+--\s+(\S+)\s+(\S+)\s+.*to:(\S+)')
    proc = subprocess.Popen(["iptables", "-t", "nat", "-nL", "PREROUTING"],
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    sout, serr = proc.communicate()
    ingress = None
    for line in sout.decode('utf-8').split("\n"):
        matches = ingressre.search(line)
        if matches is not None and matches.group(1) == ROBOT_IP and matches.group(2) == ROUTER_IP:
            ingress = matches.group(2)
            break

    # Get rules related to gateway->robot
    egressre = re.compile(r'SNAT\s+udp\s+--\s+(\S+)\s+(\S+)\s.*spt:1150')
    proc = subprocess.Popen(["iptables", "-t", "nat", "-nL", "POSTROUTING"],
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    sout, serr = proc.communicate()
    egress = None
    for line in sout.decode('utf-8').split("\n"):
        matches = egressre.search(line)
        if matches is not None and matches.group(2) == ROBOT_IP:
            egress = matches.group(1)
            break
    return ingress, egress

def sync_rules():
    """
        Grab the IP we're SUPPOSED to be using and see if the ingress/
        egress targets are correct or not.  If they're not, sync them.
    """
    target = get_connection()
    ingress, egress = get_iptables()
    change_needed = False

    if target != ingress:
        logger.info("Target: %s, ingress IP: %s" % (target, ingress))
        # We need to purge/remake the ingress rule (from robot->gateway)
        # First purge the existing rule, then add the new one.
        if ingress is not None:
            cmd = ["iptables", "-t", "nat", "-D", "PREROUTING", "-s", ROBOT_IP, "-d", ROUTER_IP, "-p", "udp", "--dport", "1150", "-j", "DNAT", "--to", ingress]
            subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
        # Now make the new rule (from robot->gateway)
        if target is not None:
            cmd = ["iptables", "-t", "nat", "-I", "PREROUTING", "-s", ROBOT_IP, "-d", ROUTER_IP, "-p", "udp", "--dport", "1150", "-j", "DNAT", "--to", target]
            subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
        change_needed = True

    if target != egress:
        logger.info("Target: %s, egress IP: %s" % (target, egress))
        # Similar to above, purge/remake the egress rule (from gateway->robot).
        if egress is not None:
            cmd = ["iptables", "-t", "nat", "-D", "POSTROUTING", "-s", egress, "-d", ROBOT_IP, "-p", "udp", "--sport", "1150", "-j", "SNAT", "--to", ROUTER_IP]
            subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
        # Now make the new rule (from gateway->robot)
        if target is not None:
            cmd = ["iptables", "-t", "nat", "-I", "POSTROUTING", "-s", target, "-d", ROBOT_IP, "-p", "udp", "--sport", "1150", "-j", "SNAT", "--to", ROUTER_IP]
            subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
        change_needed = True

    return change_needed, target

def kick_robot():
    """
        If we can't connect to the robot IP, kick the wifi connection.
    """
    proc = subprocess.Popen(["ping", "-w", "1", "-qc", "1", ROBOT_IP],
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    proc.communicate()
    if proc.returncode != 0:
        logging.warning("Robot not online.  Trying to restart the connection...")
        subprocess.Popen(["iw", "dev", "wifi_sta", "connect", ROBOT_NAME], stdout=subprocess.PIPE, stderr=subprocess.PIPE).communicate()
        time.sleep(30)

if __name__ == "__main__":
    logger = logging.getLogger(__name__)
    logger.setLevel(logging.INFO)
    handler = logging.StreamHandler(stream=sys.stdout)
    logger.addHandler(handler)

    logger.info("Starting up...")
    while True:
        changed, target = sync_rules()
        if changed:
            logger.info("Rules synced: target IP now %s." % target)
        kick_robot()
        time.sleep(1)
