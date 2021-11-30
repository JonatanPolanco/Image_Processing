
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.util.Arrays;

public class Filtro_conectividad8 implements PlugInFilter {

    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_8G;
    }

    public void run(ImageProcessor ip) {
         int height = ip.getHeight();
        int width = ip.getWidth();

        byte[] pixels = (byte[]) (ip.getPixels());
        int[][] pixelsM = new int[height][width];

        int y = 0, i = 0;

        for (i = y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelsM[y][x] = pixels[i++] & 0xff;

            }

        }
        for (y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelsM[y][x] += 50;

            }

        }

        for (i = y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                pixels[i++] = pixelsM[y][x] > 255 ? (byte) 255 : (byte) pixelsM[y][x];

            }

        }
        for (int j = 0; j < height * width; j++) {

            if (pixels[j] == -1) {
                pixels[j] = (byte) 255;
            } else {
                if (pixels[j] == 50) {
                    pixels[j] = (byte) 0;
                }
            }
        }
        

        for (i = y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelsM[y][x] = (byte) pixels[i++];
            }
        }


        for (int j = 0; j < height; j++) {
            for (int k = 0; k < width; k++) {
                if (pixelsM[j][k] == -1) {
                    pixelsM[j][k] = 255;
                }
            }

        }

        //FILTRO 8 CONECTIVIDAD PRIMERA VUELTA
        for (int y_ = 0; y_ < height; y_++) {
            for (int x = 0; x < width; x++) {
                if (pixelsM[y_][x] != 0 && ((y_ - 1) != -1 && ((x - 1) != -1))) {
                    //pixelsM[y_][x] = min(pixelsM[y_ - 1][x], pixelsM[y_][x - 1]) + 1;
                }
            }
        }

        for (int _y_ = 0; _y_ < height; _y_++) {
            for (int x = 0; x < width; x++) {
                if (pixelsM[_y_][x] != 0 && ((_y_ - 1) != -1 && ((x - 1) != -1))) {

                }
            }
        }

        //FILTRO 8 VECINDAD
        //FILTRO VECINDAD PRIMERA VUELTA
        for (int _y_ = 0; _y_ < height; _y_++) {
            for (int x = 0; x < width; x++) {
                if (!(pixelsM[_y_][x] == 0)) {
                    if (((_y_ - 1) != -1) && ((x - 1) != -1) && (x + 1 <= pixelsM[0].length) ) {
                        try{
                            pixelsM[_y_][x] = calcularMinimo8vecindad(pixelsM[_y_ - 1][x - 1], pixelsM[_y_ - 1][x],
                                pixelsM[_y_ - 1][x + 1], pixelsM[_y_][x - 1]) + 1;
                            
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                        

                    }

                }
            }
        }
        for (i = y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                pixels[i++] = (byte) pixelsM[y][x];

            }

        }

        //8 VECINDAD SEGUNDA VUELTA
        int fila = pixelsM.length - 1;
        int columna = pixelsM[0].length - 1;
        for (int _y_ = fila; _y_ >= 0; --_y_) {
            for (int x = columna; x >= 0; --x) {
                if (!(pixelsM[_y_][x] == 0)) {
                    if ((_y_ + 1 <= fila && x + 1 <= columna) && ((x - 1) != -1)) {
                        int minimo = calcularMinimo8vecindad(pixelsM[_y_][x + 1], pixelsM[_y_ + 1][x - 1],
                                pixelsM[_y_ + 1][x], pixelsM[_y_ + 1][x + 1]);

                        if (minimo + 1 < pixelsM[_y_][x]) {
                            pixelsM[_y_][x] = minimo + 1;
                        }

                    }
                }

            }
        }

        for (i = y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                pixels[i++] = (byte) pixelsM[y][x];

            }

        }
    }
    private int calcularMinimo8vecindad(int x, int y, int z, int w) {
        /*
        int[] aux = new int[4];
        aux[0] = x;
        aux[1] = y;
        aux[2] = z;
        aux[3] = w;
        int menor = aux[0];//tomamos como primer valor de la matriz para
        //comparar con los otros elementos de ella.
        for (int i = 1; i <= aux.length - 1; i++) {
            if (menor > aux[i] || aux[i] == 0) {
                menor = aux[i];
            }
        }
        return menor;
        */
        
        int[] aux = new int[4];
        aux[0] = x;
        aux[1] = y;
        aux[2] = z;
        aux[3] = w;

        Arrays.sort(aux);
        
        
        
        return aux[0]; 
    }

}
