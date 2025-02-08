# Base OS Install for the Lab/Printer Station

## Introduction

The lab/printer station has two main functions:

1. It acts as a Local Network-to-Internet gateway via wifi, so a laptop can talk
   to both the robot and the Internet simultaneously.

2. It acts as a print server for the label printer, running a website so you
   can shoot PNGs to it and it will print them out.

The first of these must be configured at the OS level, so the contents of this
directory try to set all that up in one go.  The second is run in a docker
container and all that is in ../printer.

## Initial Configuration

This config management is using [Ansible](https://docs.ansible.com).  The
general idea is that you'll need to configure just enough of an OS to get the
box on the network, configure a user to ssh in to, then run the ansible
playbook to configure basically everything else.

I used [Debian Stable](https://debian.org), which I flashed onto a USB stick
using unetbootin (available for both Linux desktops and Windows), but rufus
(Windows-only) would work just fine.  Basically create the USB stick first.
I'd suggest using whatever the stable AMD64 or x86_64 build is and go from
there.

Once you have the OS installed (wiping out whatever's on there), you'll need
to set up one user that can ssh in using a key.  Typically this means running
ssh-keygen, then making a .ssh/authorized_keys in the user's home directory
that has the contents of id_rsa.pub or similar.  Basically you want to be able
to `ssh user@whateveryourmachineipis` and get right in without a password.

The only real requirement for this to work otherwise is to install sudo and
make sure that your new user is in the sudo group:

```
# apt update && apt install -y sudo     # You will need to do this as root
# usermod -a -G sudo youruser
# groups youruser
youruser ... stuff ... sudo             # Make sure sudo is in here!
```

Once you do that, you can edit the inventory file to put in your target IP
and then just run `make`.

## Secrets

Rather than use a secrets manager (yet another moving part), or putting the
unencrypted blob in git (a HORRIBLE idea from a security perspective), I put
the keys.json in AES-encrypted form in this repo using a password that everyone
on the Riverbots team should know.  If you get decrypt errors on it, either ask
around for the password or revert to an older version of the file.

Hopefully you'll never need to update keys.json, but if you do make sure to
run `make encrypt` with the correct password and then check in the resulting
keys.json.enc

## Deployment

To deploy, you'll need:

    1. A copy of make installed somewhere in the PATH (either Windows, MacOS, or Linux).
    2. A copy of ansible installed.
    3. The super-secret password we use everywhere.
    4. Your sudo password.

To deploy, just type "make".  A successful run looks like this:

```
bj@simulator:~/2023/love/private/base$ make
echo Decrypting keys.json.enc...
Decrypting keys.json.enc...
enter AES-256-CBC decryption password:       <-- Enter the super-secret password here.
make validate
make[1]: Entering directory '/home/bj/2023/love/private/base'
Validating keys.json...
Success!
make[1]: Leaving directory '/home/bj/2023/love/private/base'
ansible-playbook -e @keys.json --ask-become-pass -i inventory 01-packages.yml 02-network-interfaces.yml 03-dynamic-dns.yml 04-vpn.yml
BECOME password:                             <-- Enter your sudo password here.

PLAY [all] *************************************************************************************************************************

TASK [Gathering Facts] *************************************************************************************************************
ok: [ahs.riverbots.org]

TASK [Install base useful packages] ************************************************************************************************
ok: [ahs.riverbots.org]

PLAY RECAP *************************************************************************************************************************
ahs.riverbots.org          : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0


PLAY [Configure network interfaces with correct names and WAN MAC.] ****************************************************************

TASK [Gathering Facts] *************************************************************************************************************
ok: [ahs.riverbots.org]

TASK [Copy over the network interface autoconfig script.] **************************************************************************
ok: [ahs.riverbots.org]

[...]

TASK [Enable the Robot Communication Manager service.] *****************************************************************************
changed: [ahs.riverbots.org]

PLAY RECAP *************************************************************************************************************************
ahs.riverbots.org          : ok=20   changed=6    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0
```
