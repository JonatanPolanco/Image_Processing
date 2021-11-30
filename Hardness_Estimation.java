/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import static ij.plugin.filter.PlugInFilter.DOES_8C;
import static ij.plugin.filter.PlugInFilter.DOES_8G;
import ij.process.ByteProcessor;
import ij.process.FloodFiller;
import ij.process.ImageProcessor;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.logging.Level;
/**
 *
 * @author ant
 */
public class Hardness_Estimation implements PlugInFilter
{
    ImagePlus imp;
    ByteProcessor ipR;
    ByteProcessor ipInput;
    
    public static int method=3;
    
    boolean showAxis=false;
    //Radio a descartar, valor de 0 a 1
    double rUmbral=0.2;
    int width;
    int height;
    byte output [];
    byte pixValue [];
    int unit;
    double force;
    double um;
    double pix;
    double coef_cuadrado;
    int cx,cy; //Centro de la figura
    int px0,py0;//Cordenadas iniciales del perímetro
    //Vecores de coordendas del perímetro
    int px []; 
    int py [];
    double pv[]; //valor del perímetro
    //Vectores de coordenadas polares
    double r[];
    double a[];
    double HV_Perimeter;
    double HV_Hough;
    
    @Override
    
    public int setup(String arg, ImagePlus imp)
    {
        this.imp = imp;
        width=imp.getWidth();//image width
        height=imp.getHeight();//image height
           
        
        GenericDialog gd = new GenericDialog("Hardness_Estimation");
        String [] metodos = {"Covariance_matrix",
            "Standard_deviational_ellipse",
            "Maximum_radius_LSM",
            "Maximum_radius",
        };
        String [] unidades = {"N",
            "Kgf"
        };
        
        //gd.addChoice("Method", metodos, metodos[method]);
        gd.addNumericField("applied force",1,3); 
        String[] items = {"Kgf", "N"};
        gd.addChoice("Unit", items, items [0]);   
        gd.addNumericField("micrometros",10,4);
        gd.addNumericField("Pixels",10,4);
        
        gd.addCheckbox("Show_axis", showAxis);
        gd.addMessage("10 micrometers = 150 pixels");
        gd.addMessage("20 micrometers = 298 pixels");
        gd.showDialog();
        if (gd.wasCanceled())
            return DONE;
        //method = gd.getNextChoiceIndex();
        force=(float) gd.getNextNumber();
        unit = gd.getNextChoiceIndex();// 0 kff, 1 N
        um=(float) gd.getNextNumber();
        pix=(float) gd.getNextNumber();
        
        showAxis = gd.getNextBoolean();
        return IJ.setupDialog(imp,DOES_8G+DOES_8C);
    }

    @Override
    public void run(ImageProcessor ip)
    {
        // Rotate image (Square to diamond)
        int size = width*height;
        ipInput=(ByteProcessor) ip;
        pixValue = (byte[]) ip.getPixels();
        output = pixValue.clone();
        ipR= new ByteProcessor(width,height,output);
        int x,y,r,s,i;
        double[][]m=new double [3][3];
        
        
        Prefs.blackBackground = true;
        
        //SE APLICA FILL HOLES
        FloodFiller ff = new FloodFiller(ipR);
        ipR.setColor(127);
        ff.fill(0, 0);
        for ( i = 0; i < size; i++) {
            if ((0xff&output[i]) == 127) {
                output[i] = (byte) 0;
            } else {
                output[i] = (byte) 255;
            }
        }
        
        //Utilizando areas y asumiendo que es cuadrado
        int xmin,xmax, ymin, ymax;
        xmin=width;
        xmax=0;
        ymin=height;
        ymax=0;
        //
        i=0;
        for(y=0;y<height;y++)
            for(x=0;x<width;x++)
            {
                if(output[i]!=0)
                {
                    xmin=xmin>x?x:xmin;
                    xmax=xmax<x?x:xmax;
                    ymin=ymin>y?y:ymin;
                    ymax=ymax<y?y:ymax;
                    
                    for (r=0;r<=2;r++)
                        for (s=0;s<=2;s++)
                            m[r][s]+=Math.pow(x,r)*Math.pow(y,s);
                }
                i++;
            }
        
        
        if (m[0][0]==0)
            {
                cx=0;
                cy=0;
            }
        else
        {
            cx=(int) Math.round( m[1][0]/m[0][0]);
            cy=(int) Math.round( m[0][1]/m[0][0]);
        }
        
        for(i=0;i<size;i++) output[i]=pixValue[i];
        //drawLine((int) centroidx,(int) centroidy, -1/p, 127);
        
        polarPerimeter(m,true);
              
        //IJ.log("centro:"+cx+", "+cy);

        
        //ImagePlus impR= new ImagePlus("Result",ipR );
        //impR.show();
        
        //Se procede a ejecutar el método de Hough
        run_Hough(ip);
    }
   
