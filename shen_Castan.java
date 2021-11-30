import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import static ij.plugin.filter.ExtendedPlugInFilter.KEEP_PREVIEW;

public class shen_Castan implements PlugInFilter {

    ImagePlus imp;
    double alfa;
    double[] tmpresX;

    double[] tmpresY;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_ALL-DOES_8C-DOES_RGB|SUPPORTS_MASKING|KEEP_PREVIEW|PARALLELIZE_STACKS;
    }

    public double[] getX() {
        return tmpresX;
    }
    public double[] getY() {
        return tmpresY;
    }

    public void run(ImageProcessor ip) {
      
        alfa = 1;

        int width = ip.getWidth();
        int height = ip.getHeight();

        final int size = width * height;
        final double[] pixels = new double[size];
        final int type = imp.getType();
        switch (type) {
            case ImagePlus.GRAY8:
                final byte[] bsrc = (byte[]) ip.getPixels();
                for (int i = 0; i < size; i++) {
                    pixels[i] = (double) (bsrc[i] & 0xFF);
                }
                break;
            case ImagePlus.GRAY16:
                final short[] ssrc = (short[]) ip.getPixels();
                for (int i = 0; i < size; i++) {
                    pixels[i] = (double) (ssrc[i] & 0xFFFF);
                }
                break;
            case ImagePlus.GRAY32:
                final float[] fsrc = (float[]) ip.getPixels();
                for (int i = 0; i < size; i++) {
                    pixels[i] = (double) fsrc[i];
                }
                break;
            default:
                IJ.error("Not a valid image");
                return;
        }

        double[] row1 = new double[width];
        double[] row2 = new double[width];
        tmpresX = new double[size];
        tmpresY = new double[size];
        final double[] tmpres = new double[size];
        final double a = ((1 - Math.exp(-alfa))) / (1 + Math.exp(-alfa));
     
        double max = Double.MIN_VALUE; 
        int offset;

        // Shen X
        for (int y = height; y-- > 0;) { 

            offset = y * width;

            row1[0] = a * pixels[offset];
            for (int x = width; x-- > 1;) {
                row1[width - x] = a * ((pixels[offset + width - x]) - row1[width - x - 1]) + row1[width - x - 1];
            }

            row2[width - 1] = a * row1[width - 1];
            for (int x = width; x-- > 1;) {
                row2[x - 1] = a * ((pixels[offset + x - 1]) - row2[x]) + row2[x];
            }

            for (int x = width; x-- > 0;) {
                tmpresX[offset + x] = alfa * (row2[x] - row1[x]);
             
            }
        }

        // Shen Y
        row1 = new double[height];
        row2 = new double[height];
        for (int x = width; x-- > 0;) { 

            row1[0] = a * (pixels[x] - a * pixels[x]) + a * pixels[x];
            for (int y = height; y-- > 1;) {
                row1[height - y] = a * ((pixels[width * (height - y) + x]) - row1[height - y - 1]) + row1[height - y - 1];
            }

            row2[height - 1] = a * row1[height - 1];
            for (int y = height; y-- > 1;) {
                row2[y - 1] = a * ((pixels[width * (y - 1) + x]) - row2[y]) + row2[y];
            }

            for (int y = height; y-- > 0;) {
                tmpresY[width * y + x] = alfa * (row2[y] - row1[y]);
               
            }
        }

        // double den = max-min;
        for (int y = height; y-- > 0;) {
            offset = y * width;
            for (int x = width; x-- > 0;) {
                if ((tmpres[offset + x] = Math.sqrt(tmpresX[offset + x] * tmpresX[offset + x] + tmpresY[offset + x] * tmpresY[offset + x])) > max) {
                    max = tmpres[offset + x];
                }
            }
        }

      
        int rx, ry, rwidth, rheight, index;
        final Rectangle r = ip.getRoi();
        if (r != null) {
            rx = r.x;
            ry = r.y;
            rwidth = r.width;
            rheight = r.height;
        } else {
            rx = 0;
            ry = 0;
            rwidth = width;
            rheight = height;
        }

        final double pMax = ip.getMax();
        switch (type) {
            case ImagePlus.GRAY8:
                final byte[] bres = (byte[]) ip.getPixels();
                for (int y = ry; y < (ry + rheight); y++) {
                    for (int x = rx; x < (rx + rwidth); x++) {
                        index = y * width + x;
                        bres[index] = (byte) ((pMax * tmpres[index]) / max);
                    }
                }
                ip.setPixels(bres);
                break;
            case ImagePlus.GRAY16:
                final short[] sres = (short[]) ip.getPixels();
                for (int y = ry; y < (ry + rheight); y++) {
                    for (int x = rx; x < (rx + rwidth); x++) {
                        index = y * width + x;
                        sres[index] = (short) ((pMax * tmpres[index]) / max);
                    }
                }
                ip.setPixels(sres);
                break;
            case ImagePlus.GRAY32:
                final float[] fres = (float[]) ip.getPixels();
                for (int y = ry; y < (ry + rheight); y++) {
                    for (int x = rx; x < (rx + rwidth); x++) {
                        index = y * width + x;
                        fres[index] = (float) (tmpres[index]);
                    }
                }
                ip.setPixels(fres);
                break;
        }

    }

}