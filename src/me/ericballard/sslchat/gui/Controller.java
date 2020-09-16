package me.ericballard.sslchat.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    AnchorPane anchorPane;

    @FXML
    Circle circle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        anchorPane.setBackground(Background.EMPTY);
        anchorPane.setClip(new Circle(circle.getLayoutX(), circle.getLayoutY(), circle.getRadius()));
    }

    public void resize() {

    }
}