    public void run_Hough(ImageProcessor ip) {

        byte imageValues[]= (byte[])ip.getPixels(); // Raw image (returned by ip.getPixels())
        byte pixelValue;
        Rectangle r = ip.getRoi();
        int houghValues[][]; // Hough Space Values
        int width= r.width; // Original Image width
        int height= r.height;  // Original Image height
        int hWidth= 500; // HoughSpace Width
        int tmp = Math.max(height,width);
        int hHeight= (int) (Math.sqrt(2)*tmp);  // HoughSpace Height
        int threshold = 45; // Percent related to the maximum hough value.
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
        ByteProcessor linesip = new ByteProcessor(width, height);
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
        
        
        // Finaliza Hough 
        
         Prefs.blackBackground = true;
        int foreground = 0, background = 0;
        int aux_fg = Prefs.blackBackground ? 255 : 0;
        foreground = linesip.isInvertedLut() ? 255 - aux_fg : aux_fg;
        background = 255 - foreground;
        
        //SE APLICA FILL HOLES
        FloodFiller ff = new FloodFiller(linesip);
        linesip.setColor(127);

        for (int y_ = 0; y_ < height; y_++) {
            if (linesip.getPixel(0, y_) == background) {
                ff.fill(0, y_);
            }
            if (linesip.getPixel(width - 1, y_) == background) {
                ff.fill(width - 1, y_);
            }
        }
        for (int x_ = 0; x_ < width; x_++) {
            if (linesip.getPixel(x_, 0) == background) {
                ff.fill(x_, 0);
            }
            if (linesip.getPixel(x_, height - 1) == background) {
                ff.fill(x_, height - 1);
            }
        }
        byte[] pixels = (byte[]) linesip.getPixels();
        int n = width * height;
        for (int i_ = 0; i_ < n; i_++) {
            if (pixels[i_] == 127) {
                pixels[i_] = (byte) background;
            } else {
                pixels[i_] = (byte) foreground;
            }
        }

           
        //Erode and dilate (4 series of 10 iterations)
        int iterations = 10; 
         count = 1;

        for (int jd = 0; jd < 40; jd++) {
            if (jd < iterations) {
                (linesip).erode(count, background);
            }
            else if (jd < iterations*2) {
                (linesip).dilate(count, background);
            }
            else if (jd < iterations*3) {
                linesip.erode(count, background);
            }
            else{
                linesip.dilate(count, background);
            }
        }
        
        
        //imp.updateAndDraw();
        linesip.findEdges();
        linesip.threshold(127);
        linesip.dilate(1, 0);
        linesip.skeletonize(255);
        Get_Corners(linesip);
        //new ImagePlus("Lines Found", linesip).show();
        
    }
    
    void Get_Corners(ByteProcessor ip)
    {
        ipInput=ip;
        int i;
        int perimetro =perimeter();
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        // Estima las esquinas como los máximos locales del radio
        double [] r10= meanPerimeter(10);
        // Calcula el radio máximo
        int irMax= maxRadiusIndex(r10,0,r10.length-1);
        
        int xCorner[]=new int[4];
        int yCorner[]= new int[4];
        int iCorner[]= new int[4];
        xCorner[0]=px[irMax];
        yCorner[0]=py[irMax];
        iCorner[0]=irMax;
        // Calcula las otras esquinas
        // avanza la octava parte del perimetro y busca un máximo en esa
        // parte
        for(int k=1;k<4;k++){
            irMax+=perimetro/8;
            irMax=maxRadiusIndex(r10,irMax,irMax+perimetro/4);

            xCorner[k]=px[irMax];
            yCorner[k]=py[irMax];
            iCorner[k]=irMax;
        }
        ByteProcessor ipNew = new ByteProcessor(width,height,output);
        ipR = ipNew;
        ipR = ip;
        for( i = 0;i<4;i++){
            int xa,ya, xb,yb;
            xa=xCorner[i];
            ya=yCorner[i];
            xb=xCorner[(i+1)%4];
            yb=yCorner[(i+1)%4];
            drawLine(xa,ya,xb,yb,100,false);
            //error+=lineError(xa,ya,xb,yb,iCorner[i],iCorner[(i+1)%4]);
        }
        
        double cuadrado =Square_Coef(xCorner, yCorner);
        IJ.log("error aproximación a cuadrado" + " = "+coef_cuadrado );
        //IJ.log("coeficiente metodo por Hough" + "= "+cuadrado );
        
        double error = 0.1;
        if (coef_cuadrado < 1 + error ) {
            IJ.log("El mejor metodo es perimetro");
            new ImagePlus("Perimeter Method", ipNew).show();
            
            if(unit==0)
       {
           IJ.log("Dureza HV Metodo Perimetro"+ "" +force+" Kgf= "+HV_Perimeter);
       }
            else
       {
           IJ.log("Dureza HV Metodo Perimetro"+"" +force+" N= "+HV_Perimeter);     
       }
        
            
        }
        else
                {
                    IJ.log("El mejor metodo es Hough");
                    new ImagePlus("Hough Method", ipInput).show();
                     //Dp1: diagonal1
       //Dp2: diagonal 2
       
       double Dp1,Dp2,D1,D2,D,HV;

       
       int dXDiagonal1 = xCorner[0]-xCorner[2];
       int dYDiagonal1 = yCorner[0]-yCorner[2];
       int dXDiagonal2 = xCorner[1]-xCorner[3];
       int dYDiagonal2 = yCorner[1]-yCorner[3];
       Dp1=Math.sqrt(dXDiagonal1*dXDiagonal1+ dYDiagonal1*dYDiagonal1);
       //IJ.log("Pixel-Distance D1=Right-Left="+Dp1);
       Dp2=Math.sqrt(dXDiagonal2*dXDiagonal2+ dYDiagonal2*dYDiagonal2);
       //IJ.log("Pixel-Distance D2=Down-up="+Dp2);
       //D1=((Dp1*20)/298)/1000; D2=((Dp2*20)/298)/1000;
       D1=((Dp1*um)/pix)/1000;
       D2=((Dp2*um)/pix)/1000;
       //IJ.log("Distance (mm) D1=Right-Left="+D1);
       //IJ.log("Distance (mm) D2=Down-up="+D2);
       D=(D1+D2)/2;
       //IJ.log("Average-Real-Distance (mm)="+D);
       if(unit==0)
       {
           HV=(1.8544*force)/(D*D);
           IJ.log("Dureza HV Metodo Hough"+ "" +force+" Kgf= "+HV);
       }
       else
       {
           HV=(0.1891*force)/(D*D);
           IJ.log("Dureza HV Metodo Hough"+"" +force+" N= "+HV);     }
                }
        
       
    }
    
