package me.ericballard.sslchat;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import me.ericballard.sslchat.gui.Controller;

import java.io.IOException;

public class SSLChat extends Application {

    // Controller logic for interface
    Controller controller;

    // User-defined name
    public String username;

    public boolean muted;

    public int onlineUsers;

    @Override
    public void start(Stage stage) {
        // Load interface from xml and register controller
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gui/resources/Interface.fxml"));
        loader.setController((controller = new Controller(this)));
        Parent root;

        try {
            root = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Configure window functionality
        stage.setScene(new Scene(root, 600, 400, Color.TRANSPARENT));
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.show();
    }
}
