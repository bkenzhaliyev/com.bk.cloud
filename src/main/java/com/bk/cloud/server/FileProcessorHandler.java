package com.bk.cloud.server;

import com.bk.cloud.utils.SenderUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class FileProcessorHandler {
    Server server;
    Socket socket;

    private File currentDir;
    private static final int SIZE = 256;
    private DataInputStream is;
    private DataOutputStream os;
    private byte[] buf;

    private boolean authenticated;
    private String nickname;
    private String login;
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public FileProcessorHandler(Socket socket, Server server) throws IOException {

        LogManager manager = LogManager.getLogManager();
        try {
            manager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[SIZE];
        currentDir = new File("serverDir");
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        executorService.execute(() -> {
            try {
                this.server = server;
                this.socket = socket;

                while (true) {
                    String command = is.readUTF();
                    if (command.equals("#AUTH")) {
                        String str = is.readUTF();
                        String[] token = str.split("\\s+");

                        nickname = server.getAuthService()
                                .getNicknameByLoginAndPassword(token[1], token[2]);
                        login = token[1];
                        if (nickname != null) {
                            if (!server.isLoginAuthenticated(login)) {
                                authenticated = true;

                                try {
                                    SenderUtils.sendFilesListToOutputStream(os, currentDir);
                                    SenderUtils.sendRootDirToOutputStream(os, currentDir);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                break;
                            } else {
                                logger.log(Level.INFO, "Пользователь с таким логином уже авторизован: " + nickname, true);
                            }
                        } else {
                            logger.log(Level.INFO, "Неверный логин/пароль " + login, true);
                        }
                    }
                    if (command.equals("#NEW#USER")){
                        String str = is.readUTF();
                        String[] token = str.split("\\s+");
                        if (token.length < 3) {
                            continue;
                        }
                        boolean regOk = server.getAuthService().
                                registration(token[0], token[1], token[2]);
                        String regMsg = null;
                        if (regOk) {
                            regMsg = "#regOk";
                        } else {
                            regMsg = "#regNo";
                        }
                        SenderUtils.sendMsgToClient(os, regMsg);
                    }
                }

                while (authenticated) {
                    String command = null;
                    try {
                        command = is.readUTF();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Server get command: " + command);
                    if (command.equals("#SEND#FILE")) {
                        SenderUtils.getFileFromInputStream(is, currentDir);
                        SenderUtils.sendFilesListToOutputStream(os, currentDir);
                    }
                    if (command.equals("#GET#FILE")) {
                        String fileName = is.readUTF();
                        File file = currentDir.toPath().resolve(fileName).toFile();
                        SenderUtils.loadFileToOutputStream(os, file);
                    }
                    if (command.equals("#LIST#DIR")) {
                        String directory = currentDir.toString() + "\\" + is.readUTF();
                        if (directory.equals("..")) {
                            currentDir = new File(currentDir.getParent());
                        } else {
                            currentDir = new File(directory);
                        }

                        SenderUtils.sendFilesListToOutputStream(os, currentDir);
                        SenderUtils.sendRootDirToOutputStream(os, currentDir);
                    }
                    if (command.equals("#DELETE#FILE")) {
                        String fileName = is.readUTF();
                        File file = currentDir.toPath().resolve(fileName).toFile();
                        file.delete();
                        SenderUtils.sendFilesListToOutputStream(os, currentDir);
                    }
                    if (command.equals("#CREATE#DIR")) {
                        String newDir = is.readUTF();
                        Path path = Paths.get(currentDir + "/" + newDir);
                        Path dir = Files.createDirectory(path);
                        SenderUtils.sendFilesListToOutputStream(os, currentDir);
                    }
                }

                if (authenticated) {
                    try {
                        SenderUtils.sendFilesListToOutputStream(os, currentDir);
                        SenderUtils.sendRootDirToOutputStream(os, currentDir);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }



    public String getNickname() {
        return nickname;
    }


    public String getLogin() {
        return login;
    }
}
