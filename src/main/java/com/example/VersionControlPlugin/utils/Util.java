package com.example.VersionControlPlugin.utils;

import com.example.VersionControlPlugin.dto.FileStatus;
import com.example.VersionControlPlugin.VersionManager;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;

public class Util {
    //将文件全部转移
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

    public static String[] splitLine(String line) {
        // 查找第一个冒号
        int colonIndex = line.indexOf(':');
        if (colonIndex == -1) {
            System.err.println("没有找到冒号: " + line);
            return null;
        }

        // 获取第一个冒号前的部分
        String part1 = line.substring(0, colonIndex).trim();

        // 获取冒号后的内容
        String rest = line.substring(colonIndex + 1).trim();

        // 查找第一个空格
        int spaceIndex = rest.indexOf(' ');
        String part2, part3;

        if (spaceIndex == -1) {
            part2 = rest; // 如果没有空格，则整个部分都属于第二段
            part3 = "";   // 第三段为空
        } else {
            // 获取第一个空格前的部分和空格后剩余的部分
            part2 = rest.substring(0, spaceIndex).trim();
            part3 = rest.substring(spaceIndex + 1).trim();
        }

        return new String[]{part1, part2, part3};
    }

    public static String getLastPartBeforeSpace(String line) {
        // 查找最后一个空格的位置
        int SpaceIndex = line.indexOf(' ');

        // 如果没有空格，返回整行
        if (SpaceIndex == -1) {
            return line.trim();
        }

        // 获取最后一个空格前的内容
        return line.substring(0, SpaceIndex).trim();
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
        // 使用Files.walkFileTree遍历文件夹
        Files.walkFileTree(directoryPath, new java.nio.file.SimpleFileVisitor<>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                // 删除文件
                Files.delete(file);
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                // 删除目录
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
