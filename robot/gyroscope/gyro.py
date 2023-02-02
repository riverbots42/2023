#!/usr/bin/python3

"""
    Simple demo of reading the MPU-6050 i2c gyro module and spitting out
    the X, Y, and Z accelerometer readings with temperature once per second.
"""

import smbus
import time

class Gyro:
    channel = 0
    address = 0x68

    def __init__(self):
        self.bus = smbus.SMBus(self.channel)

    def poweron(self):
        """ Turn the Gyro on, basically clearing the powersave register. """
        self.bus.write_i2c_block_data(self.address, 0x6B, [0x0])

    def twos_parse(self, hi, lo):
        """
            Convert a 2-byte 2s complement 16-bit value to a decimal in the
            range -32768..+32767
        """
        raw = hi<<8 | lo
        if hi < 128:
            # This is a positive number, so we can treat it normally.
            return raw
        else:
            # This is a negative number, so we need to binary NOT it and add 1 per 2's complement spec.
            return -(((~raw) & 0xffff) + 1)

    def read(self):
        """
            Read 14 vals from the gyro's registers and return x/y/z accel
            and temperature as a tuple.
        """
        vals = self.bus.read_i2c_block_data(self.address, 0x3B)
        x = self.twos_parse(vals[0], vals[1])
        y = self.twos_parse(vals[2], vals[3])
        z = self.twos_parse(vals[4], vals[5])
        # Temperature is a bit different.  Per spec, the temp in C is this
        # weird formula.
        t = round(self.twos_parse(vals[6], vals[7])/340 + 36.53, 2)
        return (x, y, z, t)

if __name__ == "__main__":
    gyro = Gyro()
    gyro.poweron()
    while True:
        (x, y, z, t) = gyro.read()
        print(x, y, z, t)
        time.sleep(1)
