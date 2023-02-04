PLAYBOOKS := $(sort $(wildcard *.yml))

all:	love.properties $(PLAYBOOKS)
	ansible-playbook --ask-become-pass -i inventory $(PLAYBOOKS)

love.properties:	love.properties.enc
	echo Decrypting $<...
	@if openssl enc -aes-256-cbc -pbkdf2 -d -in $< -out $@ 2>&1 | grep 'bad decrypt'; then \
		echo ""; \
		echo "Decrypt failed!  Bad password?"; \
		echo "This password should be in the 'Stuff to Remember' folder."; \
		echo ""; \
		rm -f $@; \
		false; \
	fi

encrypt:
	rm -f love.properties.enc
	openssl enc -aes-256-cbc -pbkdf2 -in love.properties -out love.properties.enc

clean:
	rm -f love.properties
