= Public Website

== Introduction

This directory has all the content for the public website https://love.riverbots.org

== Deployment and Ansible

Updating the website uses the Ansible configuration management system, which uses YAML
files to describe the process of copying/moving files, installing packages, and so on.

To deploy the website, you'll need to:

    1. Have a copy of "make" installed somewhere in your PATH (either Windows, MacOS, or Linux) and
    2. Have a copy of ansible installed somewhere in your PATH.
    3. Be able to ssh to simulator.riverbots.org as your local user, and have sudo set up.
    4. Know the super-secret password (that we use EVERYWHERE) to encrypt the appropriate bits.

If you type "make" and something happens other than a simple error, you're probably good.  A
successful deployment looks like this:

```
bj@simulator:~/2023/love/public$ make
echo Decrypting love.properties.enc...
Decrypting love.properties.enc...
enter AES-256-CBC decryption password:     <-- This is where you put in the super-secret password
ansible-playbook --ask-become-pass -i inventory 01-packages.yml 02-webapp.yml
BECOME password:                           <-- This is where you put in YOUR password for sudo

PLAY [all] *************************************************************************************************************************

TASK [Gathering Facts] *************************************************************************************************************
ok: [simulator.riverbots.org]

TASK [Install base useful packages] ************************************************************************************************
ok: [simulator.riverbots.org]

PLAY RECAP *************************************************************************************************************************
simulator.riverbots.org    : ok=2    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0


PLAY [all] *************************************************************************************************************************

TASK [Gathering Facts] *************************************************************************************************************
ok: [simulator.riverbots.org]

TASK [Create the appropriate tomcat directory.] ************************************************************************************
ok: [simulator.riverbots.org]

TASK [Copy the files in webapps/ to the target.] ***********************************************************************************
ok: [simulator.riverbots.org]

TASK [Copy in the database password.] **********************************************************************************************
ok: [simulator.riverbots.org]

PLAY RECAP *************************************************************************************************************************
simulator.riverbots.org    : ok=6    changed=0    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0
```
