package com.reelvault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReelvaultApplication {
    public static void main(String[] args) {
        createDatabaseDirectory();
        SpringApplication.run(ReelvaultApplication.class, args);
    }

    /** SQLite creates a missing file, but it cannot create the database parent directory. */
    private static void createDatabaseDirectory() {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create the local SQLite data directory.", exception);
        }
    }
}
