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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

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
        System.out.printf("Du hast %s ausgewählt. %s ist der Ordner.\n", fileChooser.getSelectedFile().getPath(), dir);

        if (dir.isDirectory()) {
            System.out.print("Es ist auch ein Ordner.\n");
            if (!dirOutput.exists()) {
                dirOutput.mkdir();
                System.out.print("Der Output Ordner wird erstellt.\n");
            }
            File[] dirFiles = dir.listFiles();
            File[] emptyFileArray = new File[1];
            ArrayList<File> fileList = new ArrayList<>();
            for (File f : dirFiles) {
                if (!f.isDirectory()) {
                    fileList.add(f);
                }
            }
            File[] files = fileList.toArray(emptyFileArray);


            if (files != null) {
                System.out.print("Die Liste files ist nicht gleich null. ");
                if (files[0] != null) {
                    System.out.print("files[0] ist auch nicht gleich null ");
                    if (files[0].exists()) {
                        System.out.print("und existiert auch,");
                        if (files[0].isFile()) {
                            System.out.print("es ist sogar ein File. \n");
                            slideName = files[0].getName().split("_20")[0];
                            System.out.printf("The new SlideName will be %s. There are %d file in the folder.\n", slideName, files.length);
                        } else {
                            System.out.print("es ist allerdings kein File. Suche nach File.\n");
                            for (File f : files) {
                                if (f.exists() && f.isFile()) {
                                    slideName = files[0].getName().split("_20")[0];
                                    System.out.printf("Ein passendes File gefunden. \nThe new SlideName will be %s. There are %d file in the folder.\n", slideName, files.length);
                                    break;
                                }
                            }
                        }
                    } else {
                        System.out.print("aber existiert nicht. \n");
                    }
                } else {
                    System.out.print("files[0] ist allerdings gleich null. \n");
                }
            } else {
                System.out.print("Die Liste files ist gleich null. \n");
            }

            initPresentation(files.length / maxPerSlide + 1);

            for (File f : files) {
                if (f.exists() && f.isFile()) {
                    LSM_Reader lsm_reader = new LSM_Reader();
                    ImagePlus img = lsm_reader.open(f.getPath(), true);
                    doStuffToPicture(img);
                }
            }
        } else {
            System.out.print("Es ist aber gar kein Ordner.\n");
        }
        try {
            writePresentation();
        } catch (Exception ignore) {
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

        // ----- MERGE -----
        //ImagePlus is = RGBStackMerge.mergeChannels(new ImagePlus[] {imgR, imgG, imgB}, true);
        ImageStack iss = RGBStackMerge.mergeStacks(imgs[0].getStack(), imgs[1].getStack(), imgs[2].getStack(), true);
        imgs[3] = new ImagePlus("MERGE-MAX_" + img.getTitle(), iss);
        imgs[3].show();

        String[] paths = new String[]{
                dirOutput.getPath() + "\\" + imgs[0].getTitle().replace(".lsm", ".tif"),
                dirOutput.getPath() + "\\" + imgs[1].getTitle().replace(".lsm", ".tif"),
                dirOutput.getPath() + "\\" + imgs[2].getTitle().replace(".lsm", ".tif"),
                dirOutput.getPath() + "\\" + imgs[3].getTitle().replace(".lsm", ".tif")
        };

        // ----- SAVE -----
        FileSaver saver;
        for (int i = 0; i < 4; i++) {
            // Save File
            saver = new FileSaver(imgs[i]);
            saver.saveAsTiff(paths[i]);
        }

        // ----- AUTO ADJUST -----
        MyWindowLevelTool windowLevelTool = new MyWindowLevelTool();
        for (int i = 0; i < 4; i++) {
            IJ.run(imgs[i], "RGB Color", "");
            windowLevelTool.setupImage(imgs[i], true);
            windowLevelTool.betterAutoItemActionPerformed(imgs[i]);
            windowLevelTool.adjustWindowLevel(imgs[i], 0, 0);
        }

        // ----- PP -----
        try {
            if (!isOverview) {
                pictureCounter++;
            } else {
                pictureCounter = pictureCounter + (picturesPerSlideWidth - ((pictureCounter + picturesPerSlideWidth) % picturesPerSlideWidth));
            }
            int slide = pictureCounter / maxPerSlide;
            int y = (pictureCounter % maxPerSlide) / picturesPerSlideWidth;
            int x = (pictureCounter % maxPerSlide) % picturesPerSlideWidth;

            for (int i = 0; i < 4; i++) {
                if(i == 2) continue;
                // Save File
                saver = new FileSaver(imgs[i]);
                saver.saveAsTiff(dirOutput.getPath() + "\\temp.tif");
                // Load File
                XSLFPictureData pic = slideShow.addPicture(new File(dirOutput.getPath() + "\\temp.tif"), PictureData.PictureType.TIFF);
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
        //xslfSlide.getPlaceholder(1).clearText();
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
