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
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.ericballard.sslchat.SSLChat;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class Controller implements Initializable {

    @FXML // Root
    public AnchorPane anchorPane;

    @FXML  // Message history
    public ListView<TextFlow> listView;

    @FXML  // Username
    public TextField textField;

    @FXML // Message draft
    TextArea textArea;

    @FXML // Displays # of online users in connected server
    public Text onlineTxt, imgTxt, soundTxt, connectTxt;

    @FXML // User-name color
    ColorPicker colorPicker;

    @FXML // Inform user # of typing clients
    public Label typingLbl;

    @FXML // Server status
    public Circle circle, statusCircle;

    @FXML // Title bar buttons
    Button minBtn, exitBtn;

    @FXML // User controls
    public ImageView mediaImg, soundImg, connectImg;

    @FXML
    GridPane controlGrid, titleGrid;

    // Cached red fill for status cicle
    public Paint offFill;

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
        offFill = statusCircle.getFill();

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
        textField.setOnKeyReleased(e -> {
            String name = textField.getText();

            if (name == null || name.length() < 1) {
                app.controller.connectTxt.setDisable(true);
                app.controller.connectImg.setDisable(true);
                return;
            } else
                name = name.toLowerCase();

            // Disallow special characters in name
            if (!Pattern.matches("[a-zA-Z]+", name)) {
                textField.setText(app.username);

                if (app.username != null)
                    textField.positionCaret(app.username.length());
                else {
                    app.controller.connectTxt.setDisable(true);
                    app.controller.connectImg.setDisable(true);
                }

                e.consume();
                return;
            }

            app.username = name;
            app.controller.connectTxt.setDisable(false);
            app.controller.connectImg.setDisable(false);
        });

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
                // User is actively drafting message
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
            if (msg == null || msg.length() < 2) {
                e.consume();
                return;
            }

            Color color = colorPicker.getValue();
            String userColor = color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getOpacity();
            String data = userColor + ";" + app.username + ";" + msg;

            // User is sending new message
            textArea.setText(null);
            typing = false;
            e.consume();

            if (app.server != null && app.server.started) {
                ArrayList<String> clientsToInform = (ArrayList<String>) app.server.connectedClients.clone();
                clientsToInform.remove(app.username);

                app.server.dataToSend.put("MESSAGE:" + data, clientsToInform);

                // Add msg to local client
                app.addMessage(userColor, app.username, msg, false);
            } else {
                app.client.dataToSend.add("MESSAGE:" + data);
            }
        });

        /*
            User Controls
         */

        // Style
        imgTxt.disableProperty().addListener(e -> imgTxt.setOpacity(imgTxt.isDisabled() ? 0.5 : 1.0));
        soundTxt.disableProperty().addListener(e -> soundTxt.setOpacity(soundTxt.isDisabled() ? 0.5 : 1.0));
        connectTxt.disableProperty().addListener(e -> connectTxt.setOpacity(connectTxt.isDisabled() ? 0.5 : 1.0));

        mediaImg.disableProperty().addListener(e -> mediaImg.setOpacity(mediaImg.isDisabled() ? 0.5 : 1.0));
        soundImg.disableProperty().addListener(e -> soundImg.setOpacity(soundImg.isDisabled() ? 0.5 : 1.0));
        connectImg.disableProperty().addListener(e -> connectImg.setOpacity(connectImg.isDisabled() ? 0.5 : 1.0));

        // Toggle message notification
        soundImg.setOnMouseClicked(e -> {
            if (app.muted = (!app.muted)) {
                // Muted
                soundImg.setImage(new Image(new File("src/me/ericballard/sslchat/gui/resources/images/mute.png").toURI().toString()));
            } else {
                // Un-muted
                soundImg.setImage(new Image(new File("src/me/ericballard/sslchat/gui/resources/images/sound.png").toURI().toString()));
            }
        });

        // Open server panel
        connectImg.setOnMouseClicked(e -> {
            textField.setDisable(true);
            app.alerts.getOption();
        });

        // Send media to server
        mediaImg.setOnMouseClicked(e -> {
            FileChooser fileChooser = new FileChooser();

            //Set extension filter
            FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.JPG");
            FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
            fileChooser.getExtensionFilters().addAll(extFilterJPG, extFilterPNG);

            //Show open file dialog
            File file = fileChooser.showOpenDialog(null);

            if (file == null) {
                System.out.print("Failed to get selected image.");
                return;
            }

            Color color = colorPicker.getValue();
            String userColor = color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getOpacity();
            String data = "MEDIA:" + userColor + ";" + app.username;

            System.out.println("Queued media for transfer.");

            if (app.server != null && app.server.started) {
                //TODO
            } else {
                app.client.imgToSend = file;
                app.client.dataToSend.add(data);
            }
        });
    }

    public static Optional<String> getExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }
}
