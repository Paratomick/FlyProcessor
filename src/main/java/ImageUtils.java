import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImageUtils {

    // convert BufferedImage to byte[]
    public static byte[] toByteArray(BufferedImage bi, String format)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, format, baos);
        return baos.toByteArray();
    }

    // convert byte[] to BufferedImage
    public static BufferedImage toBufferedImage(byte[] bytes)
            throws IOException {

        InputStream is = new ByteArrayInputStream(bytes);
        return ImageIO.read(is);
    }
}
