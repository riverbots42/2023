UID := $(shell id -u)
GID := $(shell id -g)

zebra-printer.jar:	$(wildcard *.java) pom.xml Dockerfile
	$(MAKE) docker
	docker run -v `pwd`:/dst -it zebra-printer cp /$@ /dst/$@
	docker run -v `pwd`:/dst -it zebra-printer chown $(UID):$(GID) /dst/$@

test:
	$(MAKE) docker
	docker run -p 8080:8080 -it zebra-printer

docker:
	docker build -t zebra-printer .

clean:
	docker run -v `pwd`:/dst -w /dst -it alpine rm -f *.class *.jar
