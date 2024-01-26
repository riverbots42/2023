#!/bin/bash

openssl enc -aes-256-cbc -pbkdf2 -pass pass:"$PASSPHRASE" -d -in /love.properties.enc -out /usr/local/tomcat/webapps/ROOT/WEB-INF/love.properties
cd /usr/local/tomcat
bin/catalina.sh run
