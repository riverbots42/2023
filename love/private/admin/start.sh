#!/bin/bash

openssl enc -aes-256-cbc -pbkdf2 -pass pass:"$passphrase" -d -in /etc/love.properties.enc -out /etc/love.properties
cd /usr/local/tomcat
bin/catalina.sh run
