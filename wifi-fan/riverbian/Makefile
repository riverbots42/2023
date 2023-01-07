UID := $(shell id -u)
GID := $(shell id -g)
ifeq ($(UID),0)
SUDO :=
else
SUDO := sudo
endif

ARMBIAN := $(shell ls Armbian*xz | sort | tail -1)

ifeq ($(ARMBIAN),)
$(error You must download an existing Armbian .xz image before running this program.)
endif

RIVERBIAN := $(shell echo $(ARMBIAN) | sed 's/^Armbian/Riverbian/g')
RIVERBIAN_UNC := $(shell echo $(RIVERBIAN) | sed 's/\.xz$$//')
$(RIVERBIAN):	$(RIVERBIAN_UNC)
	xz -c $< > $@

precompress:	$(RIVERBIAN_UNC)

$(RIVERBIAN_UNC):	$(ARMBIAN) inject.py
	$(SUDO) ./inject.py $<
	$(SUDO) chown $(UID):$(GID) $@

clean:
	rm -f Riverbian*