    double Square_Coef(int []xCorner, int []yCorner)
    {
       int dXDiagonal1 = xCorner[0]-xCorner[2];
       int dYDiagonal1 = yCorner[0]-yCorner[2];
       int dXDiagonal2 = xCorner[1]-xCorner[3];
       int dYDiagonal2 = yCorner[1]-yCorner[3];
       
       double Dp1=Math.sqrt(dXDiagonal1*dXDiagonal1+ dYDiagonal1*dYDiagonal1);
       double Dp2=Math.sqrt(dXDiagonal2*dXDiagonal2+ dYDiagonal2*dYDiagonal2);
       double area = Dp1*Dp2/2;
       double lado_estimado = Math.sqrt(area);
       double perimetro =0;
       
        for (int i = 0; i < 4; i++) {
            
            int dx = xCorner[i]-xCorner[(i+1)%4];
            int dy = yCorner[i]-yCorner[(i+1)%4];
            
            double lado = Math.sqrt(dx*dx+dy*dy);
            perimetro += lado;
        }
        double coef = Math.abs(perimetro-4*lado_estimado)/4;
        return coef; 
    }
    
    /**
     * Calcula el perímetro de la figura y actualiza los vectores px, py, pv
     * @return perímetro de la curva
     */
    int perimeter(){
        int i,x,y;
        x=cx;
        while(x<width && ipInput.get(x, cy)== 0) x++;
        if(x>=width){
            IJ.error("Not found perimeter");
            return 0;
        }
        y=cy;
        int perimetro=0;
        px0=x;
        py0=y;
        while(perimetro == 0 || (!(x==px0 && y==py0) && ipInput.get(x, y)==255)){
            ipInput.set(x, y, 127);
            if(ipInput.get(x+1,y)==255){
                x++;
            }else if (ipInput.get(x+1,y+1)==255){
                x++;
                y++;
            }else if (ipInput.get(x,y+1)==255){
                y++;
            }else if (ipInput.get(x-1,y+1)==255){
                x--;
                y++;
            }else if (ipInput.get(x-1,y)==255){
                x--;
            }else if (ipInput.get(x-1,y-1)==255){
                x--;
                y--;
            }else if (ipInput.get(x,y-1)==255){
                y--;
            }else if (ipInput.get(x+1,y-1)==255){
                x++;
                y--;
            }else{
                //Un punto para terminar
                x++;
                y++;
                //IJ.log("Fin x,y: "+x+", "+y);
            }
            perimetro++;
        }
        int diferencia = (x-px0)+ (y-py0);
        if(diferencia>2 ){
            ipInput.set(x, y, 126);
            ipInput.set(x+1, y, 126);
            ipInput.set(x-1, y, 126);
            ipInput.set(x, y+1, 126);
            ipInput.set(x, y-1, 126);
            IJ.log("Inició en: "+px0+","+py0);
            IJ.log("Terminó en: "+x+","+y);
            IJ.error("La curva no es cerrada");
        }
        //IJ.log("Perimetro: "+perimetro);
        px= new int[perimetro];
        py= new int[perimetro];
        r=new double[perimetro];
        a=new double[perimetro];
        pv = new double[perimetro];
        perimetro=0;
        x=px0;
        y=py0;
        while(perimetro == 0 || (!(x==px0 && y==py0) && ipInput.get(x, y)==127)){
            ipInput.set(x, y, 255);
            pv[perimetro]=perimetro;
            px[perimetro]=x;
            py[perimetro]=y;
            r[perimetro]=Math.sqrt((x-cx)*(x-cx)+(y-cy)*(y-cy));
            a[perimetro]=Math.atan2(y-cy, x-cx);
            if(ipInput.get(x+1,y)==127){
                x++;
            }else if (ipInput.get(x+1,y+1)==127){
                x++;
                y++;
            }else if (ipInput.get(x,y+1)==127){
                y++;
            }else if (ipInput.get(x-1,y+1)==127){
                x--;
                y++;
            }else if (ipInput.get(x-1,y)==127){
                x--;
            }else if (ipInput.get(x-1,y-1)==127){
                x--;
                y--;
            }else if (ipInput.get(x,y-1)==127){
                y--;
            }else if (ipInput.get(x+1,y-1)==127){
                x++;
                y--;
            }else{
                //Un punto para terminar
                x++;
                y++;
                //IJ.log("Fin x,y: "+x+", "+y);
            }
            perimetro++;
        }
        return perimetro;
    }
    
