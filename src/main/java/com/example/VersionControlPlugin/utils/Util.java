package com.example.VersionControlPlugin.utils;

import com.example.VersionControlPlugin.objects.FileStatus;
import com.example.VersionControlPlugin.VersionManager;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;

public class Util {
    public static void readPaths(Path directoryPath,Path des){
        if (Files.isDirectory(directoryPath)) {
            StringBuilder paths = new StringBuilder();
            collectFilePaths(directoryPath, paths);
            writeToFile(paths.toString(), des);
        } else {
            System.out.println("Provided path is not a directory.");
        }
    }

    private static void collectFilePaths(Path dir, StringBuilder paths) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    if(Util.isInProjectDir(entry)) {
                        collectFilePaths(entry, paths); // Recursive call for directories
                    }
                } else {
                    paths.append(entry.toAbsolutePath()).append(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            System.out.println("Error when collecting file paths.");
        }
    }

    private static void writeToFile(String content,Path filePath) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write(content);
        } catch (IOException e) {
            System.out.println("Error when writing file paths.");
        }
    }

    public static String getLastPartBeforeSpace(String line) {
        int SpaceIndex = line.indexOf(' ');
        return SpaceIndex == -1 ? line.trim() : line.substring(0, SpaceIndex).trim();
    }

    public static boolean isInProjectDir(Path file){
        Path src = VersionManager.getInstance().projectBasePath.resolve("src");
        File dir=src.toFile();
        try {
            return file.toFile().getCanonicalPath().startsWith(dir.getCanonicalPath());
        } catch (IOException e) {
            System.out.println("Error when check path: " + e.getMessage());
            return false;
        }
    }

    public static void deleteDirectory(Path directoryPath) throws IOException {
        Files.walkFileTree(directoryPath, new java.nio.file.SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    public static void writeHashMapToFile(HashMap<Path, FileStatus> map, Path filePath) {
        for(HashMap.Entry<Path, FileStatus> entry : map.entrySet()){
            FileStatus status = entry.getValue();
            try {
                Files.writeString(filePath,entry.getKey()+"$"+status.getStatus()+"@"+status.getTimestamp()+"#"+status.getHashCode()+"\n",StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static HashMap<Path, FileStatus> readHashMapFromFile(Path filePath) {
        HashMap<Path, FileStatus> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toString()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String path = line.substring(0, line.indexOf('$')).trim();
                String status = line.substring(line.indexOf('$') + 1, line.indexOf('@')).trim();
                String timestamp = line.substring(line.indexOf('@') + 1, line.indexOf('#')).trim();
                String hashCode = line.substring(line.indexOf('#') + 1).trim();
                map.put(Paths.get(path),new FileStatus(status,timestamp,hashCode));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }
}
