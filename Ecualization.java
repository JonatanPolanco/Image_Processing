import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

import ij.*;
import ij.process.*;
import ij.plugin.filter.*;

public class Ecualization implements PlugInFilter {

    ImagePlus imp;

    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_8G + NO_CHANGES;
    }

    @Override
    public void run(ImageProcessor ip) {
        // ip.invert();
       

        // ip.findEdges();
        
        int height = ip.getHeight(); // Obteniendo altura
        int width = ip.getWidth(); // Obteniendo ancho
        int hist = 256;
        int[] H= ip.getHistogram();
        int M = height * width;
        byte[] pixels = (byte[]) ip.getPixels();
        ImageProcessor histIp = new ByteProcessor(height, width);
        
        for (int j = 1; j <  H.length ; j++){
            H[j]=H[j-1] + H[j];
        }
        for (int i = 0; i < height; i++){
            for (int u = 0; u < width; u++){
            int a = ip.get(u, i);
            int b = H[a]*(hist-1)/M;
            ip.set(u,i,b);
        
        }
                
        imp.updateAndDraw();

    }}}