module de.macniel.srdcreature {
    requires javafx.controls;
    requires javafx.fxml;
    requires CampaignWriter.SDK;
    requires com.google.gson;

    requires org.kordamp.ikonli.javafx;

    opens de.macniel.srdcreature to javafx.fxml;
    exports de.macniel.srdcreature;
}