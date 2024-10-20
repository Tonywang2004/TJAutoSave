package com.example.VersionControlPlugin.dto;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Changes implements Serializable {
    public enum Status {
        CHANGE, CREATE, DELETE, NONE
    }

    public static class Change implements Serializable {
        public int line;
        public String content;

        public Change(int line, String content) {
            this.line = line;
            this.content = content;
        }
    }

    public Status status;

    public List<Change> deleteLines = new ArrayList<>();
    public List<Change> addLines = new ArrayList<>();
    public String filePath;
    public String fileName;


    //生成变更记录
    public Changes(Path oldFile, Path newFile) throws IOException {
        if (oldFile == null) {
            if (Files.notExists(newFile))
                throw new IOException("文件路径有误");
            status = Status.CREATE;
            filePath = newFile.toString();
            fileName = newFile.getFileName().toString();
            deleteLines = null;
            List<String> newlines = Files.readAllLines(newFile);
            for (int i = 0; i < newlines.size(); i++) {
                addLines.add(new Change(i, newlines.get(i)));
            }
        } else if (newFile == null || Files.notExists(newFile)) {
            status = Status.DELETE;
            filePath = oldFile.toString();
            fileName = oldFile.getFileName().toString();
            addLines = null;
            List<String> newlines = Files.readAllLines(oldFile);
            for (int i = 0; i < newlines.size(); i++) {
                deleteLines.add(new Change(i, newlines.get(i)));
            }
        } else {
            status = Status.CHANGE;
            filePath = newFile.toString();
            fileName = newFile.getFileName().toString();
            List<String> oldlines = Files.readAllLines(oldFile);
            List<String> newlines = Files.readAllLines(newFile);
            int p = 0;
            for (int i = 0; i < newlines.size(); i++) {
                boolean found = false;
                for (int j = p; j < oldlines.size(); j++) {
                    if (newlines.get(i).equals(oldlines.get(j))) {
                        for (; p < j; p++) {
                            deleteLines.add(new Change(p, oldlines.get(p)));
                        }
                        p = j + 1;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    addLines.add(new Change(i, newlines.get(i)));
                }
            }
            while (p < oldlines.size()) {
                deleteLines.add(new Change(p, oldlines.get(p)));
                p++;
            }
        }
        if (addLines.isEmpty() && deleteLines.isEmpty()) {
            status = Status.NONE;
        }
    }

    public void saveToFile(String filePath) throws IOException {
        new ObjectOutputStream(new FileOutputStream(filePath)).writeObject(this);
    }

    public static Changes loadFromFile(String filePath) throws IOException, ClassNotFoundException {
        return (Changes) new ObjectInputStream(new FileInputStream(filePath)).readObject();
    }

    //revert changes of a file
    public static void rollBack(Changes changes, List<String> fileContent) throws IOException {
        for (int i = changes.addLines.size() - 1; i >= 0; i--) {
            fileContent.remove(changes.addLines.get(i).line);
        }
        for (Change i : changes.deleteLines) {
            fileContent.add(i.line, i.content);
        }
    }
}