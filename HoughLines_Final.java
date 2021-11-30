import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;
import ij.gui.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;




/** HoughLines_Final.java
 
This ImageJ plugin shows the Hough Transform Space for lines
of predefined radius. The source image must be an 8-Bit black & white.
<p>
Houghlines_.java is open-source. You are free to do anything you want
with this source as long as I get credit for my work.
 
@author Edited and improved by Jonatan Polanco
      Universidad de Ibagué
      * from Hemerson Pistori (pistori@ec.ucdb.br)
      e Eduardo Rocha Costa (eduardo.rocha@poli.usp.br)
 
*/



public class HoughLines_Final implements PlugInFilter {
     ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp=imp;
        if (arg.equals("about")) {
            return DONE;
        }
        return DOES_8G+DOES_STACKS+SUPPORTS_MASKING;
    }
    
    public void run(ImageProcessor ip) {


        byte imageValues[]= (byte[])ip.getPixels(); // Raw image (returned by ip.getPixels())
        byte pixelValue;
        Rectangle r = ip.getRoi();
        int houghValues[][]; // Hough Space Values
        int width= r.width; // Original Image width
        int height= r.height;  // Original Image height
        int hWidth= 500; // HoughSpace Width
        int tmp = Math.max(height,width);
        int hHeight= (int) (Math.sqrt(2)*tmp);  // HoughSpace Height
        int threshold = 40; // Percent related to the maximum hough value.
        int centre_x=  width / 2;
        int centre_y= height / 2;
        double theta_step= Math.PI / hWidth;
        
        //Create the hough array and initialize to zero
        houghValues = new int [hWidth][2*hHeight];
        for(int i = 0; i < hWidth; i++) {
            for(int j = 0; j < 2*hHeight; j++) {
                houghValues[i][j] = 0;
            }
        }
        
        //Update hough array 
        for(int i = 0; i < width; i++) {

            for(int j = 0; j < height; j++) {
                pixelValue = imageValues[j*width + i];
                //Find white pixels
                if(pixelValue != 0) {       
                    for(int k = 0; k < hWidth; k++) {

                        //Work out the r values for each theta step (r is the distance to the origin(center) to the closest line)
                        tmp = (int) (((i-centre_x)*Math.cos(k*theta_step)) +
                                     ((j-centre_y)*Math.sin(k*theta_step)));

                        tmp += hHeight;
                        if (tmp >= 0 && tmp < 2*hHeight)
                            //Increment hough array //Let's vote! 
                            houghValues[k][tmp]++;
                            //polar hough array (k -> theta and tmp ->rho)
                    }
                }
            }
        }


        // Create image View for Marked Lines
        ImageProcessor linesip = new ByteProcessor(width, height);
        byte[] linesPixels = (byte[])linesip.getPixels();

        // Draw the lines in a new image
        // getBestLines(maxLines);
        int high = 0;

        int thresh;

        //Find the max hough value for the thresholding operation
        for(int i = 0; i < hWidth; i++) {
            for(int j = 0; j < 2*hHeight; j++) {
                if(houghValues[i][j] > high) {
                    high = houghValues[i][j];
                }
            }
        }

        //Set the threshold limit
        thresh = high*(100-threshold)/100;

        // Search for local peaks above threshold to draw // calculate maximum votes
        boolean draw = false;
        int k;
        int h=0;
        int v=0;
        int l;
        int dt;     // test theta
        int dr;     // test offset
        int[] b=new int[256*256];
        int m[]=new int[256*256];
        int count=-1;
        
        for(int i = 0; i < hWidth; i++) {
            for(int j = 0; j < 2*hHeight; j++) {

                // only consider points above threshold
                if(houghValues[i][j] >= thresh) {   //(i=theta, j=rho) 
                    

                    // see if local maxima
                    draw = true;
                    int peak = houghValues[i][j];
                    for(k = -1; k < 2; k++) {
                        for(l = -1; l < 2; l++) {
                            if (k==0 && l==0)
                                continue;
                            dt = i+k;
                            dr = j+l;
                            if (dr < 0 || dr >= 2*hHeight)
                                continue;
                            if (dt < 0)
                                dt = dt + hWidth;
                            if (dt >= hWidth)
                                dt = dt - hWidth;
                            if (houghValues[dt][dr] > peak) {
                                draw = false;
                                break;
                            }
                        }
                    }
                    if (!draw)
                        continue;
   
                    //Draw edges in output array  //
                    double tsin = Math.sin(i*theta_step);
                    double tcos = Math.cos(i*theta_step);
          
                    if (i <= hWidth/4 || i >= (3*hWidth)/4) {
                        for(int y =0; y < height; y++, h++) {
                            int x = (int) (((j-hHeight) - ((y-centre_y)*tsin)) / tcos) + centre_x;
                            
                            if(x < width && x >= 0) {
                                linesPixels[y*width+x] = -1;

                            }
                        }
                    } 
                    else {
                        for(int x = 0; x < width; x++, v++) {
                            int y = (int) (((j-hHeight) - ((x-centre_x)*tcos)) / tsin) + centre_y;
   
                            if(y < height && y >= 0) {
                                linesPixels[y*width+x] = -1;
                            }
                        }                        
                    }
                }
            }
        }
              
        imp.updateAndDraw();
        
      
        
        new ImagePlus("Lines Found", linesip).show();
        
    }
}


