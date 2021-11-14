import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.FileSaver;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.RGBStackSplitter;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {
    File dir;
    File dirOutput;
    JFileChooser fileChooser;
    XMLSlideShow slideShow;
    XSLFSlideMaster slideMaster;

    int pictureCounter = -1;
    int picturesPerSlideWidth = 6;
    int picturesPerSlideHeight = 4;
    int maxPerSlide = picturesPerSlideWidth * picturesPerSlideHeight;

    XSLFSlide[][] currentSlides;

    String slideName = "Slide";

    public static void main(String[] args) throws IOException {
        new Main();
    }

    public Main() throws IOException {
        JFrame frame = new JFrame();
        fileChooser = new JFileChooser(new File(".").getCanonicalPath());
        fileChooser.setDialogTitle("Choose any file in the folder");
        fileChooser.showOpenDialog(frame);
        if (fileChooser.getSelectedFile() == null) return;
        dir = fileChooser.getSelectedFile().getParentFile();
        dirOutput = new File(dir.getPath() + "/output");

        if (dir.isDirectory()) {
            if (!dirOutput.exists()) {
                dirOutput.mkdir();
            }
            File[] files = dir.listFiles();
            if(files != null && files[0] != null && files[0].exists() && files[0].isFile()) {
                slideName = files[0].getName().split("_20")[0];
            }

            initPresentation(files.length / maxPerSlide + 1);

            for (File f : files) {
                if (f.exists() && f.isFile()) {
                    LSM_Reader lsm_reader = new LSM_Reader();
                    ImagePlus img = lsm_reader.open(f.getPath(), true);
                    doStuffToPicture(img);
                }
            }
        }
        try {
            writePresentation();
        } catch (Exception e) {
        }
        fileChooser.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        System.exit(0);
    }

    public void doStuffToPicture(ImagePlus img) {

        MyZProjector zProjector = new MyZProjector(img);

        String voxelSize = ((String) img.getProperty("Info")).split("Voxel_size_X: ")[1].split(" µm")[0];
        boolean isOverview = Double.parseDouble(voxelSize) > 0.15d;

        String info = ((String) img.getProperty("Info")).split("Z_size:")[1].split("\n")[0];
        if (info.equals("1")) {
            img.close();
            return;
        }
        zProjector.run(1, Integer.parseInt(info), MyZProjector.MAX_METHOD);
        ImagePlus imgZ = zProjector.getProjection();

        RGBStackSplitter splitter = new RGBStackSplitter();
        splitter.split(imgZ);
        img.close();

        ImagePlus[] imgs = new ImagePlus[4];

        imgs[0] = WindowManager.getImage("C3-MAX_" + img.getTitle());
        imgs[1] = WindowManager.getImage("C2-MAX_" + img.getTitle());
        imgs[2] = WindowManager.getImage("C1-MAX_" + img.getTitle());

        //ImagePlus is = RGBStackMerge.mergeChannels(new ImagePlus[] {imgR, imgG, imgB}, true);
        ImageStack iss = RGBStackMerge.mergeStacks(imgs[0].getStack(), imgs[1].getStack(), imgs[2].getStack(), true);
        imgs[3] = new ImagePlus("MERGE-MAX_" + img.getTitle(), iss);
        imgs[3].show();

        MyWindowLevelTool windowLevelTool = new MyWindowLevelTool();
        for (int i = 0; i < 4; i++) {
            IJ.run(imgs[i], "RGB Color", "");
            windowLevelTool.setupImage(imgs[i], true);
            windowLevelTool.betterAutoItemActionPerformed(imgs[i]);
            windowLevelTool.adjustWindowLevel(imgs[i], 0, 0);
        }


        System.out.println();

        String[] paths = new String[]{
                dirOutput.getPath() + "\\" + imgs[0].getTitle().replace(".lsm", ".tif"),
                dirOutput.getPath() + "\\" + imgs[1].getTitle().replace(".lsm", ".tif"),
                dirOutput.getPath() + "\\" + imgs[2].getTitle().replace(".lsm", ".tif"),
                dirOutput.getPath() + "\\" + imgs[3].getTitle().replace(".lsm", ".tif")
        };

        try {
            if (!isOverview) {
                pictureCounter++;
            } else {
                pictureCounter = pictureCounter + (picturesPerSlideWidth - ((pictureCounter + picturesPerSlideWidth) % picturesPerSlideWidth));
            }
            int slide = pictureCounter / maxPerSlide;
            int y = (pictureCounter % maxPerSlide) / picturesPerSlideWidth;
            int x = (pictureCounter % maxPerSlide) % picturesPerSlideWidth;

            FileSaver saver;
            for (int i = 0; i < 4; i++) {
                // Save File
                if (i == 2) continue;
                saver = new FileSaver(imgs[i]);
                saver.saveAsTiff(paths[i]);
                // Load File
                XSLFPictureData pic = slideShow.addPicture(new File(paths[i]), PictureData.PictureType.TIFF);
                XSLFPictureShape pShape = currentSlides[i][slide].createPicture(pic);
                pShape.setAnchor(new Rectangle(x * 118 + 10, y * 120 + 10, 113, 113));
            }
        } catch (IOException ioe) {
            System.err.println("Could not fine the pictures, that where just saved.");
        }

        imgs[0].close();
        imgs[1].close();
        imgs[2].close();
        imgs[3].close();
    }

    public void initPresentation(int slides) {
        slideShow = new XMLSlideShow();
        slideMaster = slideShow.getSlideMasters().get(0);

        XSLFSlideLayout xslfSlideLayout = slideMaster.getLayout(SlideLayout.TITLE);
        XSLFSlide xslfSlide = slideShow.createSlide(xslfSlideLayout);
        XSLFTextShape xslfTextShape = xslfSlide.getPlaceholder(0);
        xslfSlide.getPlaceholder(1).clearText();
        xslfTextShape.setText(slideName);

        currentSlides = new XSLFSlide[4][];
        for (int i = 0; i < 4; i++) {
            currentSlides[i] = new XSLFSlide[slides];
            if (i == 2) continue;
            for (int j = 0; j < slides; j++) {
                currentSlides[i][j] = slideShow.createSlide(slideMaster.getLayout(SlideLayout.BLANK));
            }
        }
    }

    public void writePresentation() throws Exception {
        FileOutputStream fileOutputStream = new FileOutputStream(new File(dirOutput + "/" + slideName + ".pptx"));
        slideShow.write(fileOutputStream);
        slideShow.close();
        fileOutputStream.close();
    }
}
