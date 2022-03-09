import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class GuiFly extends Application {

    private File selectedFolder = null;
    private int numFilesInFolder = 0;

    public static void launch(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        VBox root = new VBox();
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add("style.css");
        stage.setScene(scene);
        stage.setTitle("Immunofluorescence Picture Processor");

        // Create the grid
        GridPane grid1 = new GridPane();
        grid1.setHgap(10);
        grid1.setVgap(5);
        GridPane grid2 = new GridPane();
        grid2.setHgap(10);
        grid2.setVgap(5);

        // Set the column constraints
        ColumnConstraints column1 = new ColumnConstraints(0, 0, 800); // 0-800px wide, less grow priority
        column1.setHgrow(Priority.SOMETIMES);
        ColumnConstraints column2 = new ColumnConstraints(150); // 300px wide
        column2.setHgrow(Priority.ALWAYS);
        column2.setHalignment(HPos.RIGHT);
        ColumnConstraints column3 = new ColumnConstraints(200, 200, 600); // 200-800px wide
        column3.setHgrow(Priority.ALWAYS);
        column3.setHalignment(HPos.LEFT);
        ColumnConstraints column4 = new ColumnConstraints(0, 0, 800); // 0-800px wide, less grow priority
        column4.setHgrow(Priority.SOMETIMES);
        grid1.getColumnConstraints().addAll(column1, column2, column3, column4);
        grid2.getColumnConstraints().addAll(column1, column2, column3, column4);

        // Create nodes
        Label labelTitle = new Label("Immunofluorescence Picture Processor");
        labelTitle.getStyleClass().add("title");

        Label labelFolderPathLabel = new Label("Folder:");
        Label labelFolderPath = new Label("folder");

        Button btnFolder = new Button("Select folder");

        Label labelFileNumLabel = new Label("Number of Files:");
        Label labelFileNum = new Label("0");

        CheckBox boxPowerPoint = new CheckBox();
        Label labelPowerPoint = new Label("with PowerPoint");
        CheckBox boxWithLabels = new CheckBox();
        Label labelWithLabels = new Label("with Labels");
        CheckBox boxAutoAdjust = new CheckBox();
        Label labelAutoAdjust = new Label("autoAdjust");
        Label labelAdjustX = new Label("Adjust X:");
        TextField fieldAdjustX = new TextField();
        Slider sliderAdjustX = new Slider();
        Label labelAdjustY = new Label("Adjust Y:");
        TextField fieldAdjustY = new TextField();
        Slider sliderAdjustY = new Slider();
        Label labelVoxelSize = new Label("Voxel Size in µm");
        TextField fieldVoxelSize = new TextField();
        Slider sliderVoxelSize = new Slider();

        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        Separator separator3 = new Separator();

        Button btnCompute = new Button("Compute the folder");
        btnCompute.getStyleClass().add("btnCompute");

        // Set them into the grid
        grid1.add(labelFolderPathLabel,1, 0, 1, 1);
        grid1.add(labelFolderPath,     2, 0, 2, 1);
        grid1.add(btnFolder,           1, 1, 1, 1);
        grid1.add(labelFileNumLabel,   1, 2, 1, 1);
        grid1.add(labelFileNum,        2, 2, 1, 1);
        grid2.add(boxPowerPoint,       1, 3, 1, 1);
        grid2.add(labelPowerPoint,     2, 3, 1, 1);
        grid2.add(boxWithLabels,       1, 4, 1, 1);
        grid2.add(labelWithLabels,     2, 4, 1, 1);
        grid2.add(boxAutoAdjust,       1, 5, 1, 1);
        grid2.add(labelAutoAdjust,     2, 5, 1, 1);
        grid2.add(labelAdjustX,        1, 6, 1, 1);
        grid2.add(fieldAdjustX,        1, 7, 1, 1);
        grid2.add(sliderAdjustX,       2, 7, 1, 1);
        grid2.add(labelAdjustY,        1, 8, 1, 1);
        grid2.add(fieldAdjustY,        1, 9, 1, 1);
        grid2.add(sliderAdjustY,       2, 9, 1, 1);
        grid2.add(labelVoxelSize,      1, 10, 1, 1);
        grid2.add(fieldVoxelSize,      1, 11, 1, 1);
        grid2.add(sliderVoxelSize,     2, 11, 1, 1);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a file inside the folder...");

        // Set checkboxes to selected at default
        boxPowerPoint.setSelected(true);
        boxWithLabels.setSelected(true);
        boxAutoAdjust.setSelected(true);
        // Deactivate Sliders, if autoAdjust is enabled
        labelAdjustX.disableProperty().bind(boxAutoAdjust.selectedProperty());
        labelAdjustY.disableProperty().bind(boxAutoAdjust.selectedProperty());
        fieldAdjustX.disableProperty().bind(boxAutoAdjust.selectedProperty());
        fieldAdjustY.disableProperty().bind(boxAutoAdjust.selectedProperty());
        sliderAdjustX.disableProperty().bind(boxAutoAdjust.selectedProperty());
        sliderAdjustY.disableProperty().bind(boxAutoAdjust.selectedProperty());
        // Set the voxelSize to 0.15 at default, and bind their slider and textfield values
        sliderAdjustX.setMin(-255);
        sliderAdjustX.setMax(255);
        sliderAdjustX.setMajorTickUnit(10);
        sliderAdjustX.setMinorTickCount(10);
        sliderAdjustX.valueProperty().addListener((observableValue, before, after) -> {
            fieldAdjustX.setText(Double.toString((double)after));
        });
        fieldAdjustX.textProperty().addListener((observableValue, before, after) -> {
            double value = sliderAdjustX.getValue();
            try {
                value = Double.parseDouble(after);
            } catch (NumberFormatException ignored) {}
            sliderAdjustX.setValue(value);
            fieldAdjustX.setText(Double.toString(value));
        });
        sliderAdjustY.setMin(-255);
        sliderAdjustY.setMax(255);
        sliderAdjustY.setMajorTickUnit(10);
        sliderAdjustY.setMinorTickCount(10);
        sliderAdjustY.valueProperty().addListener((observableValue, before, after) -> {
            fieldAdjustY.setText(Double.toString((double)after));
        });
        fieldAdjustY.textProperty().addListener((observableValue, before, after) -> {
            double value = sliderAdjustY.getValue();
            try {
                value = Double.parseDouble(after);
            } catch (NumberFormatException ignored) {}
            sliderAdjustY.setValue(value);
            fieldAdjustY.setText(Double.toString(value));
        });
        sliderVoxelSize.setMin(0d);
        sliderVoxelSize.setMax(1d);
        sliderVoxelSize.setMajorTickUnit(0.1d);
        sliderVoxelSize.setMinorTickCount(10);
        sliderVoxelSize.valueProperty().addListener((observableValue, before, after) -> {
            fieldVoxelSize.setText(Double.toString((double)after));
        });
        fieldVoxelSize.textProperty().addListener((observableValue, before, after) -> {
            double value = sliderVoxelSize.getValue();
            try {
                value = Double.parseDouble(after);
            } catch (NumberFormatException ignored) {}
            sliderVoxelSize.setValue(value);
            fieldVoxelSize.setText(Double.toString(value));
        });

        sliderAdjustX.setValue(0);
        fieldAdjustX.setText(Double.toString(0));
        sliderAdjustY.setValue(0);
        fieldAdjustY.setText(Double.toString(0));
        sliderVoxelSize.setValue(0.15);
        fieldVoxelSize.setText(Double.toString(0.15));

        // ----- ACTION LISTENERS / BUTTON CLICKS -----
        // Create Action Listener
        // for the "select Folder" button
        btnFolder.setOnAction(actionEvent -> {
            System.out.println(actionEvent);
            File chosenFile = fileChooser.showOpenDialog(stage);
            if(chosenFile != null) {
                System.out.println(chosenFile);
                labelFolderPath.setText(chosenFile.getParentFile().getPath());
                selectedFolder = chosenFile.getParentFile();
                numFilesInFolder = ProcessingFly.getFilesInFolder(selectedFolder).length;
                labelFileNum.setText(Integer.toString(numFilesInFolder));
            }
        });
        // for the compute button
        btnCompute.setOnAction(actionEvent -> {
            System.out.println(actionEvent);
            if (numFilesInFolder == 0) {
                System.out.println("No Folder or empty folder has been selected yet.");
            } else {
                System.out.println("Starting operations on the folder and create a PowerPoint.");
                ProcessingFly pf = new ProcessingFly();
                pf.withPowerPoint = boxPowerPoint.isSelected();
                pf.withLabel = boxWithLabels.isSelected();
                pf.autoAdjust = boxAutoAdjust.isSelected();
                pf.adjustXValue = sliderAdjustX.getValue();
                pf.adjustYValue = sliderAdjustY.getValue();
                pf.voxelSizeForOverviewPrediction = sliderVoxelSize.getValue();
                Thread thread = new Thread(() -> pf.processFolder(selectedFolder));
                thread.start();
            }
        });

        root.getChildren().addAll(labelTitle, separator1, grid1, separator2, grid2, separator3, btnCompute);
        stage.show();
    }
}
