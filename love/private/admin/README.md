# Valbot Administrative User Interface

## Introduction

This program connects to the database on love.riverbots.org and manages
any messages in the messages table.  Basically it's a spreadsheet-like
data entry system and is very Web 1.0 in nature (AJAX? We don't need no
steenking AJAX!).

## Architecture

This runs in a podman container on the print server and has embedded
within a rudimentary print driver to talk to the Zebra printer to print
the stickers we're distributing.

Because the container needs to talk directly to the printer, the
container must be --privileged and have /dev:/dev mapped :-(.

## Build

The container must be built on an x86_64 machine and pushed to podmanhub
before it can be deployed, which means using a standard PC, the print
server itself, or a cloud instance that's on x64.

To build a container, just:

```
make podman   # builds the container locally
make push     # tries to login to podmanhub and push the image
```
