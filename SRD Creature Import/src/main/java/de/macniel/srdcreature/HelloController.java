package de.macniel.srdcreature;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HelloController {
    @FXML
    private Label welcomeText;
    private Stage stage;

    @FXML
    protected void onHelloButtonClick() {

        CreatureImporter importer = new CreatureImporter();
        
        importer.setOnGenerateNote(param -> {
            System.out.println(param);
            return true;
        });
        
        importer.startTask(null, stage, null);

    }
    
    public void setStage(Stage s) {
        this.stage = s;
    }
}