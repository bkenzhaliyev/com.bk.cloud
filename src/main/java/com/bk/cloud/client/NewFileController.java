package com.bk.cloud.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

public class NewFileController {
    @FXML
    public TextField newFileName;
    @FXML
    public Button btnCancel;
    @FXML
    public Button btnNewFile;
    public Button btnNewDir;

    private TableController controller;

    public void newDir(ActionEvent actionEvent) throws IOException {
        String fileName = newFileName.getText().trim();
        controller.CreateNewDirName(fileName);
    }

    public void cancel(ActionEvent actionEvent) {
        ((Stage) (((Button) actionEvent.getSource()).getScene().getWindow())).close();
    }

    public void setController(TableController controller) {
        this.controller = controller;
    }

    public void newFile(ActionEvent actionEvent) throws IOException {
        String fileName = newFileName.getText().trim();
        controller.CreateNewFile(fileName);
    }
}

