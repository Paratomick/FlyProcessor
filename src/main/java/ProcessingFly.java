import commands.WindowLevelTool;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.io.FileSaver;
import ij.plugin.RGBStackMerge;
import ij.plugin.ZProjector;
import ij.plugin.filter.RGBStackSplitter;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.*;
import org.imagearchive.lsm.reader.Reader;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProcessingFly {
    File dirOutput;
    XMLSlideShow slideShow;
    XSLFSlideMaster slideMaster;

    int pictureCounter = -1;
    int picturesPerSlideWidth = 6;
    int picturesPerSlideHeight = 4;
    int maxPerSlide = picturesPerSlideWidth * picturesPerSlideHeight;

    XSLFSlide[][] currentSlides;

    String slideName = "Slide";

    public static File[] getFilesInFolder(File folder) {
        if (folder.isDirectory()) {                         // Checking if dir is a folder
            File[] dirFiles = folder.listFiles();           // An array of all filenames in the folder.
            File[] emptyFileArray = new File[1];            // Empty array to soon write the non directory files to.
            ArrayList<File> fileList = new ArrayList<>();   // New empty List.
            if(dirFiles != null) {                          // Looks if there are files or folders in the folder.
                for (File f : dirFiles) {
                    if (!f.isDirectory()) {                     // Filter out all paths, that are directories.
                        fileList.add(f);    // If you want to filter for filetypes, for example .lsm, you could do that here.
                    }
                }
                // Array of files, containing all non directory files in the selected folder.
                return fileList.toArray(emptyFileArray);
            }
        }
        return new File[0];
    }

    public ProcessingFly(File folder, boolean withPowerPoint) {
        if (folder == null) {
            System.err.println("No File was given.");
            return;
        }
        dirOutput = new File(folder.getPath() + "/output");
        System.out.printf("You have selected %s as the folder.\n", folder);

        if (folder.isDirectory()) { // Checking if dir is a folder
            // Array of files, containing all non directory files in the selected folder.
            File[] files = getFilesInFolder(folder);

            if(withPowerPoint) {
                // Look through the files and find one for the slideName.
                for (File f : files) {
                    if (f.exists() && f.isFile()) {
                        slideName = files[0].getName().split("_20")[0];
                        break;
                    }
                }
                initPresentation(files.length / maxPerSlide + 1);
            }

            if (!dirOutput.exists()) { // If the output folder does not exist,
                dirOutput.mkdir();     // create it.
            }
            for (File f : files) {                                         // For all pictures in the folder
                System.out.println(f);
                Reader lsm_reader = new Reader();                          // load them
                ImagePlus img = lsm_reader.open(f.getPath(), true); // into imagePlus
                doStuffToPicture(img, withPowerPoint);                     // and do stuff
            }
        }
        if(withPowerPoint) {
            writePresentation();
        }
    }

    public void doStuffToPicture(ImagePlus img, boolean withPowerPoint) {
        // ----- READ -----
        // Determine if the img is an overview
        // Reads the number in the Info Property between "Voxel_size_X:" and "µm".
        String voxelSize = ((String) img.getProperty("Info")).split("Voxel_size_X: ")[1].split(" µm")[0];
        boolean isOverview = Double.parseDouble(voxelSize) > 0.15d; // if that number is bigger then 0.15, we say it's an overview.

        // Get the stackSize to run the ZProjector with
        // Reads the number in the Info Property between "Z_size:" and the end of the line.
        String stackSize = ((String) img.getProperty("Info")).split("Z_size:")[1].split("\n")[0];
        if (stackSize.equals("1")) { // if the stack size is 1, we discard the picture.
            img.close();
            return;
        }
        // ----- Z PROJECTION -----
        // Run the z-projection.
        ImagePlus imgZ = ZProjector.run(img, "max", 1, Integer.parseInt(stackSize));

        // ----- SPLIT -----
        // Split the open image into c1, c2, c3
        RGBStackSplitter splitter = new RGBStackSplitter();
        splitter.split(imgZ);   // Splits the image
        img.close();            // Closes the original image.

        // Load the open images c1, c2, c3 into ImagePlus Classes
        ImagePlus[] imgs = new ImagePlus[4];
        imgs[0] = WindowManager.getImage("C3-MAX_" + img.getTitle());
        imgs[1] = WindowManager.getImage("C2-MAX_" + img.getTitle());
        imgs[2] = WindowManager.getImage("C1-MAX_" + img.getTitle());
        // Create a list with the index of every non-null image.
        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < 3; i++) if (imgs[i] != null) indexList.add(i);

        // ----- CONVERT TO RGB -----
        // Convert c1, c2, c3 to RGB
        indexList.forEach(i -> IJ.run(imgs[i], "RGB Color", ""));

        // ----- MERGE -----
        // Get the Stacks of c1, c2, c3
        ImageStack[] imgStacks = new ImageStack[3];
        indexList.forEach(i -> imgStacks[i] = imgs[i].getStack());
        // Load the width, height and size of the first available c
        int w = imgStacks[indexList.get(0)].getWidth();
        int h = imgStacks[indexList.get(0)].getHeight();
        int s = imgStacks[indexList.get(0)].getSize();
        // Merge c1, c2, c3 to one img
        ImageStack iss = new RGBStackMerge().mergeStacks(w, h, s, imgStacks[0], imgStacks[1], imgStacks[2], true);
        // Put merge into the array and indexList
        imgs[3] = new ImagePlus("MERGE-MAX_" + img.getTitle(), iss);
        imgs[3].show();
        indexList.add(3);

        // ----- SAVE -----
        // Create filenames for savings and save
        String[] paths = new String[4];
        indexList.forEach(i -> {
            // Create name
            paths[i] = dirOutput.getPath() + "\\" + imgs[i].getTitle().replace(".lsm", ".tif");
            // Save File
            FileSaver saver = new FileSaver(imgs[i]);
            saver.saveAsTiff(paths[i]);
        });

        // Only relevant for the PowerPoint
        if(withPowerPoint) {
            // ----- AUTO ADJUST -----
            WindowLevelTool windowLevelTool = new WindowLevelTool();
            indexList.forEach(i -> {
                windowLevelTool.setupImage(imgs[i], true);
                windowLevelTool.betterAutoItemActionPerformed(imgs[i]);
                windowLevelTool.adjustWindowLevel(imgs[i], 0, 0);
            });

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

                for (int i : indexList) {
                    if (i == 2) continue;
                    // Save File
                    FileSaver saver = new FileSaver(imgs[i]);
                    saver.saveAsTiff(dirOutput.getPath() + "\\temp.tif");
                    // Load File
                    XSLFPictureData pic = slideShow.addPicture(new File(dirOutput.getPath() + "\\temp.tif"), PictureData.PictureType.TIFF);
                    XSLFPictureShape pShape = currentSlides[i][slide].createPicture(pic);
                    pShape.setAnchor(new Rectangle(x * 118 + 10, y * 120 + 10, 113, 113));
                }
            } catch (IOException ioe) {
                System.err.println("Could not find the pictures, that where just saved.");
            }
        }

        // Close all remaining ImageJs
        for (int i : indexList) {
            imgs[i].close();
        }
    }

    public void initPresentation(int slides) {
        slideShow = new XMLSlideShow();
        slideMaster = slideShow.getSlideMasters().get(0);

        XSLFSlideLayout xslfSlideLayout = slideMaster.getLayout(SlideLayout.TITLE);
        XSLFSlide xslfSlide = slideShow.createSlide(xslfSlideLayout);
        XSLFTextShape xslfTextShape = xslfSlide.getPlaceholder(0);
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

    public void writePresentation() {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(dirOutput + "/" + slideName + ".pptx");
            slideShow.write(fileOutputStream);
            slideShow.close();
            fileOutputStream.close();
        } catch (IOException fileNotFoundException) {
            System.err.println("There was an error while writing the presentation.");
            fileNotFoundException.printStackTrace();
        }
    }
}
