package me.ericballard.sslchat.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.shape.Circle;
import me.ericballard.sslchat.SSLChat;

import java.net.URL;
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

    @FXML // Server status
    Circle circle;

    @FXML // User controls
    ImageView mediaImg, soundImg, connectImg;

    // Instance of main
    SSLChat app;

    public Controller(SSLChat app) {
        this.app = app;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Style
        anchorPane.setBackground(Background.EMPTY);
        anchorPane.setClip(new Circle(circle.getLayoutX(), circle.getLayoutY(), circle.getRadius()));
        textArea.setStyle("-fx-focus-color: transparent; -fx-text-box-border: transparent;");

        /*
             Message and username functionality
         */

        // Set user-defined name (username)
        textField.setOnKeyReleased(e -> app.username = textField.getText());

        // Send message when press enter
        textArea.setOnKeyPressed(e -> {
            if (e.getCode() != KeyCode.ENTER)
                return;

            // No username - prompt user to type a name
            if (app.username == null || app.username.length() < 1) {
                e.consume();
                textField.requestFocus();
                return;
            }

            String msg = textArea.getText();
            if (msg == null || msg.length() < 1)
                return;

            e.consume();
            textArea.setText(null);
            System.out.println("Sending message: (" + app.username + ") " + msg);
        });

        /*
            User Controls
         */

        // Toggle message notification
        soundImg.setOnMouseClicked(e -> app.muted = (!app.muted));

        // Send Image
        mediaImg.setOnMouseClicked(e -> {

        });

        // Open server panel

    }
}





























