
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Tresh_Clean implements PlugInFilter {

    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL;
    }

    public void run(ImageProcessor ip) {

        int width = ip.getWidth();
        int height = ip.getHeight();

        int size = width * height;
        int i;

        double[] entropy = new double[3];
        double[][] hist = new double[3][256];
        int[] histf = new int[256];
        byte[] r = new byte[size], g = new byte[size], b = new byte[size];
        byte[][] cnn = new byte[3][size];
        int[] rgb;
        int x, y;
        for (y = 0; y < height; y++) {
            for (x = 0; x < width; x++) {
                rgb = ip.getPixel(x, y, null);
                cnn[0][y * width + x] = (byte) rgb[0];//red
                cnn[1][y * width + x] = (byte) rgb[1];//green
                cnn[2][y * width + x] = (byte) rgb[2];//blue
            }
        }

        int j;
        for (j = 0; j < 3; j++) {
            for (i = 0; i < size; hist[j][cnn[j][i++] & 0xff]++);

            for (i = 0; i < 256; i++) {
                hist[j][i] /= size;
            }
            for (i = 0; i < 256; i++) {
                if (hist[j][i] != 0) {
                    entropy[j] -= hist[j][i] * Math.log(hist[j][i]);
                }
            }
            entropy[j] /= 0.30102999566;
        }
        /*IJ.log("Entropy_r=" + entropy[0]);
        IJ.log("Entropy_g=" + entropy[1]);
        IJ.log("Entropy_b=" + entropy[2]);*/

        int pos = 0; //posicion de la mayor entropia
        double mayor = entropy[0];
        for (int k = 1; k <= entropy.length - 1; k++) {
            if (mayor < entropy[k]) {
                mayor = entropy[k];
                pos = k;
            }

        }

        for (i = 0; i < size; histf[cnn[pos][i++] & 0xff]++);
        int jj = pos;
        int max = 0;
        int max1 = 0;
        int auxh = 0;
        for (i = 250; i > 4; i--) {
            if (histf[i] > histf[i + 5] && histf[i] > histf[i - 5]) {
                if (histf[i] > histf[max]) {
                    max = i;
                }
            }
        }
        //IJ.log("max="+max);
        int cont = 0;
        for (j = max - 6; j > 2; j--) {
            if (histf[j] >= histf[j + 3] && histf[j] > histf[j - 3]) {
                if (histf[j] > auxh && histf[j] < histf[max]) {
                    max1 = j;
                    //IJ.log("max1="+max1);
                    auxh = histf[max1];
                }
            }
        }


        // IJ.log("*******************");
        // IJ.log("max1="+max1);
        int min = 256;
        int minaux = height * width;
        for (i = max1; i < max; i++) {
            if (histf[i] < minaux) {
                minaux = histf[i];
                min = i;
            }
        }
        //IJ.log("min="+min);
        if (max - max1 > 55) {
            cont = max - max1;
            cont *= 0.69;
            min = max1 + cont;
        }
        min += 7;
        //IJ.log("min="+min);
        int aux = 0;
        ImagePlus impH = NewImage.createByteImage("BINARIO ", width, height, 1, NewImage.FILL_BLACK);
        ImageProcessor ipH = impH.getProcessor();

        //--------------------------------------------
        byte[] pixelss = (byte[]) ipH.getPixels();
        //---------------------------------------------
        for (y = 0; y < height; y++) {
            for (x = 0; x < width; x++) {
                aux = cnn[jj][y * width + x] & 0xff;
                //IJ.log("AUX="+aux);
                if (aux < min) {
                    pixelss[y * width + x] = (byte) 255;
                } else {
                    pixelss[y * width + x] = (byte) 0;
                }
            }
        }

        //se procede a asignar que la imagen tenga fondo negro para aplicar fill holes
        Prefs.blackBackground = true;
        int foreground = 0, background = 0;
        int aux_fg = Prefs.blackBackground ? 255 : 0;
        foreground = ipH.isInvertedLut() ? 255 - aux_fg : aux_fg;
        background = 255 - foreground;
        //SE APLICA FILL HOLES
        FloodFiller ff = new FloodFiller(ipH);
        ipH.setColor(127);

        for (int y_ = 0; y_ < height; y_++) {
            if (ipH.getPixel(0, y_) == background) {
                ff.fill(0, y_);
            }
            if (ipH.getPixel(width - 1, y_) == background) {
                ff.fill(width - 1, y_);
            }
        }
        for (int x_ = 0; x_ < width; x_++) {
            if (ipH.getPixel(x_, 0) == background) {
                ff.fill(x_, 0);
            }
            if (ipH.getPixel(x_, height - 1) == background) {
                ff.fill(x_, height - 1);
            }
        }
        byte[] pixels = (byte[]) ipH.getPixels();
        int n = width * height;
        for (int i_ = 0; i_ < n; i_++) {
            if (pixels[i_] == 127) {
                pixels[i_] = (byte) background;
            } else {
                pixels[i_] = (byte) foreground;
            }
        }

        //SE PROCEDE CON 7 ITERACIONES A LA IMAGEN
        //CLOSE
        int iterations = 10; //SE COORDINA PARA AUTOMATIZAR EL VALOR DE ITERATIONS
        int count = 1;
        //String[] mode = {"dilate", "erode"};

        for (int jd = 0; jd < 40; jd++) {
            if (jd < iterations) {
                ((ByteProcessor) ipH).erode(count, background);
            }
            else if (jd < iterations*2) {
                ((ByteProcessor) ipH).dilate(count, background);
            }
            else if (jd < iterations*3) {
                ((ByteProcessor) ipH).erode(count, background);
            }
            else{
                ((ByteProcessor) ipH).dilate(count, background);
            }
        }
            impH.show(); 
                
        }
    }


