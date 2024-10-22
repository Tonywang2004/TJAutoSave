package com.example.VersionControlPlugin.utils;

import com.example.VersionControlPlugin.objects.FileStatus;
import com.example.VersionControlPlugin.VersionManager;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;

public class Util {

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
        });//delete directory recursively
    }

    public static void writeHashMapToFile(HashMap<Path, FileStatus> map, Path filePath) {
        for (HashMap.Entry<Path, FileStatus> entry : map.entrySet()) {
            FileStatus status = entry.getValue();
            try {
                Files.writeString(filePath, entry.getKey()
                        + "$" + status.getStatus()
                        + "@" + status.getTimestamp()
                        + "#" + status.getHashCode() + "\n", StandardOpenOption.APPEND);
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
                map.put(Paths.get(path), new FileStatus(status, timestamp, hashCode));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }

    public static String readLastLine(File file) throws IOException {
        // 读取文件的最后一行
        StringBuilder sb = new StringBuilder();
        try (RandomAccessFile fileReader = new RandomAccessFile(file, "r")) {
            long fileLength = file.length();
            if (fileLength == 0) {
                return ""; // 如果文件为空，直接返回空字符串
            }
            // 从文件末尾开始向前读取
            for (long pointer = fileLength - 1; pointer >= 0; pointer--) {
                fileReader.seek(pointer);
                char c = (char) fileReader.read();
                if (c == '\n' && pointer != fileLength - 1) {
                    break; // 遇到换行符，停止读取
                }
                sb.append(c);
            }
            return sb.reverse().toString(); // 反转结果，因为是从末尾读取的
        }
    }
}
