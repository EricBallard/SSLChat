package me.ericballard.sslchat.gui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    AnchorPane anchorPane;

    @FXML
    GridPane infoGrid, typingGrid, titleGrid, controlGrid;

    @FXML
    ListView listView;

    @FXML
    TextArea textArea;

    @FXML
    Circle circle;

    @FXML
    ImageView soundImg;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        anchorPane.setBackground(Background.EMPTY);
        anchorPane.setClip(new Circle(circle.getLayoutX(), circle.getLayoutY(), circle.getRadius()));

        titleGrid.setStyle("-fx-background-color: linear-gradient(from 25% 25% to 100% 100%, #252525, #505050)");

        infoGrid.setStyle("-fx-background-color: linear-gradient(from 25% 25% to 100% 100%, #252525, #505050)");

        controlGrid.setStyle("-fx-background-color: #252525");

        textArea.setStyle("-fx-focus-color: transparent; -fx-text-box-border: transparent;");

       //soundImg.setImage(new Image("resources/volume.png"));

    }

    public void resize() {

    }
}
