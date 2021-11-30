/**
* Hough_lines.java
* Created on 18 July 2018, 17:10 by Manuel Guillermo Forero-Vargas, Cristian
* Morera-Díaz and Natalia Hernández-Riaño
* 
* e-mail: mgforero@yahoo.es
*
* Function: This plug-in finds the best Hought lines in a binary image.
* 
* Input: 8-bit image of a contour. The image is not modified.
* Output: A circle appears overlayed over the original image.
*
* This plugin does not create a new image
* 
* Copyright (c) 2018 by Manuel Guillermo Forero-Vargas
* e-mail: mgforero@yahoo.es
*
* This plugin is free software;you can redistribute it and/or modify
* it under the terms of the GNU General Public License version 2
* as published by the Free Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY;without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this plugin;if not, write to the Free Software
* Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/
import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.geom.*;

public class Hough_lines implements PlugInFilter
{
    ImagePlus imp;

    @Override
    public int setup(String arg, ImagePlus imp)
    {
        this.imp=imp;
        return DOES_8G+SUPPORTS_MASKING;
    }

    @Override
    public void run(ImageProcessor ip)
    {
        int width=ip.getWidth();
        int height=ip.getHeight();
        int b,i,j;
        int rho,theta;
        int x0,x1,x11,x2, x3, x4,x5,x6,x7,y1;
        int matHough[][];
        double a=Math.PI/180.;//Constant employed to convert from radiants to degrees-
        byte[] pixels=(byte[])ip.getPixels();
        int maxRho=(int)(Math.sqrt(width*width+height*height));
        matHough=new int[maxRho][180];
        
        for (j=0;j<height;j++)
        {
            b=j*width;
            for (i=0;i<width;i++)
                if ((pixels[b+i]&0xff)==255)
                    for (theta=0;theta<180;theta++)
                    {
                        rho=(int)(i*Math.cos(theta*a)+j*Math.sin(theta*a));
                        if (rho<0)
                            rho=-rho;
                        matHough[rho][theta]++;
                    }
        }
        //----------------------------------------------------------------------
        //Find the number of lines choosen by the user
        int max1=0, max2=0, max3=0, max4=0, rhoMax=0,rhoMax1=0, rhoMax2=0, rhoMax3=0, rhoMax4=0 ,thetaMax=0, thetaMax2=0, thetaMax3=0, thetaMax4=0;
        for (theta=0;theta<180;theta++)
            for (rho=0;rho<maxRho;rho++)
                if (matHough[rho][theta]>max1)
                {
                    max4=max3;
                    rhoMax4=rhoMax3;
                    max3=max2;
                    rhoMax3=rhoMax2;
                    max2=max1;
                    rhoMax2=rhoMax1;
                    max1=matHough[rho][theta];
                    rhoMax1=rho;
                    
                    thetaMax=theta;
                }
                         
        x0=(int)(rhoMax1/(Math.cos(thetaMax*a)));
        x1=  (int)((rhoMax1-(height*Math.sin(thetaMax*a)))/Math.cos(thetaMax*a));
        
        x2= (int)(rhoMax2/(Math.cos(thetaMax2*a)));
        x3= (int)((rhoMax2-(height*Math.sin(thetaMax2*a)))/Math.cos(thetaMax2*a));
        
        x4= (int)(rhoMax3/(Math.cos(thetaMax3*a)));
        x5= (int)((rhoMax3-(height*Math.sin(thetaMax3*a)))/Math.cos(thetaMax3*a));
        
        x6= (int)(rhoMax4/(Math.cos(thetaMax4*a)));
        x7= (int)((rhoMax4-(height*Math.sin(thetaMax4*a)))/Math.cos(thetaMax4*a));
        
        x11=width;
        y1=height;
        //----------------------------------------------------------------------
        //Show overlay line
       /* GeneralPath path=new GeneralPath();
        path.moveTo(x0,0);
        path.lineTo(x1,y1);
        Roi line=new ShapeRoi(path);
        line.setStrokeColor(Color.magenta);
        imp.setOverlay(new Overlay(line));*/
        //----------------------------------------------------------------------
        //Draw line on the image. Uncomment thee instructions if you want it
        
        IJ.log("x0="+x0+" y0="+0+" x1="+x1+" y1="+y1+ "thetaMax="+theta +"Rho="+rhoMax1);
        ip.setColor(Color.white);
        ip.moveTo(x0,0);
        ip.lineTo(x1,height);
        
        ip.moveTo(x2,0);
        ip.lineTo(x3,height);
        
        ip.moveTo(x4,0);
        ip.lineTo(x5,height);
        
        ip.moveTo(x6,0);
        ip.lineTo(x7,height);
        imp.updateAndDraw();
    }
}
