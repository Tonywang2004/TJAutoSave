package com.example.VersionControlPlugin;

import com.example.VersionControlPlugin.dto.VirtualFileDTO;
import com.example.VersionControlPlugin.enums.ChangeTypeEnum;
import com.google.gson.Gson;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;


public final class VersionManager {

    private static final VersionManager versionManager = new VersionManager();

    //将构造器设置为private禁止通过new进行实例化
    private VersionManager() {
    }

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
            } else {
                for (VirtualFile child : file.getChildren()) {
                    stack.push(child);
                }
            }
        }
        return childrenFiles;
    }

    private void saveFilesToJson(Project project, List<VirtualFileDTO> dtos, String jsonFileName){
        String basePath = project.getBasePath() + File.separator + "TJAutoSave";
        File baseCachePath = new File(basePath);
        File newCacheFile = new File(basePath + File.separator + jsonFileName);
        if (!baseCachePath.exists()) {
            baseCachePath.mkdirs();
        }
        if (!newCacheFile.exists()) {
            try {
                newCacheFile.createNewFile();
                String json = new Gson().toJson(dtos);
                Files.writeString(newCacheFile.toPath(), json, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void saveProject(Project project){
        cachedFiles.clear();
        cachedFiles = getAllChildrenFiles(project);
        List<VirtualFileDTO> dtos = new ArrayList<>();
        try {
            for (VirtualFile file : cachedFiles) {
                dtos.add(new VirtualFileDTO(file.getUrl(), file.getTimeStamp(), file.contentsToByteArray(), ChangeTypeEnum.New));
            }
            saveFilesToJson(project, dtos, "InitialProject.json");
            cachedFiles = getAllChildrenFiles(project);
            Notification notification = new Notification(
                    "TJAutoSave",
                    "TJAutoSave",
                    "Project saved!",
                    NotificationType.INFORMATION
            );
            Notifications.Bus.notify(notification, project);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveChanges(Project project){
        List<VirtualFileDTO> changedFiles = new ArrayList<>();
        try {
            changedFiles = getChangedFiles(project);
        } catch (IOException e) {}
        saveFilesToJson(project, changedFiles, "Version" + new SimpleDateFormat("yyyyMMddhhmmss").format(new Date()) + ".json");
        cachedFiles = getAllChildrenFiles(project);
        Notification notification = new Notification(
                "TJAutoSave",
                "TJAutoSave",
                "Progress saved!",
                NotificationType.INFORMATION
        );
        Notifications.Bus.notify(notification, project);
    }

    public List<VirtualFileDTO> getChangedFiles(Project project) throws IOException {
        List<VirtualFileDTO> changedFiles = new ArrayList<>();
        for (VirtualFile newFile : getAllChildrenFiles(project)) {
            if (!cachedFiles.contains(newFile)) {
                changedFiles.add(new VirtualFileDTO(newFile.getUrl(), newFile.getTimeStamp(), newFile.contentsToByteArray(), ChangeTypeEnum.New));
            }
        }
        for (VirtualFile cachedFile : cachedFiles) {
            String oldFileUrl = cachedFile.getUrl();
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(oldFileUrl);
            if (file != null && file.getModificationCount() > 0) {
                changedFiles.add(new VirtualFileDTO(file.getUrl(), file.getTimeStamp(), file.contentsToByteArray(), ChangeTypeEnum.Changed));
            }
            if (file == null) {
                changedFiles.add(new VirtualFileDTO(cachedFile.getUrl(), cachedFile.getTimeStamp(), cachedFile.contentsToByteArray(), ChangeTypeEnum.Deleted));
            }
        }
        return changedFiles;
    }

    public void clearCache() {
        cachedFiles.clear();
    }
}