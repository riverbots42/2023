package org.riverbots.zebra;

import java.awt.image.*;
import java.io.*;

public class ZPL {
    protected String label;

    public ZPL(BufferedImage img) {
        StringWriter lbl_s = new StringWriter();
        PrintWriter lbl = new PrintWriter(lbl_s);
        lbl.println("^XA");	// Start a label.
        lbl.println("^FX This is a simple label, assumes the PNG becomes a 1:1 ZPL string.");
        lbl.printf("^FX This particular label is %d x %d in size.\n", img.getWidth(), img.getHeight());
        int width = img.getWidth() - img.getWidth()%8;
        int height = img.getHeight();
        int glyphCount = width * height / 8;
        lbl.printf("^FO0,0^GFA,%d,%d,%d,", glyphCount, glyphCount, width/8);
        for(int y=0; y<height; y++) {
            int c=0;
            for(int x=0; x<width; x++) {
                int rgb = img.getRGB(x, y);
                if(rgb == 0) {
                    c = (c<<1) | 0x1;
                }
                if( x % 8 == 7) {
                    lbl.printf("%02X", c);
                    c = 0;
                }
            }
        }
        lbl.println("");
        lbl.println("^XZ");	// End the label.
        lbl.close();
        label = lbl_s.toString();
    }

    public String toString() {
        return label;
    }
}