    /**
     * Obtiene el perímetro de la figura en coordenadas polares. Tiene en cuenta
     * la rotación de ejes, por tanto una medida de ángulo positivo se toma en 
     * el sentido horario.
     * 
     * @param simple indica si utiliza los ejes simples si es true, en caso
     *              contrario descarta unos valores en las esquinas y hace
     *              mínimos cuadrados
     */
    void polarPerimeter(double [][] m, boolean simple){
        // Halla el punto del cual partir
        // a la derecha del centro
        int i;
        int perimetro =perimeter();
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        // Estima las esquinas como los máximos locales del radio
        double [] r10= meanPerimeter(10);
        // Calcula el radio máximo
        int irMax= maxRadiusIndex(r10,0,r10.length-1);
        
        int xCorner[]=new int[4];
        int yCorner[]= new int[4];
        int iCorner[]= new int[4];
        xCorner[0]=px[irMax];
        yCorner[0]=py[irMax];
        iCorner[0]=irMax;
        // Calcula las otras esquinas
        // avanza la octava parte del perimetro y busca un máximo en esa
        // parte
        for(int k=1;k<4;k++){
            irMax+=perimetro/8;
            irMax=maxRadiusIndex(r10,irMax,irMax+perimetro/4);

            xCorner[k]=px[irMax];
            yCorner[k]=py[irMax];
            iCorner[k]=irMax;
        }
        
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        // Calcula los ejes perpendiculares a los lados
        int xSide[] = new int[4];
        int ySide[] = new int[4];
        int iSide[] = new int[4];
        int irMin;
        for(int k=0;k<4;k++){
            irMin=minRadiusIndex(r10,iCorner[k],iCorner[(k+1)%4]);

            xSide[k]=px[irMin];
            ySide[k]=py[irMin];
            iSide[k]=irMin;
        }
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        // Estima la longitud de un lado como la raíz cuadrada del área
        double lado=Math.sqrt(m[0][0]);
        //double ladoB=perimetro/4.0;
        //IJ.log("ladoA: "+ladoA+"  ladoB: "+ladoB);
        //IJ.log("lado promedio: "+(ladoA+ladoB)/2);
        //double lado= ladoA;// ladoA<ladoB?ladoA:ladoB;
        //rUmbral*=lado/2;
        
        double side0=distancia(xSide[0],ySide[0],xSide[2],ySide[2]);
        double side1=distancia(xSide[1],ySide[1],xSide[3],ySide[3]);
        
        //IJ.log("side0: "+side0+"  side1: "+side1);
        
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        // Estima las esquinas del cuadrado teniendo en cuenta tres puntos
        // en medio de cada lado estimado según las esquinas dadas por
        // las diagonales
        
        // Calcula las rectas según las esquinas halladas por las diagonales
        LineImage [] rectas= new LineImage[4];
        double [] errores = new double[4];
        for(i=0;i<4;i++){
            rectas[i]=new LineImage(xCorner[i],yCorner[i],xCorner[(i+1)%4],yCorner[(i+1)%4]);
            //drawLine(rectas[i],50+10*i,false);
            errores[i]=lineError(rectas[i],iCorner[i],iCorner[(i+1)%4]);
            //IJ.log("("+i+") error:"+errores[i]);
        }
        if(showAxis){
            LineImage diagonal;
            for(i=0;i<4;i++){
                diagonal=new LineImage(xCorner[i],yCorner[i],xCorner[(i+2)%4],yCorner[(i+2)%4]);
                drawLine(diagonal,50,false);
                errores[i]=lineError(rectas[i],iCorner[i],iCorner[(i+1)%4]);
                //IJ.log("("+i+") error:"+errores[i]);
            }
        }
        
        double error=0;
        LineImage sideLine;
        LineImage lineStart;
        LineImage lineEnd;
        Point p;
        
        int incremento = (int)Math.round(lado/4);
        for(i=0;i<4;i++){
            
            int start = (iSide[i]-(int)Math.round(5*lado/12)+perimetro)%perimetro;
            int end = start+(int)Math.round(lado/3);
            lineStart=rectas[(i+3)%4];
            lineEnd =rectas[(i+1)%4];
            //if(i!=3) continue;
            //drawLine(lineStart,100,false);
            //drawLine(lineEnd,50,false);
            for(int k=0;k<3;k++){
                start+=incremento*k;
                end+=incremento*k;
                //IJ.log("(k-"+k+") start: "+start + "   end: "+end);
                
                sideLine = new LineImage(getPoints(start,end));
                
                
                
                //Esquina inicio
                p=sideLine.intersection(lineStart);
                if(p.x==0 && p.y==0)
                    continue;
                sideLine.x0=p.x;
                sideLine.y0=p.y;
                //IJ.log("   x0: "+sideLine.x0+"   y0:"+sideLine.y0);
                // Esquina final
                p=sideLine.intersection(lineEnd);
                if(p.x==0 && p.y==0)
                    continue;
                sideLine.x1=p.x;
                sideLine.y1=p.y;
                
                
                //drawLine(sideLine,150+k*10,false);
                //drawLine(px[start%perimetro],py[start%perimetro],
                //        px[end%perimetro],py[end%perimetro],200+10*k,true);
                error= lineError(sideLine,iCorner[i],iCorner[(i+1)%4]);
                
                if(error<errores[i]){
                    rectas[i]=sideLine;
                    errores[i]=error;
                    //IJ.log("("+i+"--"+k+") error:"+error);
                    //IJ.log("   m: "+sideLine.m+"   b:"+sideLine.b);
                }
            }
        }
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        //Halla las esquinas con las rectas que mejor aproximan los lados
        for(i=0;i<4;i++){
            p = rectas[i].intersection(rectas[(i+1)%4]);
            xCorner[(i+1)%4]=p.x;
            yCorner[(i+1)%4]=p.y;
            drawLine(rectas[i],100+10*i,false);
        }
        
        //Dibuja los lados conocidas las esquinas
        for( i = 0;i<4;i++){
            int xa,ya, xb,yb;
            xa=xCorner[i];
            ya=yCorner[i];
            xb=xCorner[(i+1)%4];
            yb=yCorner[(i+1)%4];
            //drawLine(xa,ya,xb,yb,100+10*k,false);
            error+=lineError(xa,ya,xb,yb,iCorner[i],iCorner[(i+1)%4]);
        }
        error/=4;
        angulo(xCorner,yCorner);
        //IJ.log("Error ajuste: "+error);
       coef_cuadrado= Square_Coef(xCorner, yCorner);
        
       //Dp1: diagonal1
       //Dp2: diagonal 2
       
       double Dp1,Dp2,D1,D2,D,HV,HV1;

       
       int dXDiagonal1 = xCorner[0]-xCorner[2];
       int dYDiagonal1 = yCorner[0]-yCorner[2];
       int dXDiagonal2 = xCorner[1]-xCorner[3];
       int dYDiagonal2 = yCorner[1]-yCorner[3];
       Dp1=Math.sqrt(dXDiagonal1*dXDiagonal1+ dYDiagonal1*dYDiagonal1);
       //IJ.log("Pixel-Distance D1=Right-Left="+Dp1);
       Dp2=Math.sqrt(dXDiagonal2*dXDiagonal2+ dYDiagonal2*dYDiagonal2);
       //IJ.log("Pixel-Distance D2=Down-up="+Dp2);
       //D1=((Dp1*20)/298)/1000; D2=((Dp2*20)/298)/1000;
       D1=((Dp1*um)/pix)/1000;
       D2=((Dp2*um)/pix)/1000;
       //IJ.log("Distance (mm) D1=Right-Left="+D1);
       //IJ.log("Distance (mm) D2=Down-up="+D2);
       D=(D1+D2)/2;
       //IJ.log("Average-Real-Distance (mm)="+D);
       if(unit==0)
       {
           HV_Perimeter=(1.8544*force)/(D*D);
           //IJ.log("Dureza HV Metodo Perimetro"+ "" +force+" Kgf= "+HV_Perimeter);
       }
       else
       {
           HV_Perimeter=(0.1891*force)/(D*D);
           //IJ.log("Dureza HV Metodo Perimetro"+"" +force+" N= "+HV_Perimeter);     
       }
        

    }
    
    
    /**
     * Calcula el ángulo de la figura a partir de la diagonal mas larga
     * tomando las coordenadas de las esquinas
     * @param xCorner
     * @param yCorner
     * @return 
     */
    double angulo(int[] xCorner, int[] yCorner){
        //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        // Calcula el ángulo a partir de la diagonal mas extensa
        double angulo;
        
        double diag1=distancia(xCorner[0],yCorner[0],xCorner[2],yCorner[2]);
        double diag2=distancia(xCorner[1],yCorner[1],xCorner[3],yCorner[3]);
        
        double dx, dy;
        if(diag2>diag1){
            dx=xCorner[3]-xCorner[1];
            dy=yCorner[3]-yCorner[1];
            
        } else{
            dx=xCorner[2]-xCorner[0];
            dy=yCorner[2]-yCorner[0];
            
        }
        if(dx==0){
            angulo=Math.PI/2;
        }else if(dy==0){
            angulo = 0;
        } else{
            angulo=Math.atan(dy/dx);
        }
        //IJ.log("angulo: "+(angulo*180/Math.PI));
        return angulo;
    }
    
