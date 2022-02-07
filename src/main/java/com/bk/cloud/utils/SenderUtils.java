package com.bk.cloud.utils;

import javax.xml.crypto.Data;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


public class SenderUtils {
    private static final int SIZE = 256;

    public static void getFileFromInputStream(DataInputStream is, File currentDir) throws IOException {
        String fileName = is.readUTF();
        byte[] buf = new byte[SIZE];
        long size = is.readLong();
        Path currentPath = currentDir.toPath().resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(currentPath.toFile())) {
            for (int i = 0; i < (size + SIZE - 1) / SIZE; i++) {
                int read = is.read(buf);
                fos.write(buf, 0, read);
            }
        }
    }

    public static void loadFileToOutputStream(DataOutputStream os, File file) throws IOException {
        os.writeUTF("#SEND#FILE");
        os.writeUTF(file.getName());
        os.writeLong(file.length());
        byte[] buf = new byte[SIZE];

        try (FileInputStream is = new FileInputStream(file)) {
            while (true) {
                int read = is.read(buf);
                if (read == -1) {
                    break;
                }
                os.write(buf, 0, read);
            }
            os.flush();
        }
    }

    public static void sendFilesListToOutputStream(DataOutputStream os, File currentDir) throws IOException {
        String[] files = currentDir.list();
        if (files != null) {
            os.writeUTF("#List");
            os.writeInt(files.length);
            for (String file : files) {
                Path path = currentDir.toPath().resolve(file);
                if (Files.isDirectory(path)) {
                    FileInfo fileInfo = new FileInfo(file, "[DIR]");
                    os.writeUTF(fileInfo.toString());
                } else {
                    long size = file.length();
                    FileInfo fileInfo = new FileInfo(file, String.format("%,d bytes", size));
                    os.writeUTF(fileInfo.toString());
                }
            }
        }

    }

    public static void sendRootDirToOutputStream(DataOutputStream os, File currentDir) throws IOException {
        os.writeUTF("#ROOTDIR");
        os.writeUTF(currentDir.toString());
    }

    public static void sendServerCurrentDirToOutputStream(DataOutputStream os, File currentDir) throws IOException {
        os.writeUTF("#CURRENTDIR");
        os.writeUTF(currentDir.toString());
    }

    public static void sendMsgToClient(DataOutputStream os, String msg) throws IOException {
        os.writeUTF(msg);
    }
}
