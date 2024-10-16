package com.example.VersionControlPlugin;

import com.example.VersionControlPlugin.enums.changeTypeEnum;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import net.minidev.json.JSONArray;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;


public final class VersionManager {

    private static final VersionManager versionManager = new VersionManager();
    //将构造器设置为private禁止通过new进行实例化
    private VersionManager() {}

    public static VersionManager getInstance() {
        return versionManager;
    }

    public ArrayList<VirtualFile> cachedFiles = new ArrayList<>(); // 缓存文件路径及其最后修改时间

    public ArrayList<VirtualFile> getAllChildrenFiles(Project project) {
        ArrayList<VirtualFile> childrenFiles = new ArrayList<>();
        Stack<VirtualFile> stack = new Stack<>();
        for (VirtualFile file : project.getBaseDir().getChildren()) {
            stack.push(file);
        }
        while (!stack.isEmpty()) {
            VirtualFile file = stack.pop();
            if (!file.isDirectory()) {
                childrenFiles.add(file);
            }
            else {
                for (VirtualFile child : file.getChildren()) {
                    stack.push(child);
                }
            }
        }
        return childrenFiles;
    }

    public static String filePathJoin(String rootDir, String... subs) {

        if (subs == null || subs.length == 0) {
            return rootDir;
        }
        StringBuilder path = new StringBuilder(rootDir);
        for (String sub : subs) {
            String s = sub;
            //若子目录前面也带了斜杠则去掉
            if (s.startsWith("/")) {
                s = s.substring(1);
            }

            if (path.toString().endsWith(File.separator) || path.toString().endsWith("/")) {
                path.append(s);
            } else {
                path.append(File.separator).append(s);
            }
        }
        return path.toString();
    }

    public void cacheProject(Project project) {
        cachedFiles = getAllChildrenFiles(project);

        Path newCacheFile = Paths.get(filePathJoin(project.getBasePath(), "initCacheProject.json"));

        try {
            Path directoryPath = newCacheFile.getParent();
            if (directoryPath != null) {
                Files.createDirectories(directoryPath);
            }
            Files.writeString(newCacheFile, JSONArray.toJSONString(cachedFiles), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 追加写模式
//        Files.writeString(versionCacheFilePath, "Hello World", StandardOpenOption.APPEND);
//        System.out.println("Project Cached");
    }

    public HashMap<String, changeTypeEnum> checkUpdate(Project project) {
        HashMap<String, changeTypeEnum> changedFiles = new HashMap<>();
        ArrayList<VirtualFile> newFiles = getAllChildrenFiles(project);
        for (VirtualFile file : newFiles) {
            if (!cachedFiles.contains(file)){
                changedFiles.put(file.getUrl(), changeTypeEnum.New);
            }
        }
        for (VirtualFile cachedFile : cachedFiles) {
            String oldFileUrl = cachedFile.getUrl();
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(oldFileUrl);
            if (file != null && file.getModificationCount() > 0) {
                changedFiles.put(oldFileUrl, changeTypeEnum.Changed);
            }
            if (file == null){
                changedFiles.put(oldFileUrl, changeTypeEnum.Deleted);
            }
        }
        return changedFiles;
    }

    public void clearCache() {
        cachedFiles.clear();
    }
}