    double distancia(int x0, int y0, int x1, int y1){
        x1=x1-x0;
        y1=y1-y0;
        return Math.sqrt(x1*x1+y1*y1);
    }
    
    /**
     * Obtiene los puntos desde el inicio hasta el final
     * @param start
     * @param end
     * @return 
     */
    
    int [][] getPoints(int start, int end){
        int[][] posxy=new int[end-start+1][2];
        for(int i = start;i<=end;i++){
            posxy[i-start][0]=px[i%r.length];
            posxy[i-start][1]=py[i%r.length];
        }
        return posxy;
    }
    
    /**
     * Obtiene el vector de coordenadas x,y de un lado, teniendo en cuenta
     * que los puntos a tener en cuenta deben estar alejados a un radio de
     * extremo que identifica la esquina
     * @param start
     * @param end
     * @return 
     */
    int [][] sidePos(int start, int end){
        IJ.log("side pos - start, end: "+start+ ", "+end);
        //Calcula el radio a partir del cual se tomarán los
        // datos para
        int minIndex=start;
        int maxIndex=end;
        //Coordendas de inicio
        int xs = px[start%r.length];
        int ys= py[start%r.length];
        //Coordenadas del final
        int xe = px[end%r.length];
        int ye= py[end%r.length];
        for(int i = start;i<=end;i++){
            if(distancia(xs,ys,px[i%r.length],py[i%r.length]) <= rUmbral){
                minIndex=i;
            }
            if(distancia(xe,ye,px[i%r.length],py[i%r.length]) <= rUmbral && maxIndex==end){
                maxIndex=i;
            }
            
        }
        
        int[][] posxy=new int[maxIndex-minIndex+1][2];
        for(int i = minIndex;i<=maxIndex;i++){
            posxy[i-minIndex][0]=px[i%r.length];
            posxy[i-minIndex][1]=py[i%r.length];
        }
        return posxy;
    }
    /**
     * Obtiene la pendiente e intercepto de una recta que pasa por dos puntos
     * en el formato que se obtiene con la regresión linea, el cual incluye
     * un formato especial para rectas verticales y horizontales.
     * @return 
     */
    double [] getLineCoeff(int x0, int y0, int x1, int y1){
        double [] coeff= new double[5];
        double dx= x1-x0;
        double dy= y1-y0;
         coeff[2]=1;//Error
        //Analiza el caso de rectas horizontales y verticales
        if(dx*dy == 0){
            coeff[0]=0;//Pendiente cero
            if(dx==0){
                //Vertical
                coeff[1]=x0;
               
                coeff[3]=1;//Vertical
                coeff[4]=0;//Horizontal
            }else{
                //Horizontal
                coeff[1]=y0;//coeficiente b
                coeff[3]=0;//Vertical
                coeff[4]=1;//Horizontal
            }
            return coeff;
        }
        coeff[0]=dy/dx;
        coeff[1]=y0-x0*coeff[0];
        coeff[3]=0;//Vertical
        coeff[4]=0;//Horizontal
        
        
        return coeff;
    }
    
