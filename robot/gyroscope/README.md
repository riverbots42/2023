# Introduction

The gyroscope module, connected to the Roborio via the i2c protocol, can tell
us our robot's orientation in space (relative to gravity), so we can balance
it.

## Hardware

The module was purchased from [Amazon](https://www.amazon.com/dp/B00LP25V1A)
and has 6-axis readings (both accelerometer and gyroscopic) and a temperature
sensor.

Hooking it up means:

* VCC to +5V power
* GND to ground
* SCL and SDA to the relevant port on the Roborio

## Demo

See the [Youtube Demo](https://youtu.be/4bNUOXHWqdU).

The demo code is in this directory as gyro.py and is intended to be used in
conjunction with the Orange Pi Zero board we've been using for the wifi-fan
project.

/boot/armbianEnv.txt will need to be adjusted to have the "i2c0" overlay turned
on and the board hooked up to pins 1, 3, 5, and 9 for Vcc, SCL, SDA, and GND
respectively.

...but this is probably better done hooked up to the Roborio :-/

## Docs

The board has fairly limited docs itself, but [the MPU-6050 PDF][https://invensense.tdk.com/wp-content/uploads/2015/02/MPU-6000-Register-Map1.pdf]
has some great info, particularly about the registers.

WPIlib (the toolkit we use to talk to the robot) has an i2c library that's
decently comprehensive, but the docs kinda suck.  Honestly, you're better off
reading the [source code to I2C.java](https://github.com/wpilibsuite/allwpilib/blob/main/wpilibj/src/main/java/edu/wpi/first/wpilibj/I2C.java)

## Special Note: 2's Complement Format

The registers that the chip has return values in the following form:

1. Most significant byte (basically the "upper" part of the number) and
2. Least significant byte (basically the "lower" part).

And the values returned are from 0-255, but values from 128-255 are actually
negative.  Yeah, this takes some getting used to and most CompSci/CompEng folks
only deal with this in a digital design or assembly language class at
university, so it's a bit weird.

We get two numbers.  Let's call them "hi" and "lo."

The total is basically:  `hi*256 + lo`

...but that assumes the number is positive...

So we need to look at numbers bigger than 32,767 (i.e. when hi > 127) as being
negative.  To convert in 2's complement, you flip all the bits and add 1.

So in a 4-bit example:

* 1010 binary = 10 decimal, but as a 2's complement, it's negative.
* Flip the bits and we get 0101.
* Add 1 and we get 0110, or 6 decimal.
* So in 2's complement, a 4-bit 1010 binary = -6 decimal.

Yeah, try not to think about it too hard.  There's a good video on the subject
[on Youtube](https://www.youtube.com/watch?v=lKTsv6iVxV4).
