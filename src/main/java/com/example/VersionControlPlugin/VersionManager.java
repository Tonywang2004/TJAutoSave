package com.example.VersionControlPlugin;

import com.example.VersionControlPlugin.dto.VirtualFileDTO;
import com.example.VersionControlPlugin.enums.changeTypeEnum;
import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import net.minidev.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public void cacheProject(Project project) {
        cachedFiles = getAllChildrenFiles(project);

        try {
            List<VirtualFileDTO> dtos = new ArrayList<>();
            for (VirtualFile file : cachedFiles) {
                VirtualFileDTO dto = new VirtualFileDTO();
                dto.setName(file.getName());
                dto.setPath(file.getPath());
                dto.setLastModified(file.getTimeStamp());
                dtos.add(dto);
            }
            Path newCacheFilePath = Paths.get(project.getBasePath() + "TJAutoSave");
            File newCacheFile = new File(project.getBasePath()+ "TJAutoSave\\initCacheProject.json");
            if (newCacheFile.exists()) { // 文件存在
                newCacheFile.delete();
            }
            Files.createDirectories(newCacheFilePath);
            Gson gson = new Gson();
            String json = gson.toJson(dtos);
            Files.writeString(newCacheFile.toPath(), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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