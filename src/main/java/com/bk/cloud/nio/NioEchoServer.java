package com.bk.cloud.nio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NioEchoServer {
    /**
     * Сделать терминал, которые умеет обрабатывать команды:
     * ls - список файлов в директории
     * cd dir_name - переместиться в директорию
     * cat file_name - распечатать содержание файла на экран
     * mkdir dir_name - создать директорию в текущей
     * touch file_name - создать пустой файл в текущей директории
     */

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer buf;

    private File currentDir = new File(System.getProperty("user.home"));

    private Path path = Paths.get(currentDir.toString());

    public NioEchoServer() throws IOException {
        buf = ByteBuffer.allocate(5);
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(8189));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started...");

        while (serverChannel.isOpen()) {
            selector.select(); // block
            System.out.println("Keys selected...");
            System.out.println("Current directory: " + path);
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept();
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder s = new StringBuilder();
        int read = 0;
        while (true) {
            read = channel.read(buf);
            if (read == 0) {
                break;
            }
            if (read < 0) {
                channel.close();
                return;
            }
            buf.flip();
            while (buf.hasRemaining()) {
                s.append((char) buf.get());
            }
            buf.clear();
        }
        // process(s)
        String command = s.toString().replace("\r\n", "");
        String[] com = command.split(" ");
        if (com.length > 0) {
            if (com[0].equals("ls")) {        //Список файлов в директории
                listOfDirectory(channel);
            } else if (com[0].equals("cd")) {  //перемещение по директориям
                try {
                    String newDirectory = com[1];
                    changeDirectory(channel, newDirectory);
                } catch (Exception e) {
                    String eMsg = "Ошибка: Не указана папка";
                    channel.write(ByteBuffer.wrap(eMsg.getBytes(StandardCharsets.UTF_8)));
                }

            } else if (com[0].equals("cat")) {
                try {
                    String file = com[1];
                    readFile(channel, file);
                } catch (Exception e) {
                    String eMsg = "Ошибка: Не указано имя файла";
                    channel.write(ByteBuffer.wrap(eMsg.getBytes(StandardCharsets.UTF_8)));
                }
            } else if (com[0].equals("mkdir")) {
                try {
                    String newDir = com[1];
                    createDir(channel, newDir);
                } catch (Exception e) {
                    String eMsg = "Ошибка: Не указано имя папки";
                    channel.write(ByteBuffer.wrap(eMsg.getBytes(StandardCharsets.UTF_8)));
                }
            } else if (com[0].equals("touch")) {
                try {
                    String newFile = com[1];
                    createFile(channel, newFile);
                } catch (Exception e) {
                    String eMsg = "Ошибка: Не указано имя файла";
                    channel.write(ByteBuffer.wrap(eMsg.getBytes(StandardCharsets.UTF_8)));
                }
            }
        } else {
            System.out.println("Received: " + command);
            channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.UTF_8)));
        }

    }

    private void createFile(SocketChannel channel, String newFile) throws IOException {
        Path path = Paths.get(currentDir + "/" + newFile);
        String s = null;
        try {
            Files.createFile(path);
            s = "Файл " + newFile + " создан в папке " + currentDir.toString();
            System.out.println("Received: " + newFile);
            channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            s = "Ошибка: Файл " + newFile + " создать не удалось...";
            channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.UTF_8)));
            e.printStackTrace();
        }
    }

    private void createDir(SocketChannel channel, String newDir) throws IOException {
        Path path = Paths.get(currentDir + "/" + newDir);
        String s = null;
        try {
            Path dir = Files.createDirectory(path);
            s = "Каталог " + newDir + " создан";
            System.out.println("Received: " + newDir);
            channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (FileAlreadyExistsException e) {
            s = "Ошибка: Каталог " + newDir + " существует";
            System.out.println(s);
            channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            s = "Ошибка: Каталог " + newDir + " создать не удалось...";
            channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.UTF_8)));
            e.printStackTrace();
        }

    }

    private void readFile(SocketChannel channel, String file) throws IOException {
        Path currentFile = Paths.get(currentDir.toString() + "/" + file);
        System.out.println("Read File: " + currentFile);
        List<String> lines = Files.readAllLines(currentFile, StandardCharsets.UTF_8);
        for (String s : lines) {
            channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void changeDirectory(SocketChannel channel, String dir) throws IOException {
        if (dir.equals("..")) {
            currentDir = new File(currentDir.getParent());
        } else {
            path = Paths.get(currentDir + "/" + dir);
            currentDir = path.toFile();
        }
        String currentDirectory = "Current directory " + currentDir + "\r\n";
        channel.write(ByteBuffer.wrap(currentDirectory.getBytes(StandardCharsets.UTF_8)));
    }

    private void handleAccept() throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        channel.write(ByteBuffer.wrap(
                "Hello user. Welcome to our terminal\n\r".getBytes(StandardCharsets.UTF_8)
        ));
        System.out.println("Client accepted...");
    }

    public static void main(String[] args) throws IOException {
        new NioEchoServer();
    }

    private void listOfDirectory(SocketChannel channel) throws IOException {
        File[] listFiles = currentDir.listFiles();
        String currentDirectory = "Current directory " + currentDir.getAbsolutePath() + "\r\n";
        channel.write(ByteBuffer.wrap(currentDirectory.getBytes(StandardCharsets.UTF_8)));
        for (int i = 0; i < listFiles.length; i++) {
            String files = listFiles[i].getName() + "\r\n";
            channel.write(ByteBuffer.wrap(files.getBytes(StandardCharsets.UTF_8)));
        }

    }
}
