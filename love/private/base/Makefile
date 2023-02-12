PLAYBOOKS := $(sort $(wildcard *.yml))

all:	keys.json $(PLAYBOOKS)
	$(MAKE) validate
	ansible-playbook -e @$< --ask-become-pass -i inventory $(PLAYBOOKS)

keys.json:	keys.json.enc
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
	rm -f keys.json.enc
	$(MAKE) validate
	openssl enc -aes-256-cbc -pbkdf2 -in keys.json -out keys.json.enc

validate:
	@echo Validating keys.json...
	@if ! python3 -m json.tool keys.json >/dev/null 2>/dev/null; then \
		echo "Failed!"; \
		echo "Double-check that keys.json is valid JSON."; \
		false; \
	else \
		echo "Success!"; \
	fi

clean:
	rm -f keys.json
