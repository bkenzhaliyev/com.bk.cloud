package com.bk.cloud.nio;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class PathExample {

    public static void main(String[] args) throws IOException {
        // Path path = Paths.get("dir/dir1/dir2/file.txt");
        Path path = Paths.get("dir", "dir1", "file.txt");
        Path dir = Paths.get("serverDir");
        Path fxml = dir.resolve("123.fxml");
        System.out.println(Files.exists(fxml));
        System.out.println(Files.size(fxml));

        System.out.println(dir);
        System.out.println(dir.toAbsolutePath());

        startListening(dir);
    }

    private static void startListening(Path path) throws IOException {
        WatchService service = FileSystems.getDefault().newWatchService();
        new Thread(() -> {
            try {
                while (true) {
                    WatchKey key = service.take();
                    List<WatchEvent<?>> events = key.pollEvents();
                    for (WatchEvent<?> event : events) {
                        System.out.println(event.context() + " " + event.kind());
                    }
                    key.reset();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        path.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    }
}
