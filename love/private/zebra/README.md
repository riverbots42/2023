# The Zebra Label Printer Print Driver

## Introduction

This directory implements a web server on http://lab.riverbots.org:8080 that can take a
PNG file, convert it to the Zebra page description language, and spit it out to a connected
label printer.

## Usage

There's a trivial UI that's accessible on the AHS network on http://lab.riverbots.org:8080
that you can upload a PNG file to to print.  Otherwise, the system is really only ever used
by the [../admin](../admin UI on http://lab.riverbots.org).

## Deployment

The container must be built on an x86_64 machine and pushed to podmanhub
before it can be deployed, which means using a standard PC, the print
server itself, or a cloud instance that's on x64.

To build a container, just:

```
make podman   # builds the container locally
make push     # tries to login to podmanhub and push the image
```
