package com.bk.cloud.client;

import com.bk.cloud.utils.FileInfo;
import com.bk.cloud.utils.SenderUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class TableController implements Initializable {

    private final String HOST = "localhost";
    private final int PORT = 8189;


    private ObservableList<FileInfo> clientList = FXCollections.observableArrayList();

    @FXML
    public TextField clientDir;
    @FXML
    public TextField serverDir;

    @FXML
    public TableView<FileInfo> clientTab;
    @FXML
    public TableColumn<FileInfo, String> serverFileName;
    @FXML
    public TableColumn<FileInfo, String> serverFileType;
    @FXML
    public TableView<FileInfo> serverTab;
    @FXML
    public TableColumn<FileInfo, String> clientFileName;
    @FXML
    public TableColumn<FileInfo, String> clientFileType;

    private DataInputStream is;
    private DataOutputStream os;

    private File currentDir;
    private File serverRootDir;


    private byte[] buf;

    private void read() {
        try {
            while (true) {
                String command = is.readUTF();
                if (command.equals("#List")) {
                    Platform.runLater(() -> serverTab.getItems().clear());
                    Platform.runLater(() -> serverTab.getItems().add(new FileInfo("..", "[DIR]")));
                    int count = is.readInt();
                    for (int i = 0; i < count; i++) {
                        String fileName = is.readUTF();
                        FileInfo files = new FileInfo();
                        files.fileInfoFromString(fileName);
                        Platform.runLater(() -> serverTab.getItems().add(files));
                        // устанавливаем тип и значение которое должно хранится в колонке
                        serverFileName.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("fileName"));
                        serverFileType.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("fileType"));
                    }
                }
                if (command.equals("#SEND#FILE")) {
                    SenderUtils.getFileFromInputStream(is, currentDir);
                }
                if(command.equals("#ROOTDIR")){
                    String fileName = is.readUTF();
                    System.out.println(fileName);
//                    serverRootDir  = new File(System.getProperty(fileName));
//                    Path path = serverRootDir.toPath().resolve(fileName);
//                    serverRootDir = path.toFile();
                    serverDir.setText(fileName);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            // reconnect to server
        }
    }

    private void fillCurrentDirFiles() {
//      Очищаем массив с данными
        clientDir.setText(currentDir.getAbsolutePath());
        clientList.clear();
        File[] listFiles = currentDir.listFiles();
        String rootPath = currentDir.toPath().getRoot().toString();
        if (!rootPath.toString().equals(currentDir.toString())) {
            clientList.add(new FileInfo("..", "[DIR]"));
        }

        for (int i = 0; i < listFiles.length; i++) {
            Path path = currentDir.toPath().resolve(listFiles[i].toString());
            if (Files.isDirectory(path)) {
                clientList.add(new FileInfo(listFiles[i].getName(), "[DIR]"));
            } else {
                long size = listFiles[i].length();
                clientList.add(new FileInfo(listFiles[i].getName(), String.format("%,d bytes", size)));
            }
        }

        clientTab.setItems(clientList);
        // устанавливаем тип и значение которое должно хранится в колонке
        clientFileName.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("fileName"));
        clientFileType.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("fileType"));

    }

    private void initClickListener() {
        clientTab.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                FileInfo selectedFile = clientTab.getSelectionModel().getSelectedItem();
                String fileName = selectedFile.getFileName();
//                System.out.println("select file: " + selectedFile.getFileName());
                Path path = currentDir.toPath().resolve(fileName);
                if (Files.isDirectory(path)) {
                    if (selectedFile.getFileName().equals("..")) {
                        currentDir = new File(currentDir.getParent());
                    } else {
                        currentDir = path.toFile();
                    }

//                    System.out.println("current dir: " + currentDir);
                    fillCurrentDirFiles();
                }
            }
        });

        serverTab.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                FileInfo selectedFile = serverTab.getSelectionModel().getSelectedItem();
//                System.out.println("Selected: " + selectedFile.getFileName());
                if (selectedFile.getFileType().equals("[DIR]")) {
                    try {
                        os.writeUTF("#LIST#DIR");
                        os.writeUTF(selectedFile.getFileName());
                        os.flush();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }

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
        FileInfo selectedFile = clientTab.getSelectionModel().getSelectedItem();
        String fileName = selectedFile.getFileName();
        File currentFile = currentDir.toPath().resolve(fileName).toFile();
        System.out.println("File upload " + currentFile);
        SenderUtils.loadFileToOutputStream(os, currentFile);
    }

    public void download(ActionEvent actionEvent) throws IOException {
        FileInfo selectedFile = clientTab.getSelectionModel().getSelectedItem();
        String fileName = selectedFile.getFileName();
        System.out.println("File download " + fileName);
        os.writeUTF("#GET#FILE");
        os.writeUTF(fileName);
        os.flush();
    }

}
