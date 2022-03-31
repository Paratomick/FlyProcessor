import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.StringProperty;
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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class GuiFly extends Application {

    private static Alert alertException;
    private static GridPane gridException;
    private static Label labelException;
    private static TextArea textAreaException;

    private static Stage logWindow;
    private static StringProperty logWindowTextProperty;

    private File selectedFolder = null;
    private int numFilesInFolder = 0;

    public static void launch(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        VBox root = new VBox();
        Scene scene = new Scene(root, 800, 700);
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
        CheckBox boxIncludeBlue = new CheckBox();
        Label labelIncludeBlue = new Label("include blue");
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
        Button btnLog = new Button("Open Log");

        // Set them into the grid
        grid1.add(labelFolderPathLabel,1, 0, 1, 1);
        grid1.add(labelFolderPath,     2, 0, 2, 1);
        grid1.add(btnFolder,           1, 1, 1, 1);
        grid1.add(labelFileNumLabel,   1, 2, 1, 1);
        grid1.add(labelFileNum,        2, 2, 1, 1);
        grid2.add(boxPowerPoint,       1, 3, 1, 1);
        grid2.add(labelPowerPoint,     2, 3, 1, 1);
        grid2.add(boxIncludeBlue,      1, 4, 1, 1);
        grid2.add(labelIncludeBlue,    2, 4, 1, 1);
        grid2.add(boxWithLabels,       1, 5, 1, 1);
        grid2.add(labelWithLabels,     2, 5, 1, 1);
        grid2.add(boxAutoAdjust,       1, 6, 1, 1);
        grid2.add(labelAutoAdjust,     2, 6, 1, 1);
        grid2.add(labelAdjustX,        1, 7, 1, 1);
        grid2.add(fieldAdjustX,        1, 8, 1, 1);
        grid2.add(sliderAdjustX,       2, 8, 1, 1);
        grid2.add(labelAdjustY,        1, 9, 1, 1);
        grid2.add(fieldAdjustY,        1, 10, 1, 1);
        grid2.add(sliderAdjustY,       2, 10, 1, 1);
        grid2.add(labelVoxelSize,      1, 11, 1, 1);
        grid2.add(fieldVoxelSize,      1, 12, 1, 1);
        grid2.add(sliderVoxelSize,     2, 12, 1, 1);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a file inside the folder...");

        // Set checkboxes to selected at default
        boxPowerPoint.setSelected(true);
        boxIncludeBlue.setSelected(false);
        boxWithLabels.setSelected(true);
        boxAutoAdjust.setSelected(true);
        // Deactivate PowerPoint related boxes if PowerPoint is not selected.
        boxIncludeBlue.disableProperty().bind(boxPowerPoint.selectedProperty().not());
        boxWithLabels.disableProperty().bind(boxPowerPoint.selectedProperty().not());
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

        // ----- EXCEPTION DIALOG -----
        alertException = new Alert(Alert.AlertType.ERROR);
        alertException.setTitle("Exception Dialog");
        alertException.setHeaderText("There was an exception in the program");
        labelException = new Label("The exception stacktrace was:");

        textAreaException = new TextArea("Exception Text should be here.");
        textAreaException.setEditable(false);
        textAreaException.setWrapText(true);
        textAreaException.setMaxWidth(Double.MAX_VALUE);
        textAreaException.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textAreaException, Priority.ALWAYS);
        GridPane.setHgrow(textAreaException, Priority.ALWAYS);
        gridException = new GridPane();
        gridException.setMaxWidth(Double.MAX_VALUE);
        gridException.add(labelException, 0, 0);
        gridException.add(textAreaException, 0,1);

        // ----- LOG WINDOW -----
        logWindow = new Stage();
        logWindow.setTitle("Log");
        TextArea textAreaLogWindow = new TextArea("");
        textAreaLogWindow.setEditable(false);
        textAreaLogWindow.setWrapText(true);
        textAreaLogWindow.setMaxWidth(Double.MAX_VALUE);
        textAreaLogWindow.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textAreaLogWindow, Priority.ALWAYS);
        GridPane.setHgrow(textAreaLogWindow, Priority.ALWAYS);
        logWindowTextProperty = textAreaLogWindow.textProperty();
        Scene scene1 = new Scene(textAreaLogWindow, 400, 300);
        logWindow.setScene(scene1);

        // ----- ACTION LISTENERS / BUTTON CLICKS -----
        // Create Action Listener
        // for the "select Folder" button
        btnFolder.setOnAction(actionEvent -> {
            consoleLogN("Selecting a folder.");
            File chosenFile = fileChooser.showOpenDialog(stage);
            if(chosenFile != null) {
                consoleLogN(" a file inside the folder has been selected: \n " + chosenFile);
                labelFolderPath.setText(chosenFile.getParentFile().getPath());
                selectedFolder = chosenFile.getParentFile();
                numFilesInFolder = ProcessingFly.getFilesInFolder(selectedFolder).length;
                labelFileNum.setText(Integer.toString(numFilesInFolder));
            }
        });
        // for the compute button
        btnCompute.setOnAction(actionEvent -> {
            consoleLogN("Starting computation..");
            if (numFilesInFolder == 0) {
                consoleLogN(" No Folder or empty folder has been selected.");
            } else {
                consoleLogN(" Starting operations on the folder..");
                if(boxPowerPoint.isSelected()) {
                    consoleLogN(" with PowerPoint.");
                }
                ProcessingFly pf = new ProcessingFly();
                pf.withPowerPoint = boxPowerPoint.isSelected();
                pf.includeBlue = boxIncludeBlue.isSelected();
                pf.withLabel = boxWithLabels.isSelected();
                pf.autoAdjust = boxAutoAdjust.isSelected();
                pf.adjustXValue = sliderAdjustX.getValue();
                pf.adjustYValue = sliderAdjustY.getValue();
                pf.voxelSizeForOverviewPrediction = sliderVoxelSize.getValue();

                Thread thread = new Thread(() -> {
                    try {
                        pf.processFolder(selectedFolder);
                    } catch (IOException e) {
                        GuiFly.openExceptionDialog(e);
                    }
                });
                thread.start();
            }
        });
        // for the Log button
        btnLog.setOnAction(actionEvent -> {
            // Open the log window.
            if(!logWindow.isShowing()) {
                logWindow.show();
            }
        });

        root.getChildren().addAll(labelTitle, separator1, grid1, separator2, grid2, separator3, btnCompute, btnLog);
        stage.show();
    }

    public static void consoleLog(String s) {
        System.out.print(s);
        if(logWindowTextProperty != null) {
            logWindowTextProperty.setValue(logWindowTextProperty.get() + s);
        }
    }

    public static void consoleLogN(String s) {
        System.out.println(s);
        if(logWindowTextProperty != null) {
            logWindowTextProperty.setValue(logWindowTextProperty.get() + s + "\n");
        }
    }

    public static void openExceptionDialog(Exception e) {
        Platform.runLater(() -> {
            alertException.setContentText(e.getMessage());

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            String exceptionText = stringWriter.toString();

            textAreaException.setText(exceptionText);
            alertException.getDialogPane().setExpandableContent(gridException);
            alertException.showAndWait();
        });
    }
}
