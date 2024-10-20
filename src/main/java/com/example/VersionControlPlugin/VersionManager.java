package com.example.VersionControlPlugin;

import com.example.VersionControlPlugin.dto.Changes;
import com.example.VersionControlPlugin.dto.FileStatus;
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
    public static final String versionSavePath = "TJAutoSave";
    public static final String tempPath = "temp";
    public static final String versionPathPrefix = "V";
    private static VersionManager versionController;
    public Path projectBasePath;
    public int version = 0;
    public HashMap<Path, FileStatus> changeMap;

    //获得当前的类
    public static VersionManager getInstance() {
        if (versionController == null) {
            versionController = new VersionManager();
        }
        return versionController;
    }

    //检测项目目录下是否有插件所需的文件夹，没有则创建
    public static void init(Project project) throws IOException {
        Path projectBasePath = Paths.get(project.getBasePath());
        versionController.projectBasePath = projectBasePath;

        Path versionPath = projectBasePath.resolve(versionSavePath);
        if (!Files.exists(versionPath)) {
            Files.createDirectory(versionPath);
        }

        Path tempDir = versionPath.resolve(tempPath);
        if (!Files.exists(tempDir)) {
            Files.createDirectory(tempDir);
        }

        Path versionInfoPath = versionPath.resolve("versionInfoPath.txt");
        if (!Files.exists(versionInfoPath)) {
            Files.createFile(versionInfoPath);
            String formattedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Files.writeString(versionInfoPath, versionController.version + " " + formattedDateTime + "\n", StandardOpenOption.APPEND);
            //创建V1.0文件夹
            Path ver = versionPath.resolve(versionPathPrefix + versionController.version);
            if (!Files.exists(ver)) {
                Files.createDirectory(ver);
                //获取当前版本目录
                Util.readPaths(versionController.projectBasePath.resolve("src"), ver.resolve("ProjectDir.txt"));
            }
        } else {
            getCurrentVersion(versionInfoPath, versionController);
        }
        //读取修改缓存
        Path MapTemp = tempDir.resolve("MapTemp.txt");
        if (!Files.exists(MapTemp)) {
            versionController.changeMap = new HashMap<>();
        } else {
            versionController.changeMap = Util.readHashMapFromFile(MapTemp);
        }
    }

    //获取最新版本信息
    private static void getCurrentVersion(Path versionInfo, VersionManager versionController) {
        String filePath = versionInfo.toString(); // 输入文件的路径

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String Line;
            String LastLine = null;
            while ((Line = br.readLine()) != null) {
                LastLine = Line;
            }
            // 处理最后一行
            if (LastLine != null) {
                String result = Util.getLastPartBeforeSpace(LastLine);
                System.out.println("当前版本: " + result);
                versionController.version = Integer.parseInt(result);
            } else {
                System.out.println("文件为空");
            }
        } catch (FileNotFoundException e) {
            System.err.println("文件未找到: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("读取文件时出错: " + e.getMessage());
        }
    }

    public boolean saveChanges() throws IOException {
        if (changeMap.isEmpty()) {
            return false;
        }
        //获取路径
        Path codeVersion = projectBasePath.resolve(versionSavePath);
        Path temp = codeVersion.resolve(tempPath);
        Path currentVersionPath = codeVersion.resolve(versionPathPrefix + (++version));
        if (!Files.exists(currentVersionPath)) {
            Files.createDirectories(currentVersionPath);
        }
        //获取最新版本目录
        Util.readPaths(versionController.projectBasePath.resolve("src"), currentVersionPath.resolve("ProjectDir.txt"));


        //记录改动
        for (HashMap.Entry<Path, FileStatus> entry : versionController.changeMap.entrySet()) {
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
                    versionController.changeMap.remove(after);
                }
            }
        }

        //将历史记录留档备份
        Path change = currentVersionPath.resolve("change.txt");
        if (!Files.exists(change)) {
            Files.createFile(change);
        }
        Util.writeHashMapToFile(versionController.changeMap, change);

        if (changeMap.isEmpty()) {
            return false;
        }
        versionController.changeMap.clear();

        //更新当前版本
        Path versionInfo = codeVersion.resolve("verInfo.txt");
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = currentDateTime.format(formatter);
        Files.writeString(versionInfo, versionController.version + " " + formattedDateTime + "\n", StandardOpenOption.APPEND);

        //重置temp
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

    //获取当前代码的版本信息
    public List<Map<String, String>> getProjectVersionInfo() throws IOException {
        Path codeVersion = projectBasePath.resolve(versionSavePath);
        //Path currentVer=codeVersion.resolve(versionPathPrefix+desVersion);
        Path currentVerDir = codeVersion.resolve("verInfo.txt");
        List<String> lines = Files.readAllLines(currentVerDir);
        lines.remove(0);//去除版本0
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

    //获取某一版本相较于上一版本的修改列表，即desVersion-1到desVersion的变化列表
    public HashMap<Path, FileStatus> getChangeDirOfDesVersion(String desVersion) {
        Path codeVersion = projectBasePath.resolve(versionSavePath);
        Path version = codeVersion.resolve(versionPathPrefix + desVersion);
        Path changeList = version.resolve("change.txt");
        return Util.readHashMapFromFile(changeList);
    }

    //获取当前缓存中的文件修改列表：当次打开项目进行的修改无法记录？
    public HashMap<Path, FileStatus> getChangeDirOfCurrentVersion() {
        return changeMap;
    }

    //获取某个文件某个版本的内容以及上一个版本的内容，desVersion-1和desVersion
    public FileCompare getFileOfCertainVersion(Path filepath, int desVersion) {
        try {
            Path codeVersion = projectBasePath.resolve(versionSavePath);
            Path temp = codeVersion.resolve(tempPath);
            //首先获取desVersion的文件变化
            Path desVer = codeVersion.resolve(versionPathPrefix + desVersion);
            HashMap<Path, FileStatus> map = Util.readHashMapFromFile(desVer.resolve("change.txt"));
            List<String> before = null;
            List<String> after = null;
            //如果本次是创建,直接获取创建的内容
            if (map.get(filepath).getStatus().equals("CREATE")) {
                before = new ArrayList<String>();
                Path changeDetails = desVer.resolve(map.get(filepath).getHashCode());
                after = Files.readAllLines(changeDetails);
                return new FileCompare(before, after);
            }//若本次是删除，直接得到删除前的内容
            else if (map.get(filepath).getStatus().equals("DELETE")) {
                after = new ArrayList<String>();
                Path changeDetails = desVer.resolve(map.get(filepath).getHashCode());
                before = Files.readAllLines(changeDetails);
                return new FileCompare(before, after);
            }
            //若是修改，首先获得该文件的最新版本
            map = versionController.changeMap;
            FileStatus status = map.get(filepath);
            Path file = status == null ? filepath : temp.resolve(status.getHashCode());
            int currentVer = version;
            //若当前不存在该文件，说明该文件已被删除，找到该文件的最后一个版本;
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
            //开始回溯
            while (currentVer >= desVersion) {
                Path version = codeVersion.resolve(versionPathPrefix + currentVer);
                Path changeList = version.resolve("change.txt");
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
                        fileContent = new ArrayList<String>();
                    }
                } else {
                    Changes changes = Changes.loadFromFile(version.resolve(status.getHashCode()).toString());
                    Changes.rollBack(changes, fileContent);
                }
                currentVer--;
                if (fileContent != null) {
                    if (currentVer == desVersion) {
                        after = new ArrayList<String>(fileContent);
                    } else if (currentVer == desVersion - 1) {
                        before = new ArrayList<String>(fileContent);
                    }
                }
            }
            return new FileCompare(before, after);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    //获取某个文件当前未push内容与最新版本的内容，获取V1.version和Current版本的文件内容
    public FileCompare getFileOfCurrentVersion(Path filepath) {
        try {
            List<String> after = null;
            List<String> before = null;
            Path codeVersion = projectBasePath.resolve(versionSavePath);
            Path temp = codeVersion.resolve(tempPath);
            FileStatus status = changeMap.get(filepath);
            if (status.getStatus().equals("CREATE")) {
                after = Files.readAllLines(filepath);
                return new FileCompare(new ArrayList<>(), after);
            } else if (status.getStatus().equals("DELETE")) {
                Path file = temp.resolve(status.getHashCode());
                before = Files.readAllLines(file);
                return new FileCompare(before, new ArrayList<>());
            } else {
                after = Files.readAllLines(filepath);
                Path file = temp.resolve(status.getHashCode());
                before = Files.readAllLines(file);
                return new FileCompare(before, after);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //获取某一版本相较于上一版本的修改列表，即desVersion-1到desVersion的变化列表
    public HashMap<Path, FileStatus> getChangeMapOfCertainVersion(int desVersion) {
        return Util.readHashMapFromFile(projectBasePath.resolve(versionSavePath)
                .resolve(versionPathPrefix + desVersion).resolve("change.txt"));
    }

    //获取某个版本的变化细节，即desVersion-1到desVersion的变化
    public Changes getChangeOfCertainVersion(Path filepath, int desVersion) {
        try {
            Path version = projectBasePath.resolve(versionSavePath).resolve(versionPathPrefix + desVersion);
            Path changeList = version.resolve("change.txt");
            HashMap<Path, FileStatus> map = Util.readHashMapFromFile(changeList);
            return Changes.loadFromFile(version.resolve(map.get(filepath).getHashCode()).toString());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    //获取当前缓存中的变化细节
    public Changes getChangeOfCurrentVersion(Path filepath) {
        try {
            FileStatus status = changeMap.get(filepath);
            Path temp = projectBasePath.resolve(versionSavePath).resolve(tempPath);
            if (status.getStatus().equals("CREATE")) {
                return new Changes(null, filepath);
            } else if (status.getStatus().equals("DELETE")) {
                Path before = temp.resolve(status.getHashCode());
                return new Changes(before, null);
            } else {
                Path before = temp.resolve(status.getHashCode());
                return new Changes(before, filepath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //项目关闭时保存map缓存到文件
    public void projectCloseSave() {
        try {
            //保存修改缓存
            Path codeVersion = projectBasePath.resolve(versionSavePath).resolve(tempPath).resolve("MapTemp.txt");
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