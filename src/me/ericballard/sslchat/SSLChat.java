package me.ericballard.sslchat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import me.ericballard.sslchat.gui.Controller;
import me.ericballard.sslchat.network.client.Client;
import me.ericballard.sslchat.network.server.Server;

import java.io.IOException;
import java.util.ArrayList;

public class SSLChat extends Application {

    // Controller logic for interface
    public Controller controller;

    // User-defined name
    public String username;

    public boolean muted;

    public Server server;

    public Client client;

    public ArrayList<String> typingClients = new ArrayList<>();

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

        // When app is closed
        stage.setOnHidden(e -> {
            if (client != null)
                client.disconnecting = true;

        });

    }

    public void updateTypingCount() {
        int typing = typingClients.size();

        if (typing == 0) {
            Platform.runLater(() -> controller.typingLbl.setText(null));
            return;
        }

        String msg = typing > 2 ? (typing + " people are typing") : typingClients.get(0) + (typing == 2 ? " and " + typingClients.get(1) + " are" : " is") + " typing";
        Platform.runLater(() -> controller.typingLbl.setText(msg));
    }
}
