package com.example.VersionControlPlugin;

import com.example.VersionControlPlugin.objects.Changes;
import com.example.VersionControlPlugin.objects.FileStatus;
import com.example.VersionControlPlugin.utils.Util;
import com.example.VersionControlPlugin.objects.FileCompare;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VersionManager {
    private static VersionManager versionManager;

    public static final String tjAutoSavePath = ".TJAutoSave";
    public static final String tempPath = "temp";
    public static final String versionPathPrefix = "Version";
    private final String verInfoPath = "verInfo.";
    private final String changePath = "changeInfo.";
    private final String mapTempPath = "changeMapTemp.";
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
        projectBasePath = Paths.get(Objects.requireNonNull(project.getBasePath()));

        Path savePath = projectBasePath.resolve(tjAutoSavePath);
        if (!Files.exists(savePath)) {
            Files.createDirectory(savePath);
            String sets = "attrib +H \"" + savePath.toAbsolutePath() + "\"";
            Runtime.getRuntime().exec(sets);//设置文件夹为隐藏
        }

        Path tempDir = savePath.resolve(tempPath);
        if (!Files.exists(tempDir)) {
            Files.createDirectory(tempDir);
        }

        //read changeMap from temp
        changeMap = new HashMap<>();
        Path MapTemp = tempDir.resolve(mapTempPath);
        if (Files.exists(MapTemp)) {
            changeMap = Util.readHashMapFromFile(MapTemp);
        }

        Path versionInfo = savePath.resolve(verInfoPath);
        if (!Files.exists(versionInfo)) {
            Files.createFile(versionInfo);
            version = 0;
        } else {
            getCurrentVersion(versionInfo, versionManager);
        }
    }

    private static void getCurrentVersion(Path versionInfo, VersionManager versionManager) {
        File file = new File(versionInfo.toString());
        try {
            String lastLine = Util.readLastLine(file);//读取最后一行，即当前版本信息
            System.out.println(lastLine);
            int SpaceIndex = lastLine.indexOf(' ');
            versionManager.version = Integer.parseInt(SpaceIndex == -1 ? lastLine.trim() : lastLine.substring(0, SpaceIndex).trim());
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    public boolean saveChanges() throws IOException {
        if (changeMap.isEmpty()) {
            return false;
        }
        Path savePath = projectBasePath.resolve(tjAutoSavePath);
        Path temp = savePath.resolve(tempPath);
        Path currentVersionPath = savePath.resolve(versionPathPrefix + (++version));
        if (!Files.exists(currentVersionPath)) {
            Files.createDirectories(currentVersionPath);
        }

        for (HashMap.Entry<Path, FileStatus> entry : changeMap.entrySet()) {
            FileStatus status = entry.getValue();
            Path before = temp.resolve(status.getHashCode());
            Path after = entry.getKey();
            Path changeFile = currentVersionPath.resolve(status.getHashCode());
            if (status.getStatus().equals("CREATE")) {
                if (!Files.exists(changeFile)) {
                    Files.copy(after, changeFile);
                }
            } else if (status.getStatus().equals("DELETE")) {
                if (!Files.exists(changeFile)) {
                    Files.copy(before, changeFile);
                }
            } else {
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

        String formattedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Files.writeString(savePath.resolve(verInfoPath),
                version + " " + formattedDateTime + "\n", StandardOpenOption.APPEND);

        Util.deleteDirectory(temp);
        Files.createDirectory(temp);

        return true;
    }

    public List<Map<String, String>> getProjectVersionInfo() throws IOException {
        List<String> lines = Files.readAllLines(projectBasePath.resolve(tjAutoSavePath).resolve(verInfoPath));
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
        return Util.readHashMapFromFile(projectBasePath.resolve(tjAutoSavePath).resolve(desVersion).resolve(changePath));
    }

    public FileCompare getFileOfCertainVersion(Path filepath, int desVersion) {
        try {
            Path savePath = projectBasePath.resolve(tjAutoSavePath);
            Path desVerPath = savePath.resolve(versionPathPrefix + desVersion);
            FileStatus fileStatus = Util.readHashMapFromFile(desVerPath.resolve(changePath)).get(filepath);

            if (fileStatus.getStatus().equals("CREATE")) {//create
                Path changeDetails = desVerPath.resolve(fileStatus.getHashCode());
                return new FileCompare(new ArrayList<>(), Files.readAllLines(changeDetails));
            } else if (fileStatus.getStatus().equals("DELETE")) {//delete
                Path changeDetails = desVerPath.resolve(fileStatus.getHashCode());
                return new FileCompare(Files.readAllLines(changeDetails), new ArrayList<>());
            }

            //change
            List<String> before = null;
            List<String> after = null;
            FileStatus status = changeMap.get(filepath);
            Path file = status == null ? filepath : savePath.resolve(tempPath).resolve(status.getHashCode());

            List<String> fileContent = null;
            if (Files.exists(file)) {
                fileContent = Files.readAllLines(file);
            }
            if (version == desVersion) {
                if (fileContent == null) {
                    after = new ArrayList<>();
                } else {
                    after = new ArrayList<>(fileContent);
                }
            }

            for (int currentVer = version; currentVer >= desVersion; ) {
                Path version = savePath.resolve(versionPathPrefix + currentVer);
                status = Util.readHashMapFromFile(version.resolve(changePath)).get(filepath);
                if (status == null) {
                    currentVer--;
                    continue;
                }
                if (status.getStatus().equals("DELETE")) {
                    fileContent = Files.readAllLines(version.resolve(status.getHashCode()));
                } else if (status.getStatus().equals("CREATE")) {
                    fileContent = new ArrayList<>();
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
        try { // 关闭时把changeMap存到文件中
            Path mapTemp = projectBasePath.resolve(tjAutoSavePath).resolve(tempPath).resolve(mapTempPath);
            if (!changeMap.isEmpty()) {
                if (!Files.exists(mapTemp)) {
                    Files.createFile(mapTemp);
                }
                Util.writeHashMapToFile(changeMap, mapTemp);// save changeMap to MapTempFile
            } else {
                Files.deleteIfExists(mapTemp);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearCache(Project project) {
        SwingUtilities.invokeLater(() -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    Util.deleteDirectory(projectBasePath.resolve(tjAutoSavePath));
                    init(project);
                    Notifications.Bus.notify(new Notification("TJAutoSave", "TJAutoSave",
                            "Cache cleared!", NotificationType.INFORMATION), project);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        });
    }

    public boolean isInProjectDir(Path file) {
        return !file.toAbsolutePath().startsWith(projectBasePath.resolve(tjAutoSavePath).toAbsolutePath());
    }
}