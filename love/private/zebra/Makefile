UID := $(shell id -u)
GID := $(shell id -g)

zebra-printer.jar:	$(wildcard *.java) pom.xml Containerfile
	$(MAKE) podman
	podman run -v `pwd`:/dst -it riverbots42/zebra cp /$@ /dst/$@
	podman run -v `pwd`:/dst -it riverbots42/zebra chown $(UID):$(GID) /dst/$@

pretty:	$(wildcard *.java)
	for i in $^; do astyle $$i; done

test:
	$(MAKE) podman
	podman run -p 8080:8080 -it riverbots42/zebra:latest

podman:
	podman build -t riverbots42/zebra:latest .
	podman login
	podman push riverbots42/zebra:latest

clean:
	podman run -v `pwd`:/dst -w /dst -it alpine rm -f *.class *.jar *.orig *.zpl *.png
