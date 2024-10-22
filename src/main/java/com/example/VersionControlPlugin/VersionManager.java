package com.example.VersionControlPlugin;

import com.example.VersionControlPlugin.objects.Changes;
import com.example.VersionControlPlugin.objects.FileStatus;
import com.example.VersionControlPlugin.utils.Util;
import com.example.VersionControlPlugin.objects.FileCompare;
import com.intellij.openapi.project.Project;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VersionManager {
    private static VersionManager versionManager;

    public static final String versionSavePath = ".TJAutoSave";
    public static final String tempPath = "temp";
    public static final String versionPathPrefix = "Version";
    private final String verInfoPath = "verInfo.";
    private final String projectDirPath = "projectDir.";
    private final String changePath = "change.";
    private final String mapTempPath = "mapTemp.";
    public Path projectBasePath;
    public int version = 0;
    public HashMap<Path, FileStatus> changeMap;

    public static VersionManager getInstance() {
        if (versionManager == null) {
            versionManager = new VersionManager();
        }
        return versionManager;
    }

    public void init(Project project) throws IOException {
        Path projectBasePath = Paths.get(project.getBasePath());
        VersionManager.getInstance().projectBasePath = projectBasePath;

        Path versionPath = projectBasePath.resolve(versionSavePath);
        if (!Files.exists(versionPath)) {
            Files.createDirectory(versionPath);
        }

        Path tempDir = versionPath.resolve(tempPath);
        if (!Files.exists(tempDir)) {
            Files.createDirectory(tempDir);
        }

        Path versionInfoPath = versionPath.resolve(verInfoPath);
        if (!Files.exists(versionInfoPath)) {
            Files.createFile(versionInfoPath);
            String formattedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            version = 0;
            Files.writeString(versionInfoPath, version + " " + formattedDateTime + "\n", StandardOpenOption.APPEND);
            Path ver = versionPath.resolve(versionPathPrefix + version);
            if (!Files.exists(ver)) {
                Files.createDirectory(ver);
                Util.readPaths(projectBasePath, ver.resolve(projectDirPath));
            }
        } else {
            getCurrentVersion(versionInfoPath, versionManager);
        }
        Path MapTemp = tempDir.resolve(mapTempPath);
        changeMap = Files.exists(MapTemp) ? Util.readHashMapFromFile(MapTemp) : new HashMap<>();
    }
    public static String readLastLine(File file) throws IOException {
        // 读取文件的最后一行
        RandomAccessFile fileReader = null;
        try {
            fileReader = new RandomAccessFile(file, "r");
            long fileLength = file.length() - 1;
            StringBuilder sb = new StringBuilder();

            // 设置初始位置为文件末尾
            fileReader.seek(fileLength);

            for (long pointer = fileLength; pointer >= 0; pointer--) {
                fileReader.seek(pointer);
                char c = (char) fileReader.read();
                if (c == '\n' && pointer != fileLength) {
                    break;
                }
                sb.append(c);
            }

            return sb.reverse().toString(); // 反转结果，因为是从末尾读取的
        } finally {
            if (fileReader != null) {
                fileReader.close();
            }
        }
    }
    private static void getCurrentVersion(Path versionInfo, VersionManager versionController) {
        String filePath = versionInfo.toString();
        File file = new File(filePath);
        try {
            String lastLine = readLastLine(file);
            System.out.println(lastLine);
            String result = Util.getLastPartBeforeSpace(lastLine);
            versionController.version = Integer.parseInt(result);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    public boolean saveChanges() throws IOException {
        if (changeMap.isEmpty()) {
            return false;
        }
        Path codeVersion = projectBasePath.resolve(versionSavePath);
        Path temp = codeVersion.resolve(tempPath);
        Path currentVersionPath = codeVersion.resolve(versionPathPrefix + (++version));
        if (!Files.exists(currentVersionPath)) {
            Files.createDirectories(currentVersionPath);
        }
        Util.readPaths(projectBasePath, currentVersionPath.resolve(projectDirPath));

        for (HashMap.Entry<Path, FileStatus> entry : changeMap.entrySet()) {
            Path after = entry.getKey();
            FileStatus status = entry.getValue();
            Path changeFile = currentVersionPath.resolve(status.getHashCode());
            if (status.getStatus().equals("CREATE")) {
                if (!Files.exists(changeFile)) {
                    Files.copy(after, changeFile);
                }
            } else if (status.getStatus().equals("DELETE")) {
                Path before = temp.resolve(status.getHashCode());
                if (!Files.exists(changeFile)) {
                    Files.copy(before, changeFile);
                }
            } else {
                Path before = temp.resolve(status.getHashCode());
                Changes changes = new Changes(before, after);
                if (changes.status != Changes.Status.NONE) {
                    changes.saveToFile(changeFile.toString());
                } else {
                    changeMap.remove(after);
                }
            }
        }

        Path change = currentVersionPath.resolve(changePath);
        if (!Files.exists(change)) {
            Files.createFile(change);
        }
        Util.writeHashMapToFile(changeMap, change);

        if (changeMap.isEmpty()) {
            return false;
        }
        changeMap.clear();


        Path versionInfo = codeVersion.resolve(verInfoPath);
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        Files.writeString(versionInfo, version + " " + formattedDateTime + "\n", StandardOpenOption.APPEND);


        Util.deleteDirectory(temp);
        Files.createDirectory(temp);

        return true;
    }

    public List<Map<String, String>> getProjectVersionInfo() throws IOException {
        Path codeVersion = projectBasePath.resolve(versionSavePath);
        Path currentVerDir = codeVersion.resolve(verInfoPath);
        List<String> lines = Files.readAllLines(currentVerDir);
        lines.remove(0);
        List<Map<String, String>> resultList = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split(" ");
            Map<String, String> map = new HashMap<>();
            map.put("version", parts[0]);
            map.put("time", parts[1] + " " + parts[2]);
            resultList.add(map);
        }
        return resultList;
    }

    public HashMap<Path, FileStatus> getChangeDirOfDesVersion(String desVersion) {
        return Util.readHashMapFromFile(projectBasePath.resolve(versionSavePath).resolve(desVersion).resolve(changePath));
    }

    public FileCompare getFileOfCertainVersion(Path filepath, int desVersion) {
        try {
            Path codeVersion = projectBasePath.resolve(versionSavePath);
            Path temp = codeVersion.resolve(tempPath);

            Path desVer = codeVersion.resolve(versionPathPrefix + desVersion);
            HashMap<Path, FileStatus> map = Util.readHashMapFromFile(desVer.resolve(changePath));
            List<String> before = null;
            List<String> after = null;

            if (map.get(filepath).getStatus().equals("CREATE")) {
                before = new ArrayList<>();
                Path changeDetails = desVer.resolve(map.get(filepath).getHashCode());
                after = Files.readAllLines(changeDetails);
                return new FileCompare(before, after);
            } else if (map.get(filepath).getStatus().equals("DELETE")) {
                after = new ArrayList<>();
                Path changeDetails = desVer.resolve(map.get(filepath).getHashCode());
                before = Files.readAllLines(changeDetails);
                return new FileCompare(before, after);
            }

            map = versionManager.changeMap;
            FileStatus status = map.get(filepath);
            Path file = status == null ? filepath : temp.resolve(status.getHashCode());
            int currentVer = version;

            List<String> fileContent = null;
            if (Files.exists(file)) {
                fileContent = Files.readAllLines(file);
            }
            if (currentVer == desVersion) {
                if (fileContent == null) {
                    after = new ArrayList<>();
                } else {
                    after = new ArrayList<>(fileContent);
                }
            }

            while (currentVer >= desVersion) {
                Path version = codeVersion.resolve(versionPathPrefix + currentVer);
                Path changeList = version.resolve(changePath);
                map = Util.readHashMapFromFile(changeList);
                status = map.get(filepath);
                if (status == null) {
                    currentVer--;
                    continue;
                }
                if (status.getStatus().equals("DELETE")) {
                    fileContent = Files.readAllLines(version.resolve(status.getHashCode()));
                } else if (status.getStatus().equals("CREATE")) {
                    if (fileContent != null) {
                        fileContent.clear();
                    } else {
                        fileContent = new ArrayList<>();
                    }
                } else {
                    Changes changes = Changes.loadFromFile(version.resolve(status.getHashCode()).toString());
                    Changes.rollBack(changes, fileContent);
                }
                currentVer--;
                if (fileContent != null) {
                    if (currentVer == desVersion) {
                        after = new ArrayList<>(fileContent);
                    } else if (currentVer == desVersion - 1) {
                        before = new ArrayList<>(fileContent);
                    }
                }
            }
            return new FileCompare(before, after);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void projectCloseSave() {
        try {
            Path codeVersion = projectBasePath.resolve(versionSavePath).resolve(tempPath).resolve(mapTempPath);
            if (!changeMap.isEmpty()) {
                if (!Files.exists(codeVersion)) {
                    Files.createFile(codeVersion);
                }
                Util.writeHashMapToFile(changeMap, codeVersion);// save changeMap to MapTempFile
            } else {
                Files.deleteIfExists(codeVersion);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}