package de.macniel.srdcreature;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import de.macniel.campaignwriter.SDK.*;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class CreatureImporter implements DataPlugin {

    class ActorNoteItem {

        public enum ActorNoteItemType {
            HEADER("Überschrift"),
            TEXT("Text"),
            RESOURCE("Resource"),
            IMAGE("Bild"),

            STRING("Zeichen");

            public final String label;
            ActorNoteItemType(String label) {
                this.label = label;
            }
            ActorNoteItemType valueOfLabel(String label) {
                for (ActorNoteItemType e: values()) {
                    if (e.label.equals(label)) {
                        return e;
                    }
                }
                return null;
            }
        }


        ActorNoteItem.ActorNoteItemType type;

        String label;

        String content;

        Integer max;

        Integer value;

        public void setLabel(String label) {
            this.label = label;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public void setMax(Integer max) {
            this.max = max;
        }

        public void setType(ActorNoteItem.ActorNoteItemType type) {
            this.type = type;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public ActorNoteItem.ActorNoteItemType getType() {
            return type;
        }

        public Integer getMax() {
            return max;
        }

        public Integer getValue() {
            return value;
        }

        public String getContent() {
            return content;
        }
    }

    class Actor {

        List<ActorNoteItem> items;

        public List<ActorNoteItem> getItems() {
            return items;
        }

        public void setItems(List<ActorNoteItem> merged) {
            this.items = merged;
        }

        public Actor() {
            items = new ArrayList<>();
        }

    }

    class ActorNote extends Note<Actor> {

        private Actor content;

        private Date createdDate;
        private Date lastModifiedDate;

        @Override
        public Date getCreatedDate() {
            return createdDate;
        }

        @Override
        public Date getLastModifiedDate() {
            return lastModifiedDate;
        }

        @Override
        public String getContent() {
            return new Gson().toJson(content);
        }

        @Override
        public Actor getContentAsObject() {
            return content;
        }

        public void setContent(String content) {
            this.content = new Gson().fromJson(content, Actor.class);
        }

        @Override
        public String getType() {
            return "actor";
        }

        public ActorNote() {
            super();

            this.content = new Actor();
            this.setLabel("Neuer Akteur");
            this.setReference(UUID.randomUUID());
            this.createdDate = new Date();
            this.lastModifiedDate = new Date();
            this.content.items = new ArrayList<>();
        }

    }




    private CampaignFileInterface file;
    private Callback<CampaignFileInterface, Boolean> callback;

    private ArrayList<ActorNote> creatures;

    private HashMap<String, String> fieldMappings;

    private VBox actorNoteItems;
    private Callback<Note, Boolean> generateNote;

    @Override
    public String menuItemLabel() {
        return "SRD Creature Import";
    }

    @Override
    public void startTask(CampaignFileInterface campaignFileInterface, Stage stage, FileAccessLayerInterface fileAccessLayerInterface) {

        Stage wnd = new Stage();

        BorderPane bp = new BorderPane();

        ListView<ActorNote> items = new ListView<>();

        fieldMappings = new HashMap<>();

        ScrollPane pane = new ScrollPane();

        actorNoteItems = new VBox();

        pane.setContent(actorNoteItems);
        bp.setCenter(pane);

        HBox controls = new HBox();

        HBox strecher = new HBox();
        HBox.setHgrow(strecher, Priority.ALWAYS);
        Button importCreature = new Button("import");

        controls.getChildren().addAll(strecher, importCreature);

        bp.setBottom(controls);

        items.setCellFactory(listView -> new ListCell<ActorNote>() {
            @Override
            protected void updateItem(ActorNote item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {

                    HBox box = new HBox();
                    item.getContentAsObject().getItems().stream().filter(e -> e.getLabel().startsWith("name")).findFirst().ifPresent(name -> {
                            box.getChildren().add(new Label(name.getContent()));
                    });

                    this.setGraphic(box);
                }
            }
        });

        this.file = campaignFileInterface;

        try {
            URL src = new URL("https://gist.githubusercontent.com/tkfu/9819e4ac6d529e225e9fc58b358c3479/raw/d4df8804c25a662efc42936db60cfbc0a5b19db8/srd_5e_monsters.json");

            JsonElement node = JsonParser.parseReader(new InputStreamReader(src.openStream()));

            JsonArray list = node.getAsJsonArray();
            creatures = new ArrayList<>();
            list.forEach( entry -> {
                JsonObject actualEntry = (JsonObject) entry;

                ActorNote actor = new ActorNote();

                Actor tmp = new Actor();

                actualEntry.keySet().forEach(label -> {
                    ActorNoteItem item = new ActorNoteItem();

                    item.setLabel(label);
                    item.setContent(actualEntry.get(label).getAsString());
                    if (item.getContent().startsWith("<")) { // is html therefor TEXT
                        item.setType(ActorNoteItem.ActorNoteItemType.TEXT);
                    } else if (item.getContent().startsWith("http")) { // is url therefor IMAGE
                        item.setType(ActorNoteItem.ActorNoteItemType.IMAGE);
                    } else {
                        item.setType(ActorNoteItem.ActorNoteItemType.STRING);
                    }

                    actor.getContentAsObject().getItems().add(item);
                });
                actor.setLabel(actualEntry.get("name").getAsString());

                creatures.add(actor);
            });
            items.setItems(FXCollections.observableArrayList(creatures));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        items.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && oldValue != newValue) {
                render((ActorNote) newValue);
            }
        });

        importCreature.onActionProperty().set(e -> {

            ActorNote toImport = items.getSelectionModel().getSelectedItem();
            List<ActorNoteItem> mappedItems = toImport.getContentAsObject().getItems().stream().map( item -> {
                ActorNoteItem mapped = new ActorNoteItem();
                if (fieldMappings.get((item.getLabel())) != null && !fieldMappings.get((item.getLabel())).trim().isEmpty()) {
                    mapped.setLabel(fieldMappings.get(item.getLabel()));

                } else {
                    mapped.setLabel(item.getLabel());
                }
                mapped.setType(item.getType());
                mapped.setContent(item.getContent());
                mapped.setMax(item.getMax());
                mapped.setValue(item.getValue());
                return mapped;
            }).toList();
                    toImport.getContentAsObject().setItems(mappedItems);

            if (generateNote != null) {
                generateNote.call(toImport);
            }
        });

        bp.setLeft(items);

        wnd.setScene(new Scene(bp, 200, 200));
        wnd.setTitle("SRD Creature Import");
        wnd.initOwner(stage);
        wnd.initModality(Modality.APPLICATION_MODAL);

        wnd.show();
    }

    @Override
    public void setOnGenerateNote(Callback<Note, Boolean> callback) {
        this.generateNote = callback;
    }

    @Override
    public void setOnFinishedTask(Callback<CampaignFileInterface, Boolean> callback) {
        this.callback = callback;
    }

    public void render(ActorNote actor) {
        actorNoteItems.getChildren().clear();

        actor.getContentAsObject().getItems().forEach(item -> {
            HBox line = new HBox();
            TextField mappingName = new TextField();
            mappingName.setText(fieldMappings.get(item.getLabel()));
            mappingName.textProperty().addListener((observable, oldValue, newValue) -> {
                fieldMappings.put(item.getLabel(), newValue);
            });
            line.getChildren().add(mappingName);
            switch(item.getType()) {
                case TEXT -> {
                    Label label = new Label();
                    label.setPrefWidth(120);
                    label.setText(item.getLabel());
                    TextFlow texteditor = new TextFlow();

                    texteditor.getChildren().add(new Text(item.getContent()));
                    line.getChildren().add(label);
                    line.getChildren().add(texteditor);
                }
                case STRING -> {
                    Label label = new Label();
                    label.setPrefWidth(120);
                    label.setText(item.getLabel());
                    TextFlow texteditor = new TextFlow();
                    texteditor.getChildren().add(new Text(item.getContent()));
                    line.getChildren().add(label);
                    line.getChildren().add(texteditor);
                }
                case IMAGE -> {
                    Label label = new Label();
                    label.setPrefWidth(120);
                    label.setText(item.getLabel());
                    ImageView v = new ImageView();
                    v.setPreserveRatio(true);
                    v.setFitWidth(250);
                    v.setFitHeight(250);

                    v.setImage(new Image(item.getContent()));


                    line.getChildren().add(label);
                    line.getChildren().add(v);
                }
                case HEADER -> {
                    VBox label = new VBox();
                    label.setPrefWidth(120);

                    TextFlow content = new TextFlow();
                    Text text = new Text(item.getContent());
                    text.setStyle("-fx-font-weight: bold;");
                    content.setTextAlignment(TextAlignment.CENTER);
                    content.getChildren().add(text);
                    HBox.setHgrow(content, Priority.ALWAYS);
                    line.getChildren().add(label);
                    line.getChildren().add(content);
                }
                case RESOURCE -> {
                    Label label = new Label();
                    label.setPrefWidth(120);
                    label.setText(item.getLabel());
                    TextField value = new TextField(String.valueOf(item.getValue()));
                    Label maxValue = new Label(String.valueOf(item.getMax()));

                    value.textProperty().addListener((editor, oldText, newText) -> {
                        item.setValue(Integer.valueOf(newText));
                    });

                    line.getChildren().add(label);
                    line.getChildren().add(value);
                    line.getChildren().add(new Label(" / "));
                    line.getChildren().add(maxValue);
                }
            }
            actorNoteItems.getChildren().add(line);
        });
    }

    @Override
    public void register(RegistryInterface registryInterface) {
        registryInterface.registerDataProvider(this);
        registryInterface.registerType("actor", ActorNote.class);
    }
}
