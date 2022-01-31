package com.bk.cloud.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;

public class regController {
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField nicknameField;
    @FXML
    private TextArea textArea;

    private TableController controller;

    public void setController(TableController controller) {
        this.controller = controller;
    }

    @FXML
    public void tryToReg(ActionEvent actionEvent) throws IOException {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String nickname = nicknameField.getText().trim();

        if (login.equals("") || password.equals("") || nickname.equals("")) {
            textArea.appendText("Поля должны быть не пустые\n");
            return;
        }

        if (login.contains(" ") || password.contains(" ") || nickname.contains(" ")) {
            textArea.appendText("Логин пароль и никнейм не должны содержать пробелы\n");
            return;
        }

        controller.registration(login, password, nickname);
    }

    public void regResult(String msg) {
        textArea.appendText(msg + "\n");
    }
}
