package com.example.VersionControlPlugin;

import com.example.VersionControlPlugin.objects.Changes;
import com.example.VersionControlPlugin.objects.FileStatus;
import com.example.VersionControlPlugin.utils.Util;
import com.intellij.openapi.project.Project;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VersionManager {
    private static VersionManager versionManager;

    public static final String versionSavePath = ".TJAutoSave";
    public static final String tempPath = "temp";
    private static final String versionPathPrefix = "Version ";
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
        versionManager.projectBasePath = projectBasePath;

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
            Files.writeString(versionInfoPath, versionManager.version + " " + formattedDateTime + "\n", StandardOpenOption.APPEND);
            // V1.0
            Path ver = versionPath.resolve(versionPathPrefix + versionManager.version);
            if (!Files.exists(ver)) {
                Files.createDirectory(ver);
                Util.readPaths(versionManager.projectBasePath.resolve("src"), ver.resolve(projectDirPath));
            }
        } else {
            getCurrentVersion(versionInfoPath, versionManager);
        }
        Path MapTemp = tempDir.resolve(mapTempPath);
        if (!Files.exists(MapTemp)) {
            versionManager.changeMap = new HashMap<>();
        } else {
            versionManager.changeMap = Util.readHashMapFromFile(MapTemp);
        }
    }

    private static void getCurrentVersion(Path versionInfo, VersionManager versionController) {
        String filePath = versionInfo.toString();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String Line;
            String LastLine = null;
            while ((Line = br.readLine()) != null) {
                LastLine = Line;
            }
            if (LastLine != null) {
                String result = Util.getLastPartBeforeSpace(LastLine);
                versionController.version = Integer.parseInt(result);
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not Found: " + e.getMessage());
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
        Util.readPaths(versionManager.projectBasePath.resolve("src"), currentVersionPath.resolve(projectDirPath));


        for (HashMap.Entry<Path, FileStatus> entry : versionManager.changeMap.entrySet()) {
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
                    versionManager.changeMap.remove(after);
                }
            }
        }

        Path change = currentVersionPath.resolve(changePath);
        if (!Files.exists(change)) {
            Files.createFile(change);
        }
        Util.writeHashMapToFile(versionManager.changeMap, change);

        if (changeMap.isEmpty()) {
            return false;
        }
        versionManager.changeMap.clear();


        Path versionInfo = codeVersion.resolve(verInfoPath);
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        Files.writeString(versionInfo, versionManager.version + " " + formattedDateTime + "\n", StandardOpenOption.APPEND);


        Util.deleteDirectory(temp);
        Files.createDirectory(temp);

        return true;
    }

    public static class FileCompare {
        public List<String> before;
        public List<String> after;

        public FileCompare(List<String> before, List<String> after) {
            this.before = before;
            this.after = after;
        }
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
            }
            else if (map.get(filepath).getStatus().equals("DELETE")) {
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
                Util.writeHashMapToFile(changeMap, codeVersion);
            } else {
                Files.deleteIfExists(codeVersion);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}