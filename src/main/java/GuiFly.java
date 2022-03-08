import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        // Set the column constraints
        ColumnConstraints column1 = new ColumnConstraints(300); // 300px wide
        column1.setHgrow(Priority.ALWAYS);
        column1.setHalignment(HPos.RIGHT);
        ColumnConstraints column2 = new ColumnConstraints(200, 200, 800); // 200-800px wide
        column2.setHgrow(Priority.ALWAYS);
        column2.setHalignment(HPos.LEFT);
        grid.getColumnConstraints().addAll(column1, column2);

        // Create nodes
        Label labelTitle = new Label("Immunofluorescence Picture Processor");
        labelTitle.getStyleClass().add("title");

        Label labelFolderPathLabel = new Label("Folder:");
        Label labelFolderPath = new Label("folder");

        Button btnFolder = new Button("Select folder");

        Label labelFileNumLabel = new Label("Number of Files:");
        Label labelFileNum = new Label("0");

        Button btnWithPowerpoint = new Button("Compute with PowerPoint");
        Button btnWithoutPowerpoint = new Button("Compute without PowerPoint");

        // Set them into the grid
        grid.add(labelFolderPathLabel, 0, 0, 1, 1);
        grid.add(labelFolderPath, 1, 0, 1, 1);
        grid.add(btnFolder, 0, 1, 1, 1);
        grid.add(labelFileNumLabel, 0, 2, 1, 1);
        grid.add(labelFileNum, 1, 2, 1, 1);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a file inside the folder...");

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
        // for the "with Powerpoint" button
        btnWithPowerpoint.setOnAction(actionEvent -> {
            System.out.println(actionEvent);
            if (numFilesInFolder == 0) {
                System.out.println("No Folder or empty folder has been selected yet.");
            } else {
                System.out.println("Starting operations on the folder and create a PowerPoint.");
                Thread thread = new Thread(() -> new ProcessingFly(selectedFolder, true));
                thread.start();
            }
        });
        // for the "without Powerpoint" button
        btnWithoutPowerpoint.setOnAction(actionEvent -> {
            System.out.println(actionEvent);
            if (numFilesInFolder == 0) {
                System.out.println("No Folder or empty folder has been selected yet.");
            } else {
                System.out.println("Starting operations on the folder without creating a PowerPoint.");
                Thread thread = new Thread(() -> new ProcessingFly(selectedFolder, false));
                thread.start();
            }
        });

        root.getChildren().addAll(labelTitle, grid, btnWithoutPowerpoint, btnWithPowerpoint);
        stage.show();
    }
}