    /**
     * Calcula la regresión lineal de x,y utilizando mínimos cuadrados sobre
     * los vectores de coordenadas almacenados en un array con px[N][xy]
     * pxy[i][0] -- xi
     * pxy[i][1] -- yi
     * 
     * @param posxy puntos
     * @return [m,b,error,vertical, horizontal] 
     */
    double[] regresion(int[][] posxy){
        double coeff[]=new double[5];
        int N= posxy.length;
        int []x=new int[N];
        int []y=new int[N];
        //IJ.log("segemento analizado: "+(end-start)+"  ["+start+", "+end+"]");
        for(int i=0;i<N;i++){
            x[i]=posxy[i][0];
            y[i]=posxy[i][1];
        }
        
        
        double Sy=0, Sx=0, Sx2=0, Sxy=0;

        //int N=Nd;//No se contará el cero
        int i;
        for(i=0;i<x.length;i++){
            
            Sx2+=x[i]*x[i];
            Sx+=x[i];
            Sy+=y[i];
            Sxy+=y[i]*x[i];
        }
        double dy=(N*Sxy-Sy*Sx);
        double dx=(N*Sx2-Sx*Sx);
        
        //Analiza el caso de rectas horizontales y verticales
        if(dx*dy == 0){
            coeff[0]=0;//Pendiente cero
            if(dx==0){
                //Vertical
                coeff[1]=Sx/N;
                coeff[2]=1;//Error
                coeff[3]=1;//Vertical
                coeff[4]=0;//Horizontal
            }else{
                coeff[1]=Sy/N;//coeficiente b
                coeff[2]=1;//Error
                coeff[3]=0;//Vertical
                coeff[4]=1;//Horizontal
            }
            return coeff;
        }
        
        
        double a=dy/dx;
        double b=(Sy-a*Sx)/N;
//        IJ.log("valor de a:"+a);
//        IJ.log("valor de b:"+b);
//        IJ.log("valor de Sx:"+Sx);
//        IJ.log("valor de Sy:"+Sy);
//        IJ.log("valor de N:"+N);
        double e=0;
        for(i=0;i<x.length;i++){
            double dif = y[i]- (a*x[i]+b);
            e+= dif*dif;
        }
        e=Math.sqrt(e);
        
        coeff[0]=a;
        coeff[1]=b;
        coeff[2]=e;
        coeff[3]=0;
        coeff[4]=0;
        return coeff;        
    }
    
    /**
     * Calcula el radio máximo en el intervalo fijado
     * @param r
     * @param start
     * @param end
     * @return 
     */
    int maxRadiusIndex( double [] r,int start, int end){
        int irMax=start; // indice del radio máximo
        if(start > end){
            end+=r.length;
        }
        //IJ.log("segemento analizado: "+(end-start)+"["+start+", "+end+"]");
        for(int i=start;i<=end;i++){
            if(r[i%r.length]>r[irMax%r.length])
                irMax=i%r.length;
            
        }
        //IJ.log("indice: "+irMax);
        return irMax;
    }
    
