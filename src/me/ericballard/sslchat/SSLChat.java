package me.ericballard.sslchat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import me.ericballard.sslchat.gui.Alerts;
import me.ericballard.sslchat.gui.Controller;
import me.ericballard.sslchat.network.client.Client;
import me.ericballard.sslchat.network.server.Server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SSLChat extends Application {

    // Controller logic for interface
    public Controller controller;

    public Alerts alerts = new Alerts(this);

    // User-defined name
    public String username;

    public boolean muted;

    public Server server;

    public Client client;

    private MediaPlayer alertPlayer;

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

            if (server!= null) {
                server.disconnecting = true;

                ArrayList<String> toInform = (ArrayList<String>) server.connectedClients.clone();
                toInform.remove(username);

                server.dataToSend.put("SHUTDOWN:Server", toInform);
            }
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

    public void addMessage(String colorCode, String fromUser, String newMessage, boolean playSound) {
        // Parse color
        String[] cInfo = colorCode.split(",");

        Color color = Color.color(Double.parseDouble(cInfo[0]), Double.parseDouble(cInfo[1]),
                Double.parseDouble(cInfo[2]), Double.parseDouble(cInfo[3]));

        Font f = new Font(14);

        Text user = new Text(fromUser);
        user.setFont(Font.font(f.getFamily(), FontWeight.BOLD, FontPosture.REGULAR, 13));
        user.setFill(color);

        Text separator = new Text(": ");
        separator.setFill(Color.WHITE);
        separator.setFont(f);

        TextFlow message = new TextFlow(new Text("\t"), user, separator);
        ArrayList<TextFlow> formattedMessage = new ArrayList<>();

        // Wrap message
        int lineLimit = 52, length = newMessage.length(), lines = (length / lineLimit);

        for (int i = -1; i <= lines; i++) {
            if (i < 0)
                continue;

            boolean endOfMsg = length < lineLimit;
            int charOffset = (endOfMsg ? length : lineLimit) - 1, charFailSafe = new Integer(charOffset);

            while (newMessage.charAt(charOffset) != ' ') {
                if (endOfMsg)
                    break;
                else if (charOffset <= 1) {
                    charOffset = charFailSafe;
                    break;
                }

                charOffset -= 1;
            }

            String newLine = newMessage.substring(0, charOffset + (endOfMsg ? 1 : 0));
            newMessage = newMessage.substring(charOffset);

            length -= charOffset;


            Text statement = new Text(newLine);
            statement.setFill(Color.LIGHTGRAY);
            statement.setFont(f);

            if (i == 0) {
                message.getChildren().add(statement);
                formattedMessage.add(message);
            } else {
                formattedMessage.add(new TextFlow(new Text("\t\t"), statement));
            }
        }

        Platform.runLater(() -> {
            controller.listView.getItems().addAll(formattedMessage);
            controller.listView.scrollTo(controller.listView.getItems().size() - 1);

            if (playSound) {
                if (alertPlayer == null) {
                    Media alertSound = new Media(new File("src/me/ericballard/sslchat/gui/resources/alert.wav").toURI().toString());
                    alertPlayer = new MediaPlayer(alertSound);
                }

                // Rewind
                alertPlayer.seek(Duration.ZERO);
                alertPlayer.play();
            }
        });
    }
}
