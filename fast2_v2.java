/*
This algorithm is based on the article "Noise Estimation Using Adaptive 
Gaussian Filtering And Variable Block Size Image Segmentation" 
*/
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.gui.GenericDialog;
import ij.gui.NewImage;

public class fast2_v2 implements PlugInFilter
{
    ImagePlus imp;
    int width;
    int height;
    //String dir="C:\\Users\\ASUS\\Desktop\\ImageJ\\std";
    //String fname="datos.csv";
    int[][] homogeneous1;
    int[][] homogeneous2;
    double[][] mediablock1;
    double[][] ima;
    double[][] deviation;
    double[][] deviation2;
    float per1;
    float per2;
    int h;
    int w;
    float average=0;
    double sum1;
    double sum2;
    double deviationmin1;
    int side1;
    double sigma;
    float k;
    float div;
    int col;
    boolean mer,iha,cutmask,m1,m2;
    int fa;

    public int setup(String arg, ImagePlus imp)
    {
        GenericDialog gd=new GenericDialog("Turajlic");
        String[] items2={"Black","White"};
        String[] items3={"Polynomial","Exponential"};
        gd.addCheckbox("Improved", true);
        gd.addCheckbox("I_P1", true);
        gd.addCheckbox("I_P2", true);
        gd.addCheckbox("homogeneus areas", false);
        gd.addCheckbox("cutmask", false);
        gd.addChoice("Adjusment factor",items3,items3[0]);
        gd.addNumericField("k",1.9,2);
        gd.addNumericField("Image percentage you want to take in step 1",11.0,2);
        gd.addNumericField("Percentage of blocks in step 2",85.0,2);
        gd.addChoice("Blocks color",items2,items2[0]);
        //gd.addStringField("directory:", dir,24);
        //gd.addStringField("save_As:", fname,24);
        gd.showDialog();
        if(gd.wasCanceled())
            return-1;
        this.imp = imp;
        width=imp.getWidth();//image width
        height=imp.getHeight();//image height
        mer=gd.getNextBoolean();
        m1=gd.getNextBoolean();
        m2=gd.getNextBoolean();
        iha=gd.getNextBoolean();
        cutmask=gd.getNextBoolean();
        fa=gd.getNextChoiceIndex();
        k=(float)gd.getNextNumber();//factor1 is used to find the initial homogeneous areas
        per1=(float)gd.getNextNumber();
        per2=(float)gd.getNextNumber();
        col=(gd.getNextChoiceIndex())*255;
        //String nwdir=gd.getNextString();
        //String nwfname=gd.getNextString();
        //if( ! "".equals(nwdir.trim())) dir=nwdir;
        //if( ! "".equals(nwfname.trim())) fname=nwfname;
        side1=64;
        h=(int)(height/side1);//blocks number by the image height
        w=(int)(width/side1);//blocks number by the image width
        return IJ.setupDialog(imp,DOES_8G+DOES_8C);
    }
    @Override
    public void run(ImageProcessor ip)
    {
        double TInicio, TFin, tiempo; //variables to determine the execution time
        TInicio=System.currentTimeMillis();//start time
        //List<Integer> posX= new ArrayList<>();
        //List<Integer> posY= new ArrayList<>();
        //List<Double> std= new ArrayList<>();
        int i,j,ii,jj;
        int w2=width+2;
        int h2=height+2;
        int w1=width+1;
        int h1=height+1;
        ima=new double[w2][h2];
        //duplicate the input image        
        for(j=0;j<height;j++)
            for(i=0;i<width;i++)
               ima[i+1][j+1]=ip.getPixel(i,j);
        for(i=0;i<width;i++)
        {
            ima[i+1][0]=ip.getPixel(i,0);
            ima[i+1][h1]=ip.getPixel(i,height-1);
        }
        //copia los bordes en forma de espejo para que la mascara se pueda usar en los bordes
        for(j=0;j<h2;j++)
        {
            //espejo de arriba
            ima[0][j]=ima[1][j];
            //espejo abajo
            ima[w1][j]=ima[width][j];
        }
        //apply the convolution mask to highlight the noise
        double [][] ima2=new double[width][height];
        for(j=1;j<h1;j++)
            for(i=1;i<w1;i++)
                ima2[i-1][j-1]=(double)(((ima[i-1][j-1])+(ima[i][j-1]*-2)+(ima[i+1][j-1])+
                                        (ima[i-1][j]*-2)+(ima[i][j]*4)+(ima[i+1][j]*-2)+
                                        (ima[i-1][j+1])+(ima[i][j+1]*-2)+(ima[i+1][j+1])));
        //mejorar utilizar dos vectores                    
        deviation=new double[w][h];
        mediablock1=new double[w][h];
        double [] vecaux =new double [h*w];
        int sideh;
        int sidew;
        double rpi2=1.253314137;//is sqrt(pi/2)
        int z=0;
        int sizeBlock=side1*side1;
        //The average is calculated and the noise level is estimated
        for(j=0;j<h;j++)
        {
            //sidew and sideh  are used as pointers in the blocks
            sideh=j*side1;
            for(i=0;i<w;i++)
            {//sum of intensities in each block
                sidew=i*side1;
                for(jj=sideh;jj<sideh+side1;jj++)
                    for(ii=sidew;ii<sidew+side1;ii++)
                    {    
                        average+=ip.getPixel(ii,jj);
                        if(0>ima2[ii][jj])
                            sum1+=ima2[ii][jj]*-1;
                        else 
                            sum1+=ima2[ii][jj];
                    }
                //desviaation=sqrt(pi/2)*(1/6*(W-2)*(H-2))*SUM|x(i,j)*h|
                mediablock1[i][j]=average/sizeBlock;
                //IJ.log("mediablock["+i+"]["+j+"]="+mediablock1[i][j]);
                deviation[i][j]=rpi2*sum1/(6*(side1-2)*(side1-2));
                //IJ.log("deviation"+"["+i+"]["+j+"]="+deviation[i][j]+",   mediablock1"+"["+i+"]["+j+"]="+mediablock1[i][j]);
                //IJ.log("deviation["+i+"]["+j+"]="+deviation[i][j]);
                vecaux [z]=deviation[i][j];
                z++;
                sum1=0;
                average=0;
            }
        }
        int legvecaux=vecaux.length;
        double aux;
        //orders deviations from least to greatest
        for (z=0;z<legvecaux;z++)
            for (i=0;i<legvecaux-z-1;i++)
            {    
                if(vecaux[i]>vecaux[i+1])
                {
                  aux=vecaux[i+1];
                  vecaux[i+1]=vecaux[i];
                  vecaux[i]=aux;
                }    
            }
        deviationmin1=vecaux[0];
        int zin = 0;
        int sizemins=Math.round(h*w*per1/100);
        //IJ.log("sizemins="+sizemins);
        //IJ.log("tamano vecaux="+legvecaux);
        //finds the block with the standard deviation as small 
        //as possible that is within a Gaussian distribution
        double [] vaux2 =new double [h*w];
        if(mer==true && m1==true)
        {   
            aux=0;
            for(z=0;z<legvecaux;z++)
            {   
                if(aux>0)
                    break;
                for(j=0;j<h;j++)
                {   
                    if(aux>0)
                        break;
                    for(i=0;i<w;i++)
                    {
                       //IJ.log("deviation["+i+"]["+j+"]="+deviation[i][j]);
                        if(vecaux[z]==deviation[i][j])
                            if((int)(mediablock1[i][j]-(3*vecaux[z]))>=0&&(int)(mediablock1[i][j]+(3*vecaux[z]))<=255)
                            {
                                //IJ.log("z="+z);
                                deviationmin1=vecaux[z];
                                aux=2;
                                zin=z;
                                break;
                            }
                        if(z==legvecaux-1-sizemins)
                        {    
                            deviationmin1=vecaux[legvecaux-1-sizemins];
                            zin=legvecaux/4;
                            aux=2;
                            break;
                        }
                    }
                }
            }
        }
        if (mer==true && m1==true)
        {
            if(fa==0)
                IJ.log("desmin1_pol="+deviationmin1);
            else
                IJ.log("desmin1_exp="+deviationmin1);
        }
        else
            IJ.log("desmin1="+deviationmin1);
        //IJ.log("desmin1="+deviationmin1);
        //IJ.log("zin="+zin);
        //select homogeneous blocks
        int nh1=0;
        homogeneous1=new int[w][h];
        for(j=0;j<h;j++)
            for(i=0;i<w;i++)
                for(z=zin;z<sizemins+zin;z++)
                    if(vecaux[z]==deviation[i][j])
                    {    
                        homogeneous1[i][j]=1;
                        nh1++;
                        //IJ.log("homogeneous1["+i+"]["+j+"]");
                        break;
                    }  
        //*****************************************************************
        //step 2
        int r2=(int) Math.round(k*deviationmin1);
        if(r2<3)
            r2=3;
        else if(r2>=side1)
            r2=side1-1;
        if (mer==true && m1==true)
        {
            if(fa==0)
                IJ.log("r2_pol="+r2);
            else
                IJ.log("r2_exp="+r2);
        }
        else
            IJ.log("r2="+r2);
        //Calculation of the value of the adjustment factor
        double q=deviationmin1;
        if(mer==true && m2==true)
        {   
           if(fa==0)
           {   
               IJ.log("pol");
               div=(float) (8.09-(0.189*q)+(0.005*q*q)-(q*q*q*0.00003779));
           }
           else
           {   
               IJ.log("exp");
               double aa=8.8912;
               double rr=-0.117;
               div=  (float)(aa*(Math.pow(q,rr)));
           }  
       }
       else
           div=6;
        int bib=(int)(side1/r2);//number of small blocks  for side big block
        int wsb=w*bib;//small blocks number for image width
        int hsb=h*bib;//small blocks number for image height
        //IJ.log("num de bloques 2="+wsb*hsb);
        int sideh2;
        int sidew2;
        homogeneous2=new int[wsb][hsb];
        double [] vecaux2=new double[nh1*bib*bib];
        deviation2=new double[wsb][hsb];
        
        z=0;
        //calculation of the standard deviation of each small block 
        //within the initial homogeneous blocks
        for(j=0;j<h;j++)
        {
            sideh=j*side1;
            for(i=0;i<w;i++)//i and j point to the homogeneous block of the first image
            {
                sidew=i*side1;
                if(homogeneous1[i][j]==1)
                {
                    //IJ.log("*-*-*-*-*-*-*-*-*-*-*-*");
                    //IJ.log("bloque1["+i+"]["+j+"]");
                    for(jj=0;jj<bib;jj++)
                    {
                        sideh2=jj*r2;
                        for(ii=0;ii<bib;ii++)//ii y jj apoint to the sub-block
                        {
                            sidew2=ii*r2;
                            //IJ.log("**************************");
                            //IJ.log("bloque2["+ii+"]["+jj+"]");
                            for(int jjj=sideh+sideh2;jjj<sideh+sideh2+r2;jjj++)
                            {
                                for(int iii=sidew+sidew2;iii<sidew+sidew2+r2;iii++)
                                {
                                    //IJ.log("**************************");
                                    //IJ.log("pixel[iii][jjj]="+iii+","+jjj);
                                    if(0>ima2[iii][jjj])
                                        sum2+=ima2[iii][jjj]*-1;
                                    else
                                        sum2+=ima2[iii][jjj];
                                }
                            }
                            deviation2[ii+(i*bib)][jj+(j*bib)]=rpi2*sum2/(div*(r2-2)*(r2-2));
                            //IJ.log("deviation2["+ii+(i*bib)+"]["+jj+(j*bib)+"]="+deviation2[ii+(i*bib)][jj+(j*bib)]);
                            homogeneous2[ii+(i*bib)][jj+(j*bib)]=1;//<<<---------------------
                            vecaux2 [z]=deviation2[ii+(i*bib)][jj+(j*bib)];
                            z++;
                            sum2=0;
                            //deviationmin=(float)Math.min(deviationmin,deviation2[ii+(i*w2)][jj+(j*h2)]);
                        }
                    }
                }
            }
        }
        //orders deviations from least to greatest
        //IJ.log("total bloques 2 ="+vecaux2.length);
        int sizemins2=Math.round(vecaux2.length*per2/100);
        //IJ.log("num de bloques 2 homo="+sizemins2);
        for (z=0;z<vecaux2.length;z++)
            for (i=0;i<((vecaux2.length)-z-1);i++)
            {    
                if(vecaux2[i]>vecaux2[i+1])
                {
                  aux=vecaux2[i+1];
                  vecaux2[i+1]=vecaux2[i];
                  vecaux2[i]=aux;
                }    
            }
        if (mer==true && m2==true)
        {
            if(fa==0)
                IJ.log("desmin2_pol="+vecaux2[0]);
            else
                IJ.log("desmin2_exp="+vecaux2[0]);
        }
        else
            IJ.log("desmin2="+vecaux2[0]);
        /*for (i = 0; i< vecaux2.length; i++)
        {
          IJ.log("vecaux2["+i+"]="+vecaux2[i]);
        }*/
        //select small homogeneous blocks
        for(j=0;j<hsb;j++)
            for(i=0;i<wsb;i++)
                for(z=0;z<sizemins2;z++)
                    if(homogeneous2[i][j]==1)
                        if(vecaux2[z]==deviation2[i][j])
                        {    
                            homogeneous2[i][j]=2;
                            //IJ.log("homogeneous2["+i+"]["+j+"]");
                            break;
                        }
        //average of standard deviations
        average=0;
        for(i=0;i<sizemins2;i++)
            average+=vecaux2[i];
        sigma=average/sizemins2;
        //IJ.log("sigma="+sigma);
        if (mer==true && m2==true)
        {
            if(fa==0)
                IJ.log("sigma_pol="+sigma);
            else
                IJ.log("sigma_exp="+sigma);
        }
        else
            IJ.log("sigma="+sigma);
        double[][] iend=new double[width][height];
        double[][] mask_nb=new double[width][height];
        int numcuadros=0;
        //color homogeneous areas of black or white
        for (i=0;i<width;i++) 
            for (j=0;j<height;j++) 
                iend[i][j]=ip.getPixel(i,j);
        for(j=0;j<h;j++)
        {
            sideh=j*side1;
            for(i=0;i<w;i++)//i and j point to the homogeneous block of the first image
            {
                sidew=i*side1;
                if(homogeneous1[i][j]==1)
                {
                    //IJ.log("*-*-*-*-*-*-*-*-*-*-*-*");
                    //IJ.log("bloque1["+i+"]["+j+"]");
                    for(jj=0;jj<bib;jj++)
                    {
                        sideh2=jj*r2;
                        for(ii=0;ii<bib;ii++)//ii y jj apoint to the sub-block
                        {
                            sidew2=ii*r2;
                            if(homogeneous2[ii+(i*bib)][jj+(j*bib)]==2)
                            {
                                //IJ.log("_______________________");
                                //IJ.log("bloque2["+ii+"]["+jj+"]");
                                //posX.add(sidew+sidew2);
                                //posY.add(sideh+sideh2);
                                numcuadros++;
                                //std.add(deviation2[ii+(i*bib)][jj+(j*bib)]);
                                for(int jjj=sideh+sideh2;jjj<sideh+sideh2+r2;jjj++)
                                    for(int iii=sidew+sidew2;iii<sidew+sidew2+r2;iii++)
                                    {
                                        iend[iii][jjj]=col;
                                        mask_nb[iii][jjj]=255;
                                    }
                            }    
                        }
                    }
                }
            }
        }
        if (iha==true)
        {
            ImagePlus impH=NewImage.createByteImage("ESTIMACION DE RUIDO ",width,height,1,NewImage.FILL_BLACK);
            ImageProcessor ipH=impH.getProcessor();
            byte[] pixelss=(byte[])ipH.getPixels();
            for(j=0;j<height;j++)
            {
                for(i=0;i<width;i++)
                {
                    pixelss[j*width+i]=(byte)iend[i][j];
                }
            }
            impH.show();
        }
        if(cutmask==true)
        {
            ImagePlus impH_1=NewImage.createByteImage("ESTIMACION DE RUIDO_"+r2 ,width,height,1,NewImage.FILL_BLACK);
            ImageProcessor ipH_1=impH_1.getProcessor();
            byte[] pixelss1=(byte[])ipH_1.getPixels();
            for(j=0;j<height;j++)
                for(i=0;i<width;i++)
                    pixelss1[j*width+i]=(byte)mask_nb[i][j];
            impH_1.show();  
        }
        
        TFin = System.currentTimeMillis(); //end time
        tiempo = TFin - TInicio; //compute of  execution time in milliseconds
        if (mer==true)
        {
            if(fa==0)
                IJ.log("time(ms)_pol="+tiempo);
            else
                IJ.log("time(ms)_exp="+tiempo);
        }
        else
            IJ.log("time(ms)="+tiempo);
        IJ.log("************************************");
        //GuardarDatos(posX,posY,std);
        //IJ.log("# CUADROS : " + numcuadros);
        //IJ.log("# posx : " + posX.size());
    }
//    void GuardarDatos(List<Integer> posX,List<Integer> posY,List<Double> std){
//        
//        StringBuilder sb = new StringBuilder();
//        sb.append("x, y, std\n");
//        // Append strings from array
//        for (int i=0;i<posX.size();i++) {
//         sb.append(Integer.toString(posX.get(i))).append(", ");
//         sb.append(Integer.toString(posY.get(i))).append(", ");
//         sb.append(Double.toString(std.get(i))).append("\n");
//        }
//        String saveString = IJ.saveString( sb.toString(),dir+"/"+fname);
//        IJ.log(saveString);
//    }
}
