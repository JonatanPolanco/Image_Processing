import ij.*;
import ij.gui.NewImage;
import ij.process.*;
import ij.plugin.filter.*;
// Threshold Umbral1

public class Umbral1_ implements PlugInFilter
{
    ImagePlus imp;
    int width = 0;
    int height = 0;

    @Override
    public int setup(String arg,ImagePlus imp)
    {
        this.imp=imp;
        height=imp.getHeight();
        width=imp.getWidth();
        return DOES_RGB;
    }

    @Override
    public void run(ImageProcessor ip)
    {
        int i;
        int size=ip.getHeight()*ip.getWidth();
        double [] entropy=new double [3];
        double[][] hist=new double[3][256];
        int[] histf=new int[256];
        byte[] r=new byte[size],g=new byte[size],b=new byte[size];
        byte[][] cnn=new byte[3][size];
        int[] rgb;
        int x,y;
        for (y = 0; y < height; y++)
            for (x = 0; x < width; x++)
            {
                rgb = ip.getPixel(x,y,null);
                cnn[0][y*width+x]=(byte)rgb[0];//red
                cnn[1][y*width+x]=(byte)rgb[1];//green
                cnn[2][y*width+x]=(byte)rgb[2];//blue
            }
        int j;
        for (j = 0; j < 3; j++) 
        {
            for(i=0;i<size;hist[j][cnn[j][i++]&0xff]++);
            
            for (i=0;i<256;i++) 
                hist[j][i]/=size;
            for (i=0;i<256;i++)
                if(hist[j][i]!=0)
                    entropy[j]-=hist[j][i]*Math.log(hist[j][i]);
            entropy[j]/=0.30102999566;
        }
        IJ.log("Entropy_r="+entropy[0]);
        IJ.log("Entropy_g="+entropy[1]); 
        IJ.log("Entropy_b="+entropy[2]);
        j=0;
        if(entropy[1]>entropy[0] && entropy[1]>entropy[2])
            j=1;
        if(entropy[2]>entropy[0] && entropy[2]>entropy[1])
            j=2;
        
        int jj=j;
        IJ.log("canal selc="+j);
        for(i=0;i<size;histf[cnn[j][i++]&0xff]++);
        int max=0;
        int max1=0;
        int auxh=0;
        for(i=250;i>4;i--)
        {
            if(histf[i]>histf[i+5] && histf[i]>histf[i-5])
                if(histf[i]>histf[max])
                        max=i;
        }
        //IJ.log("max="+max);
        int cont=0;
        for(j=max-6;j>2;j--)
        {
            if(histf[j]>=histf[j+3] && histf[j]>histf[j-3])
                   if(histf[j]>auxh && histf[j]<histf[max] )
                   {
                           max1=j;
                           //IJ.log("max1="+max1);
                           auxh=histf[max1];
                   }
        }
        
            
       // IJ.log("*******************");
       // IJ.log("max1="+max1);
        int min=256;
        int minaux=height*width;
        for(i=max1;i<max;i++)
        {
            if(histf[i]<minaux)
            {
                minaux=histf[i];
                min=i;
            }
        }
        //IJ.log("min="+min);
        if (max-max1>55)
        {
            cont=max-max1;
            cont*=0.69;
            min=max1+cont;
        }
        min+=7;
        //IJ.log("min="+min);
        int aux=0;
        ImagePlus impH=NewImage.createByteImage("Binary ",width,height,1,NewImage.FILL_BLACK);
        ImageProcessor ipH=impH.getProcessor();
        byte[] pixelss=(byte[])ipH.getPixels();
        for(y=0;y<height;y++)
            for(x=0;x<width;x++)
            {
               aux=cnn[jj][y*width+x]&0xff;
               //IJ.log("AUX="+aux);
               if(aux<min)
                    pixelss[y*width+x]=(byte) 255;
               else
                   pixelss[y*width+x]=(byte) 0;
            }
        IJ.log("Umbral="+min);
        impH.show();
    }
    
}