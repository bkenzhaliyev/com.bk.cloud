package com.bk.cloud.server;

import com.bk.cloud.utils.SenderUtils;
import javafx.scene.control.ListView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

public class FileProcessorHandler implements Runnable {

    private File currentDir;
    private static final int SIZE = 256;
    private DataInputStream is;
    private DataOutputStream os;
    private byte[] buf;

    public FileProcessorHandler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[SIZE];
        currentDir = new File("serverDir");
        SenderUtils.sendFilesListToOutputStream(os, currentDir);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = is.readUTF();
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
