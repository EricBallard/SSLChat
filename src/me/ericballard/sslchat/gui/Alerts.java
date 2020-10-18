package me.ericballard.sslchat.gui;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import me.ericballard.sslchat.SSLChat;
import me.ericballard.sslchat.network.client.Client;
import me.ericballard.sslchat.network.server.Server;

import java.awt.*;
import java.util.Optional;

public class Alerts {

    private String typedIP;

    private final SSLChat app;

    public Alerts(SSLChat app) {
        this.app = app;
    }

    public void inform(String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            style(alert);

            alert.setHeaderText(header);
            alert.setContentText(content);

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(app.controller.anchorPane.getStylesheets().get(0));
            dialogPane.getStyleClass().add("myDialog");

            alert.show();
        });
    }


    public void getOption() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        style(alert);

        alert.setHeaderText("SERVER CONNECTION");
        alert.setContentText("Would you like to host or join a server?");

        ButtonType hostBtn = new ButtonType("Host");
        ButtonType joinBtn = new ButtonType("Join");

        alert.getButtonTypes().setAll(hostBtn, joinBtn, ButtonType.CANCEL);
        Optional<ButtonType> result = alert.showAndWait();

        ButtonType clicked = result.get();
        boolean host = clicked == hostBtn;

        if (host || clicked == joinBtn) {
            if (getDetails(host)) {
                app.controller.imgTxt.setDisable(false);
                app.controller.mediaImg.setDisable(false);

                app.controller.soundTxt.setDisable(false);
                app.controller.soundImg.setDisable(false);
            }
        } else {
            // Closed or canceled
            app.controller.textField.setDisable(false);
        }
    }

    private boolean getDetails(boolean host) {
        TextInputDialog dialog = new TextInputDialog("127.0.0.1:25565");
        style(dialog);

        dialog.setHeaderText("SERVER CONNECTION");
        dialog.setContentText("Please enter server address and port:");

        dialog.showAndWait().ifPresent(typed -> typedIP = typed);

        if (typedIP != null && typedIP.contains(":")) {
            String[] info = typedIP.split(":");
            String address = info[0], port = info[1];
            info = address.split("\\.");

            // Validate address
            if (address.contains(".") && info.length == 4) {
                // Connect to server
                if (host) {
                    //TODO check if connected to server/client (client!=null)
                    // disconnect and than set obj to null

                    (app.server = new Server(app, address, port)).initialize();
                } else {
                    (app.client = new Client(app, address, port)).initialize();
                }

                // Clear messages
                app.controller.listView.getItems().clear();

                // Show online status
                app.controller.statusCircle.setFill(app.controller.circle.getFill());
                return true;
            } else {
                // Invalid address
                app.alerts.inform("INVALID ADDRESS", "Unable to connect to server, please try again.");
            }
        } else {
            app.controller.textField.setDisable(false);
        }

        typedIP = null;
        return false;
    }

    private void style(Dialog alert) {
        alert.initStyle(StageStyle.TRANSPARENT);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(app.controller.anchorPane.getStylesheets().get(0));
        dialogPane.getStyleClass().add("myDialog");

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        Stage mainStage = (Stage) app.controller.anchorPane.getScene().getWindow();

        stage.setX(mainStage.getX());
        stage.setY(mainStage.getY());
    }
}