    int minRadiusIndex( double [] r,int start, int end){
        int irMin=start; // indice del radio mínimo
        if(start > end){
            end+=r.length;
        }
        //IJ.log("segemento analizado: "+(end-start)+"["+start+", "+end+"]");
        for(int i=start;i<=end;i++){
            if(r[i%r.length]<r[irMin%r.length])
                irMin=i%r.length;
            
        }
        //IJ.log("indice: "+irMax);
        return irMin;
    }
    
    /**
     * Calcula el promedio de 2n+1 valores del radio
     * @param n número entero positivo
     * @return 
     */
    double [] meanPerimeter(int n){
        double [] meanRadius=new double[r.length];
        double radius;
        for(int i = 0;i<r.length;i++){
            radius=0;
            for(int j=i-n;j<=i+n;j++){
                radius+=r[(j+r.length)%r.length];
            }
            radius/=(2*n+1);
            meanRadius[i]=radius;
        }
        return meanRadius;
    }
    
    void drawLine( LineImage line, int value, boolean strict){
        drawLine(line.x0,line.y0, line.x1, line.y1, value, strict );
    }
    /**
     * Dibuja una linea que pasa por el centroide en cx, cy
     * @param cx coordenada x del centro
     * @param cy coordenada y del centro
     * @param m pendiente de la recta
     * @param value valor que se asignara a los pixeles
     */
    void drawLine( int cx, int cy, double m, int value){
        ipR.setColor(value);
        
        int x0,y0, x1,y1;
        if( m!=0 ){
            x0=0;
            y0=(int) Math.round(m*(-cx)+cy);
            x1=width;
            y1= (int) Math.round(m*(width-cx)+cy);
            ipR.drawLine(x0, y0,x1, y1);
        }else{
            //Caso de los ejes coinciden con los del plano cartesiano
            ipR.setColor(127);
            ipR.drawLine(cx, 0, cx, height);
            ipR.drawLine(0, cy, width, cy);
        }
    }
    /**
     * Dibuja una linea dados dos puntos, si se indica que es strict
     * traza la linea entre los dos puntos, en caso contrario traza
     * la recta que pasa por dichos dos puntos
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @param strict 
     */
    void drawLine(int x0,int y0, int x1, int y1, int value, boolean strict){
        ipR.setColor(value);
        int dx= x1-x0;
        int dy= y1-y0;
        if(strict){
            ipR.drawLine(x0, y0, x1, y1);
        } else{
            if(dx==0){
                ipR.drawLine(x1, 0, x1, height);
            }else if(dy==0){
                ipR.drawLine(0, y1, width, y1);
            } else{
                double m = dy/(1.0*dx);
                double b = y1-m*x1;
                int xa,ya,xb,yb;
                if(m<1){
                    xa=0;
                    ya=(int) Math.round(m*xa+b);
                    xb=width;
                    yb=(int) Math.round(m*xb+b);
                }else{
                    ya=0;
                    xa=(int) Math.round((ya-b)/m);
                    yb=height;
                    xb=(int) Math.round((yb-b)/m);
                }
                ipR.drawLine(xa, ya, xb, yb);
            }
        }
    }
    
    double lineError(LineImage line, int start, int end){
        if(line.m == 0){
            IJ.log("Warning: m is zero");
        }
        if(line.x0 - line.x1 == 0 && line.m != 0){
            IJ.log("Warning: deltaX is zero and m is not zero");
        }
        if(line.x0 - line.x1 == 0 && line.m != 0){
            IJ.log("Warning: deltaY is zero and m is not zero");
        }
        return lineError(line.x0,line.y0,line.x1,line.y1, start, end);
    }
    /**
     * Calcula el error de los pixeles que coinciden con la linea
     * recta en un valor de 0 a 1.
     * @return 
     */
    double lineError(int x0, int y0, int x1, int y1, int start, int end){
        end=start>end?end+px.length:end;
        int dx = x1-x0;
        int dy = y1-y0;
        //IJ.log("\ndx: "+dx+"  dy: "+dy);
        int [] xv = px;
        int [] yv = py;
        
        if(Math.abs(dy)> Math.abs( dx)){
            //Se cambian los ejes para tener 
            // como referencia a x para calcular y
            // y generar puntos continuos
            xv=py;
            yv=px;
            int tmp =x0;
            x0=y0;
            y0=tmp;
            tmp=y1;
            y1=x1;
            x1=tmp;
            
            tmp=dx;
            dx=dy;
            dy=tmp;
        }
        
        
        //Umbral de distancia sobre la que se cuenta como
        //punto sobre la recta
        double dMax =0.1*distancia(x0,y0,x1,y1);
        
        double error=0;
        double pixAnalizados=0;
        //Toma x como variable independiente
        double m = dy/(1.0*dx);
        double b = y1-m*x1;
        int y;
        //Garantiza que x0 < x1
        int tmp=x0;
        x0=x0<x1?x0:x1;
        x1=x1>tmp?x1:tmp;
        //Recta 
        
        double e;
        //int outlayers=0;
        for(int i = start+1; i<end;i++){
            if(xv[i%px.length]>x0 && xv[i%px.length]<x1){
                y= (int) Math.round(m*xv[i%px.length]+b);
                
                dy= yv[i%px.length]-y ;
                dy= Math.abs(dy);
                if(dy<=dMax){
                    e = dy/dMax;
                    error+= e*e;
                    
                }else{
                    //e = dy/dMax;
                    //error+= e*e;
                    error+=1;
                    //outlayers+=1;
                }
                    
                pixAnalizados++;
                
                
            }
        }
        //pixAnalizados=dx;
        //pixAnalizados=pixAnalizados<2*dx/3?2*dx/3:pixAnalizados;
        
        error=pixAnalizados>0? error/pixAnalizados:100;
        //IJ.log("\n  dMax:"+dMax);
        //IJ.log("  Outlayers:"+outlayers);
        //IJ.log("  PixAnalizados:"+pixAnalizados);
        //IJ.log("  deltaX:"+(x1-x0));
        //IJ.log("  deltaY:"+(y1-y0));
        //IJ.log("  m: "+m);
        //IJ.log("  b: "+b);
        //IJ.log("  Error: "+error);
        return error;
    }
}


