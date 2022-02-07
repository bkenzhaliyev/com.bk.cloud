package com.bk.cloud.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Server {
    static final int PORT = 8189;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private AuthService authService;
    ServerSocket server;
    private List<FileProcessorHandler> clients;
    Socket socket;

    public Server() throws IOException {
        LogManager manager = LogManager.getLogManager();
        try {
            manager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        clients = new CopyOnWriteArrayList<>();

        if (!SQLHandler.connect()) {
            logger.log(Level.WARNING, "Не удалось подключиться к БД", true);
            throw new RuntimeException("Не удалось подключиться к БД");
        }

        authService = new DbAuthService();

        try {
            server = new ServerSocket(PORT);
            logger.log(Level.INFO, "Server started!", true);
            System.out.println("Server started...");
            while (true) {
                socket = server.accept();
                logger.log(Level.INFO, "Client connected", true);
                new FileProcessorHandler(socket, this);
//                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            SQLHandler.disconnect();
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isLoginAuthenticated(String login) {
        for (FileProcessorHandler c : clients) {
            if (c.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }
}
