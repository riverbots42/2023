package org.riverbots.zebra;

import java.awt.image.*;
import java.io.*;

public class ZPL {
    protected String label;

    public boolean[][] bwImage(BufferedImage src, int width, int height) {
        boolean dst[][] = new boolean[height][width];
        for( int row=0; row<height; row++ ) {
            for( int col=0; col<width; col++ ) {
                dst[row][col] = false;
                int p = src.getRGB(col, row);
                int a = (p>>24)&0xff;
                int r = (p>>16)&0xff;
                int g = (p>>8)&0xff;
                int b = p&0xff;
                // We assume transparent-ish pixels are white, so we only care
                // about alpha > 50%.
                if( a > 128 ) {
                    int gv = (int) (
                                 0.299 * (double) r +
                                 0.687 * (double) g +
                                 0.114 * (double) b
                             );
                    if(gv < 128) {
                        dst[row][col] = true;
                    }
                }
            }
        }
        return dst;
    }

    public ZPL(BufferedImage img) {
        StringWriter lbl_s = new StringWriter();
        PrintWriter lbl = new PrintWriter(lbl_s);
        lbl.printf("^XA\r\n");	// Start a label.
        lbl.printf("^FX This is a simple label, assumes the PNG becomes a 1:1 ZPL string.\r\n");
        lbl.printf("^FX This particular label is %d x %d in size.\r\n", img.getWidth(), img.getHeight());
        int width = img.getWidth() - img.getWidth()%8;
        int height = img.getHeight();
        int glyphCount = width * height / 8;
        lbl.printf("^PW%d\r\n", width);
        lbl.printf("^FO0,0^GFA,%d,%d,%d,", glyphCount, glyphCount, width/8);
        boolean[][] pixmap = bwImage(img, width, height);
        for(int row=0; row<height; row++) {
            int c=0;
            for(int col=0; col<width; col++) {
                c <<= 1;
                if(pixmap[row][col]) {
                    c |= 0x01;
                }
                if( col % 8 == 7) {
                    lbl.printf("%02X", c);
                    c = 0;
                }
            }
        }
        lbl.printf("\r\n");
        lbl.printf("^XZ\r\n");	// End the label.
        lbl.close();
        label = lbl_s.toString();
    }

    public String toString() {
        return label;
    }
}
