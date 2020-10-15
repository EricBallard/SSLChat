package me.ericballard.sslchat.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import me.ericballard.sslchat.SSLChat;
import me.ericballard.sslchat.network.client.Client;
import me.ericballard.sslchat.network.server.Server;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML // Root
    AnchorPane anchorPane;

    @FXML  // Message history
    ListView listView;

    @FXML  // Username
    TextField textField;

    @FXML // Message draft
    TextArea textArea;

    @FXML // Displays # of online users in connected server
    public Text onlineTxt;

    @FXML // User-name color
    ColorPicker colorPicker;

    @FXML // Inform user # of typing clients
    public Label typingLbl;

    @FXML // Server status
    Circle circle;

    @FXML // Title bar buttons
    Button minBtn, exitBtn;

    @FXML // User controls
    ImageView mediaImg, soundImg, connectImg;

    @FXML
    GridPane controlGrid, titleGrid;

    // Instance of main
    SSLChat app;

    // Client is drafting message
    public boolean typing;

    public Controller(SSLChat app) {
        this.app = app;
    }

    double xOffset = -1, yOffset = -1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Style
        anchorPane.setBackground(Background.EMPTY);
        anchorPane.setClip(new Circle(circle.getLayoutX(), circle.getLayoutY(), circle.getRadius()));
        textArea.setStyle("-fx-focus-color: transparent; -fx-text-box-border: transparent;");

        /*
             Window functionality
         */

        anchorPane.setOnMousePressed(e -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
        });

        anchorPane.setOnMouseDragged(e -> {
            Stage stage = (Stage) anchorPane.getScene().getWindow();
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
            e.consume();
        });

        controlGrid.setOnMouseEntered(e -> controlGrid.setStyle("-fx-background-color:  rgba(50, 50, 50, 0.85);"));
        controlGrid.setOnMouseExited(e -> controlGrid.setStyle("-fx-background-color:  rgba(50, 50, 50, 0.75);"));

        titleGrid.setOnMouseEntered(e -> titleGrid.setStyle("-fx-background-color:  rgba(50, 50, 50, 0.85);"));
        titleGrid.setOnMouseExited(e -> titleGrid.setStyle("-fx-background-color:  rgba(50, 50, 50, 0.75);"));

        /*
            Title bar functionality
         */

        //Exit application
        exitBtn.setOnAction(e -> ((Stage) anchorPane.getScene().getWindow()).close());

        // Minimize window
        minBtn.setOnAction(e -> ((Stage) anchorPane.getScene().getWindow()).setIconified(true));

        /*
             Message and username functionality
         */

        // Set user-defined name (username)
        textField.setOnKeyReleased(e -> app.username = textField.getText().toLowerCase());

        textArea.focusedProperty().addListener(e -> {
            // User has stopped typing while drafting a message
            if (!typing)
                return;
            else
                typing = false;

            if (app.server != null && app.server.started) {
                // Inform users on server we have stopped typing
                ArrayList<String> clientsToInform = (ArrayList<String>) app.server.connectedClients.clone();
                clientsToInform.remove(app.username);

                app.server.dataToSend.put("IDLE:" + app.username, clientsToInform);
            }
        });

        // Send message when press enter
        textArea.setOnKeyPressed(e -> {
            // No username - prompt user to type a name
            if (app.username == null || app.username.length() < 1) {
                e.consume();
                textField.requestFocus();
                return;
            }

            if (e.getCode() != KeyCode.ENTER) {
                // User is actively drafting messag
                if (typing)
                    return;
                else
                    typing = true;

                if (app.server != null && app.server.started) {
                    // User is hosting server
                    // Inform connected clients - user is typing
                    ArrayList<String> clientsToInform = (ArrayList<String>) app.server.connectedClients.clone();
                    clientsToInform.remove(app.username);

                    app.server.dataToSend.put("TYPING:" + app.username, clientsToInform);
                }
                return;
            }

            String msg = textArea.getText();
            if (msg == null || msg.length() < 1)
                return;

            e.consume();
            typing = false;
            textArea.setText(null);
            System.out.println("Sending message: (" + app.username + ") " + msg);
            //TODO
        });

        /*
            User Controls
         */

        // Toggle message notification
        soundImg.setOnMouseClicked(e -> {
            if (app.muted = (!app.muted)) {
                // Muted
                soundImg.setImage(new Image("gui/resources/mute.png"));
            } else {
                // Un-muted
                soundImg.setImage(new Image("gui/resources/sound.png"));
            }
        });

        // Send Image
        mediaImg.setOnMouseClicked(e -> {

        });

        // Open server panel

        //TODO prevent access if username is not set

        connectImg.setOnMouseClicked(e -> {
            (app.server = new Server(app)).initialize();
        });

        mediaImg.setOnMouseClicked(e -> {
            (app.client = new Client(app)).initialize();
        });
    }
}





