class LineImage2{
    public int x0;
    public int y0;
    public int x1;
    public int y1;
    
    public double m;
    public double b;
    public double error;
    private boolean horizontal;
    private boolean vertical;
    /**
     * Crea los parámetros de la linea utilizando 2 puntos
     * @param x0
     * @param y0
     * @param x1
     * @param y1 
     */
    public LineImage2(int x0, int y0, int x1, int y1){
        this.x0=x0;
        this.y0=y0;
        this.x1=x1;
        this.y1=y1;
        
        double dx= x1-x0;
        double dy= y1-y0;
        vertical=false;//Vertical
        horizontal=false;//Horizontal
        error=0;
        //Analiza el caso de rectas horizontales y verticales
        if(dx*dy == 0){
            m=0;//Pendiente cero
            if(dx==0){
                //Vertical
                b=x0;
                vertical=true;//Vertical
            }else{
                //Horizontal
                b=y0;//coeficiente b
                horizontal=true;//Horizontal
            }

        }else{
            m=dy/dx;
            b=y0-x0*m;
            
        }
    }
    /**
     * Crea la linea a partir de un conjunto de puntos utilizando
     * una regresión lineal
     * @param posxy 
     */
    public LineImage2(int[][] posxy){

        int N= posxy.length;
        int []x=new int[N];
        int []y=new int[N];
        //IJ.log("segemento analizado: "+(end-start)+"  ["+start+", "+end+"]");
        for(int i=0;i<N;i++){
            x[i]=posxy[i][0];
            y[i]=posxy[i][1];
        }
        
        
        double Sy=0, Sx=0, Sx2=0, Sxy=0;

        //int N=Nd;//No se contará el cero
        int i;
        for(i=0;i<x.length;i++){
            
            Sx2+=x[i]*x[i];
            Sx+=x[i];
            Sy+=y[i];
            Sxy+=y[i]*x[i];
        }
        double dy=(N*Sxy-Sy*Sx);
        double dx=(N*Sx2-Sx*Sx);
        
        vertical=false;//Vertical
        horizontal=false;//Horizontal
        //Analiza el caso de rectas horizontales y verticales
        if(dx*dy == 0){
            m=0;//Pendiente cero
            error=1;//Error
            if(dx==0){
                //Vertical
                b=Sx/N;
                vertical=true;//Vertical
                x0=(int) Math.round(b);
                x1=x0;
                y0=0;
                y1=30;
            }else{
                b=Sy/N;//coeficiente b
                horizontal=true;
                y0=(int) Math.round(b);
                y1=y0;
                x0=0;
                y0=30;
            }
        } else{
            m=dy/dx;
            b=(Sy-m*Sx)/N;
            error=0;
            for(i=0;i<x.length;i++){
                double dif = y[i]- (m*x[i]+b);
                error+= dif*dif;
            }
            error=Math.sqrt(error)/N;
            x0=x[0];
            y0=(int) Math.round(x0*m+b);
            
            x1=x[N-1];
            y1=(int) Math.round(x1*m+b);
            
        }
    }
    /**
     * Calcula la intersección con la recta dada como argumento
     * @param line
     * @return 
     */
    public Point intersection(LineImage line){
        double x,y;
        x=0;
        y=0;
        if(m == line.m){
            // Las dos rectas pueden ser vertical y horizontal
            // o ser paralelas
            if( vertical && line.isHorizontal()){
                x= b;
                y= line.b;
            }else if ( horizontal && line.isVertical()){
                y= b;
                x= line.b;
            }else
                IJ.error("Error, impossible to find intersection");
        }else if(m ==0){
            if(vertical){
                //Vertical
                //El valor de b indica la posición x de la recta
                x= b;
                y= line.m*x+line.b;
            }else{
                //Horizontal
                //El valor de b indica la posición y de la recta
                y= b;
                x=(y-line.b)/line.m;
            }
            
        } else if(line.m==0){
            if(line.isVertical()){
                //Vertical
                //El valor de b indica la posición x de la recta
                x= line.b;
                y= x*m+b;
            }else{
                //Horizontal
                //El valor de b indica la posición y de la recta
                y=  line.b;
                x= (y-b)/m;
            }
            
        } else{
            // rectas normales
            x=(b-line.b)/(line.m-m);
            y=m*x+b;
        }

        return new Point((int) Math.round( x),(int) Math.round( y));
    }
    boolean isVertical(){
        return vertical;
    }
    boolean isHorizontal(){
        return horizontal;
    }
    double getY(double x){
        return m*x+b;
    }
}