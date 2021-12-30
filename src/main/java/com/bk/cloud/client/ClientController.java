package com.bk.cloud.client;

import com.bk.cloud.utils.SenderUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    private final String HOST = "localhost";
    private final int PORT = 8189;

    public ListView<String> listClient;
    public ListView<String> listServer;

    private DataInputStream is;
    private DataOutputStream os;

    private File currentDir;

    private byte[] buf;

    private void read() {
        try {
            while (true) {
                String command = is.readUTF();
                if (command.equals("#List")) {
                    Platform.runLater(() -> listServer.getItems().clear());
                    int count = is.readInt();
                    for (int i = 0; i < count; i++) {
                        String fileName = is.readUTF();
                        Platform.runLater(() -> listServer.getItems().add(fileName));
                    }
                }
                if (command.equals("#SEND#FILE")) {
                    SenderUtils.getFileFromInputStream(is, currentDir);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            // reconnect to server
        }
    }

    private void fillCurrentDirFiles() {
        listClient.getItems().clear();
        listClient.getItems().add("..");
        listClient.getItems().addAll(currentDir.list());
    }

    private void initClickListener() {
        listClient.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String fileName = listClient.getSelectionModel().getSelectedItem();
                Path path = currentDir.toPath().resolve(fileName);
                if (Files.isDirectory(path)) {
                    currentDir = path.toFile();
                    fillCurrentDirFiles();
                }
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            buf = new byte[256];
            currentDir = new File(System.getProperty("user.home"));
            fillCurrentDirFiles();
            initClickListener();
            Socket socket = new Socket(HOST, PORT);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());

            Thread readThread = new Thread(this::read);
            readThread.setDaemon(true);
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String fileName = listClient.getSelectionModel().getSelectedItem();
        File currentFile = currentDir.toPath().resolve(fileName).toFile();
        System.out.println("File upload " + currentFile);
        SenderUtils.loadFileToOutputStream(os, currentFile);
    }

    public void download(ActionEvent actionEvent) throws IOException {
        String fileName = listServer.getSelectionModel().getSelectedItem();
        System.out.println("File download " + fileName);
        os.writeUTF("#GET#FILE");
        os.writeUTF(fileName);
        os.flush();
    }
}
