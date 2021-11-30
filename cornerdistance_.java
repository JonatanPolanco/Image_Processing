/*
This algorithm is based on the article "Noise Estimation Using Adaptive 
Gaussian Filtering And Variable Block Size Image Segmentation" 
and with certain adaptations made by Manuel Forero and Sergio Miranda
*/
import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.plugin.filter.*;
import ij.gui.NewImage;
import static ij.plugin.filter.PlugInFilter.DOES_8C;
import static ij.plugin.filter.PlugInFilter.DOES_8G;



public class cornerdistance_ implements PlugInFilter
{
    ImagePlus imp;
    int width;
    int height;
    int [][] ima;
    int unit;
    float force;
    float um;
    float pix;
    

    public int setup(String arg, ImagePlus imp)
    {
        GenericDialog gd=new GenericDialog("hardness");
        gd.addNumericField("applied force",1,3); 
        String[] items = {"Kgf", "N"};
        gd.addChoice("Unit", items, items [0]);   
        gd.addNumericField("micrometros",10,4);
        gd.addNumericField("Pixels",10,4);
        gd.showDialog();
        if(gd.wasCanceled())
            return-1;
        // usar los variables de entrada dentro del proceso    
        this.imp = imp;
        force=(float) gd.getNextNumber();
        unit = gd.getNextChoiceIndex();// 0 kff, 1 N
        um=(float) gd.getNextNumber();
        pix=(float) gd.getNextNumber();
        width=imp.getWidth();//image width
        height=imp.getHeight();//image height
        return DOES_ALL;
    }
    @Override
 
    public void run(ImageProcessor ip)
    {
        long TInicio, TFin, tiempo; //variables to determine the execution time
        TInicio=System.currentTimeMillis();//start time
        int i,j;
       
        
        //encutra los extremos del rombo
        boolean ux=false;
        int pos[][]=new int [2][4];//x, y
        ima =new int [width][height];
        //copia ima
        //esquina superior
        for(j=0;j<height;j++)
            for(i=0;i<width;i++)
            {    
                ima[i][j]=ip.getPixel(i,j);
            }   
        //esquina superior
         for(j=0;j<height;j++)
         {
            for(i=0;i<width;i++)
            {    
                if(ima[i][j]>127)
                {
                    ima[i][j]=127;
                    pos [0][0]=i;
                    pos[1][0]=j;
                    ux=true;
                    break;
                }
            }
            if(ux==true)
                break;
         }
         IJ.log("up["+pos [0][0]+"]["+pos[1][0]+"]");
         ux=false;
         //esquina inferior
         for(j=height-1;j>-1;j--)
          {
            for(i=width-1;i>-1;i--)
            {    
                if(ima[i][j]>127)
                {
                    ima[i][j]=127;
                    pos [0][1]=i;
                    pos[1][1]=j;
                    ux=true;
                    break;
                }
            }
            if(ux==true)
                break;
         }
         IJ.log("down["+pos [0][1]+"]["+pos[1][1]+"]");
         //esquina izquierda
         ux=false;
         for(i=0;i<width;i++)
         {
            for(j=0;j<height;j++)
            {    
                if(ima[i][j]>127)
                {
                    ima[i][j]=127;
                    pos [0][2]=i;
                    pos[1][2]=j;
                    ux=true;
                    break;
                }
            }
            if(ux==true)
                break;
         }
         IJ.log("left["+pos [0][2]+"]["+pos[1][2]+"]");
         ux=false;
         //esquina derecha
         
         for(i=width-1;i>-1;i--)
          {
            for(j=height-1;j>-1;j--)
            {    
                if(ima[i][j]>127)
                {
                    ima[i][j]=127;
                    pos [0][3]=i;
                    pos[1][3]=j;
                    ux=true;
                    break;
                }
            }
            if(ux==true)
                break;
         }
         IJ.log("right["+pos [0][3]+"]["+pos[1][3]+"]");
         int c;
        //mejorar utilizar dos vectores  
        ImagePlus impt=NewImage.createByteImage("Hardness",width,height,1,NewImage.FILL_BLACK);
        ImageProcessor ipt=impt.getProcessor();
        byte[] pixelc=(byte[])ipt.getPixels();
        for(j=c=0;j<height;j++)
        {
            for(i=0;i<width;i++)
            {
                pixelc[c++]=(byte)ima[i][j];
            }
        }
        impt.show();
        // Distance D1=left-right D2=up-down
       float Dp1,Dp2,D1,D2,D,HV,HV1;
       
       Dp1=(float)Math.sqrt(Math.pow(pos [0][3]-pos [0][2], 2)+ Math.pow(pos[1][3]-pos[1][2],2));
       IJ.log("Pixel-Distance D1=Right-Left="+Dp1);
       Dp2=(float)Math.sqrt(Math.pow(pos [0][1]-pos [0][0], 2)+ Math.pow(pos[1][1]-pos[1][0],2));
       IJ.log("Pixel-Distance D2=Down-up="+Dp2);
       //D1=((Dp1*20)/298)/1000; D2=((Dp2*20)/298)/1000;
       D1=((Dp1*um)/pix)/1000; D2=((Dp2*um)/pix)/1000;
       IJ.log("Distance (mm) D1=Right-Left="+D1);
       IJ.log("Distance (mm) D2=Down-up="+D2);
       D=(D1+D2)/2;
       IJ.log("Average-Real-Distance (mm)="+D);
       if(unit==0)
       {
           HV=(float)(1.8544*force)/(D*D);
           IJ.log("Dureza HV"+force+"Kgf="+HV);
       }
       else
       {
           HV=(float)(0.1891*force)/(D*D);
           IJ.log("Dureza HV"+force+"N="+HV);     }
       
       
    }

}