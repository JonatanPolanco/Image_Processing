import ij.*;
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import javax.swing.*;
import ij.gui.*;

public class Cos_Filter implements 
PlugInFilter {

    ImagePlus imp;

    @Override
    public int setup(String arg, 
    ImagePlus imp) {
        this.imp = imp;
        return DOES_8G;
    }
    @Override
    public void run(ImageProcessor ip)
        {
            double mas[] = null;    // Mascara 
            double peso = 0; 
            int p = 6;
            int q =0;
            int s = 0;
            byte []pixels = (byte[])ip.getPixels();
            int height = ip.getHeight();
            int width = ip.getWidth();
            int [][]bitmap1 = new int[height][width];  
            int [][]bitmap2 = new int[height][width];  
            int center = (int)mas.length/2; 
            int n_pixel;
  
            for(int c=0;c<=90;c+=15)
            {  
                    mas[p] = Math.cos(c);
                    mas[6-q] = Math.cos(c);
                    p++;
                    q++;
            }  
     
            for(int y=0;y<height;y++)
            {
                for(int x=0;x<width;x++)
                {   bitmap1[y][x] = pixels[s]&0xFF;
                    bitmap2[y][x] = pixels[s]&0xFF;
                    s++;
                }
            }               
            
             for(int x=0;x<width;x++)
            {
                for(int y=center;y<height-center;y++)
                {
                    double summ = 0;
                    for(int z=-center;z<=center;z++)
                    { summ += mas[center+z]*bitmap2[y+z][x]; }
                    n_pixel = (int)(summ/peso);
                    bitmap1[y][x] = n_pixel;
                }
            }
                       
            for(int y=0;y<height;y++)
            {
                for(int x=center;x<width-center;x++)
                {
                    double summ = 0;
                    for(int z=-center;z<=center;z++)
                    { summ += mas[center+z]*bitmap1[y][x+z];  }
                    n_pixel = (int)(summ/peso);
                    bitmap2[y][x] = n_pixel;
                }
            }
           
            int i,y;
            for(i=y=0;y<height;y++)
            {
                for(int x=0;x<width;x++)
                {  pixels[i] = (byte)bitmap1[y][x];
                    i++;}
            }            
            imp.updateAndDraw(); 
        }}