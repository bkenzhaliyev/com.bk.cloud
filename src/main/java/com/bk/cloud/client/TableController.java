package com.bk.cloud.client;

import com.bk.cloud.utils.FileInfo;
import com.bk.cloud.utils.SenderUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
    @FXML
    public Button tryToAuth;
    @FXML
    public Button tryToReg;
    @FXML
    public TextField pswdField;
    @FXML
    public TextField userName;
    @FXML
    public HBox loginPanel;

    private DataInputStream is;
    private DataOutputStream os;

    private File currentDir;
    private String serverRootDir;

    private String newDirName;
    private Stage newFileStage;
    private Stage regStage;

    private Stage stage;

    private NewFileController newFileController;
    private regController regController;

    private boolean authenticated;
    private String nickname;
    private String login;

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
                if (command.equals("#ROOTDIR")) {
                    String fileName = is.readUTF();
//                    System.out.println(fileName);
                    serverRootDir = fileName;
                    serverDir.setText(fileName);
                }
                if (command.equals("#regOk")) {
                    regController.regResult("Регистрация прошла успешно");
                    nickname = userName.getText();
                    tryToReg.setDisable(true);
                }
                if (command.equals("#regNo")) {
                    regController.regResult("Логин или никнейм уже заняты");
                }
                if (command.equals("#AuthOK")) {
                    loginPanel.setVisible(false);
                    nickname = userName.getText();
                    setTitle(nickname);
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
        clientTab.sort();


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
                if (selectedFile.getFileName().equals("..")) {
                    if (selectedFile.getFileName().equals(serverRootDir)) {
                        return;
                    }
                }
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
            // устанавливаем тип и значение которое должно хранится в колонке
            clientFileName.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("fileName"));
            clientFileType.setCellValueFactory(new PropertyValueFactory<FileInfo, String>("fileType"));
            clientTab.getSortOrder().add(clientFileType);
            clientTab.getSortOrder().add(clientFileName);

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
//        System.out.println("File download " + fileName);
        os.writeUTF("#GET#FILE");
        os.writeUTF(fileName);
        os.flush();
    }

    public void btnChooseDir(ActionEvent actionEvent) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(new Stage());
        if (selectedDirectory != null) {
            currentDir = selectedDirectory;
            clientDir.setText(currentDir.getAbsolutePath());
            fillCurrentDirFiles();
        }
    }

    public void delete(ActionEvent actionEvent) throws IOException {
        FileInfo selectedFile = serverTab.getSelectionModel().getSelectedItem();
        String fileName = selectedFile.getFileName();
        os.writeUTF("#DELETE#FILE");
        os.writeUTF(fileName);
        os.flush();
    }

    public void newDir(ActionEvent actionEvent) throws IOException {
        if (newFileStage == null) {
            createNewFileWindow(true);
        }
        newFileStage.show();
    }

    public void createNewFileWindow(boolean dir) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("newFile.fxml"));
            Parent root = fxmlLoader.load();
            newFileStage = new Stage();
            String title = null;
            if (dir = true) {
                title = "Создание папки...";
            } else {
                title = "Создание файла...";
            }
            newFileStage.setTitle(title);
            newFileStage.setScene(new Scene(root, 292, 80));
            newFileController = fxmlLoader.getController();
            newFileController.setController(this);

            newFileStage.initStyle(StageStyle.UTILITY);
            newFileStage.initModality(Modality.APPLICATION_MODAL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void CreateNewDirName(String dir) throws IOException {
        os.writeUTF("#CREATE#DIR");
        os.writeUTF(dir);
        os.flush();
    }

    public void registration(String login, String password, String nickname) throws IOException {
        String regMsg = String.format("%s %s %s", login, password, nickname);
        os.writeUTF("#NEW#USER");
        os.writeUTF(regMsg);
        os.flush();
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        clientTab.setVisible(authenticated);
        serverTab.setManaged(authenticated);

        if (!authenticated) {
            nickname = "";
        }
    }

    public void tryToAuth(ActionEvent actionEvent) throws IOException {
        String login = userName.getText().trim();
        this.login = login;
        String password = pswdField.getText().trim();

        String str = String.format("#AUTH %s %s", login, password);
//        System.out.println("Auth command: " + str);
        os.writeUTF("#AUTH");
        os.writeUTF(str);
        os.flush();
    }

    public void tryToReg(ActionEvent actionEvent) {
        if (regStage == null) {
            createRegWindow();
        }
        regStage.show();
    }

    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("Регистрация нового пользователя...");
            regStage.setScene(new Scene(root, 280, 139));
            regController = fxmlLoader.getController();
            regController.setController(this);

            regStage.initStyle(StageStyle.UTILITY);
            regStage.initModality(Modality.APPLICATION_MODAL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientUpDir(ActionEvent actionEvent) {
        String rootPath = currentDir.toPath().getRoot().toString();
        if (!rootPath.toString().equals(currentDir.toString())) {
            clientList.add(new FileInfo("..", "[DIR]"));
        }
    }

    private void setTitle(String nickname) {
        stage = (Stage) clientDir.getScene().getWindow();
        Platform.runLater(() -> {
        if (!nickname.equals("")) {
            stage.setTitle(String.format("MyCloud - [ %s ]", nickname));
        } else {
            stage.setTitle("MyCloud - необходимо авторизоваться...");
        }
        });
    }
}
