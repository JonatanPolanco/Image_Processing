import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.plugin.filter.*;
import ij.gui.NewImage;
import static ij.plugin.filter.PlugInFilter.DOES_8C;
import static ij.plugin.filter.PlugInFilter.DOES_8G;

public class Filter_By_Area implements PlugInFilter {
	ImagePlus imp;
        int width=0;
        int height=0;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
                height=imp.getHeight();
                width=imp.getWidth();
		return DOES_ALL;
                
	}

	public void run(ImageProcessor ip) {
            
        int i,j;    
        //solo deja pasar el objeto de la imagen con mayor area
        //en este caso el rombo
        int[] hist=((ShortProcessor)ip).getHistogram();
        double maxValue=  ip.getMax();
        int labelmax=0;
        int nummax=0;
        ImagePlus impH=NewImage.createByteImage("Filter_By_Area ",width,height,1,NewImage.FILL_BLACK);
        ImageProcessor ipH=impH.getProcessor();
        byte[] pixelss=(byte[])ipH.getPixels();
        for(i=1;i<=maxValue;i++)
            if(hist[i]>nummax)
            {
                labelmax=i;
                nummax=hist[i];
            }
        int pixx=0;
        for(j=0;j<height;j++)
            for(i=0;i<width;i++)
            {
                pixx=ip.getPixel(i,j);
               if(pixx!=labelmax &&pixx!=0)
                    pixelss[j*width+i]=(byte) 0;
               if(pixx==labelmax)
                   pixelss[j*width+i]=(byte) 255;
            }
        
 
        impH.show();
	}

}
