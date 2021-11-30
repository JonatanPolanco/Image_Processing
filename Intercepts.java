import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Intercepts implements PlugInFilter {
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
            int width = imp.getWidth();
            int height = imp.getHeight();
            int image[][] =new int [width][height];
            

                          for (int i = 0; i < height; i++) {
                              for (int j = 0; j < width; j++) {
                                   int sum_vecinos = ip.getPixel(i-1,j-1)+ip.getPixel(i,j-1)+ip.getPixel(i+1,j-1)+ip.getPixel(i+1,j+1)+ip.getPixel(i,j+1)+ip.getPixel(i-1,j+1);
                                  if (ip.getPixel(i,j)==255) {
                                     
                                      if (sum_vecinos>=1020) {
                                          IJ.log("Intercepto["+i+"]["+j+"]");
                                      }
                                      
                                  }
                              }
                
            }
                          
	}

}
