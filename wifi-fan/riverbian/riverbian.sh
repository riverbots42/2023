#!/bin/bash

# Install needed packages
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y \
	nginx \
	php7.4-fpm php7.4-mbstring php7.4-zip \
	wget \
	tomcat9 \
	golang \
	git \
	vim \
	hostapd bridge-utils iptables dnsmasq \
	openjdk-17-jdk-headless \
	sudo

# Remove NM, since it's always getting in the damn way.
dpkg -r network-manager-openvpn
dpkg -r network-manager

# Make nginx and tomcat9 pull from the same place.
rm -rf /var/www/html
ln -s /var/lib/tomcat9/webapps/ROOT/ /var/www/html
rm -f /var/www/html/index.html

# Install the terminal client
pushd /tmp && \
	wget https://github.com/sorenisanerd/gotty/releases/download/v1.5.0/gotty_v1.5.0_linux_arm.tar.gz && \
	tar xf gotty_v1.5.0_linux_arm.tar.gz && \
	mv -f gotty /usr/bin/ && \
	rm -f gotty_v1.5.0_linux_arm.tar.gz && \
popd
systemctl enable gotty.service

# Install the IDE
pushd /tmp && \
	wget https://github.com/Atheos/Atheos/archive/refs/tags/v5.5.0.tar.gz && \
	tar -C /var/www/html -xf v5.5.0.tar.gz && \
	mv -f /var/www/html/Atheos-5.5.0 /var/www/html/ide && \
	rm -f v5.5.0.tar.gz && \
popd

# Install the filebrowser
curl -fsSL https://raw.githubusercontent.com/filebrowser/get/master/get.sh | bash
systemctl enable filebrowser
filebrowser -d /var/lib/filebrowser/filebrowser.db config init
filebrowser -d /var/lib/filebrowser/filebrowser.db config set --port=8180
filebrowser -d /var/lib/filebrowser/filebrowser.db config set --auth.method=json
filebrowser -d /var/lib/filebrowser/filebrowser.db config set --baseurl=/files
filebrowser -d /var/lib/filebrowser/filebrowser.db users add riverbots theROBOTuprising --perm.admin

# Fix perms.
chown -R www-data:www-data /var/www/html

# Set up the riverbots user with the default password.
useradd -s /bin/bash -p '$6$DLdNCPRBVNasMGFv$neNALU4nLU3macD2FDJItTtjwm2PlcxoLJ2w7H68DGSYC5MqUKBH7iTWmPd1gunOO5JQEBEo//WwVQVFUC9qg.' -G sudo -m riverbots
echo 'echo "Your web content is in /var/www/html; your local git repo is in ~/wifi."' >> /home/riverbots/.bashrc
echo 'echo "To set your WiFi AP settings, run the command wifi.py"' >> /home/riverbots/.bashrc
mkdir -p /var/lib/tomcat9/webapps/ROOT/ide/workspace/wifi
chown -R www-data:www-data /var/lib/tomcat9/webapps/ROOT/ide
chmod g+w,g+s /var/lib/tomcat9/webapps/ROOT/ide/workspace/wifi
ln -s /var/lib/tomcat9/webapps/ROOT/ide/workspace/wifi /home/riverbots/repo
pushd /home/riverbots/repo && \
	su riverbots -c 'git init' && \
popd

# Disable the root firstrun thingy, as we have totally different defaults.
usermod -s /usr/sbin/nologin root
rm -f /root/.not_logged_in_yet

# Mess around with hostapd and whatnot.
rm -f /etc/hostapd.conf
echo 'DAEMON_CONF="/etc/hostapd/hostapd.conf"' > /etc/default/hostapd

# Set the website.
rm -f /etc/nginx/sites-enabled/default
ln -s /etc/nginx/sites-available/riverbots /etc/nginx/sites-enabled/

# Default host stuff.
echo "wifi.riverbots.org" > /etc/hostname
echo "10.68.45.1 wifi wifi.riverbots.org" >> /etc/hosts

exit 0
