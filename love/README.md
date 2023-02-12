# Riverbots Valentine's Day Fundraiser Infrastructure

## Introduction

There are two main parts of the Valentine's fundraiser from an IT perspective:

1. When the orders are gathered, entering them into a web form that spits out
   a sticker with a custom code/QR on it.
2. When the order is delivered (by a Rick-Rolling-Robot on Feb 14), having a
   landing page for that QR.

## Components

1. The website on simulator.riverbots.org, which is a name-based virtual host
   of love.riverbots.org.
2. A print server that acts as a data entry station and spits out labels on
   a Zebra GC420 printer.

The website code is under public/ and the print server is under private/.

## Additional Responsiblity of the Printer Station

Because of where it is in the network, this is also a golden opportunity to fix
up a glaring problem with talking to the robot.  In this case, we use the WiFi
on the print station to act as both a Wireless AP AND a station to talk to the
robot and any controller machines simultaneously.
