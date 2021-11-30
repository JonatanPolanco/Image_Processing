
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Filter_Brillo2D implements PlugInFilter {

    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_8G;
    }

    public void run(ImageProcessor ip) {
        
        int height = ip.getHeight();
        int width = ip.getWidth();
        byte[] pixels = (byte[])(ip.getPixels());
        int[][] pixelsM = new int[height][width];
        int y, i;
        
        for (i=y=0; y< height; y++)
        {
                for (int x=0; x< width; x++)
                {
                    pixelsM[y][x] = pixels[i++] & 0xff;
                }
        
    }
        for (y=0; y< height; y++)
        {
            for (int x=0; x< width; x++)
            {
                pixelsM[y][x] += 50;
                
                if(pixelsM[y][x] > 255 )
                {
                    pixelsM[y][x] = (byte) 255;
                }
            }
        }
        
        for (i=y=0; y< height; y++)
        {
            for (int x=0; x< width; x++)
            {
                pixels[i++] = (byte)pixelsM[y][x];
            }
        }

}
}
