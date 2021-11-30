import ij.*;
import ij.process.*;
import ij.plugin.filter.*;

public class Filtro_Brillo implements PlugInFilter {

    ImagePlus imp;

    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_8G;
    }

    @Override
    public void run(ImageProcessor ip) {
        // ip.invert();

        // ip.findEdges();
        
        int height = ip.getHeight(); // Obteniendo altura
        int width = ip.getWidth(); // Obteniendo ancho

        byte[] pixels = (byte[]) ip.getPixels();

        for (int i = 0; i < height * width; i++) {
            int pixel = (pixels[i] & 0xff) + 50;

            /*
                    if (pixel > 255) {
                        pixels[i] = (byte) 255;
                    } else {
                        pixels[i] = (byte) pixel;
                    }
             */
            
            pixels[i] = pixel > 255 ? (byte) 255 : (byte) pixel;
        }

        imp.updateAndDraw();

    }

}