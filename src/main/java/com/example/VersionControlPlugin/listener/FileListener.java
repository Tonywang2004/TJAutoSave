package com.example.VersionControlPlugin.listener;

import com.example.VersionControlPlugin.objects.FileStatus;
import com.example.VersionControlPlugin.utils.Util;
import com.example.VersionControlPlugin.VersionManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class FileListener implements VirtualFileListener {
    //监测文件创建
    @Override
    public void fileCreated(@NotNull VirtualFileEvent event) {
        storeChanges(event, "CREATE");
    }

    @Override
    public void beforeFileDeletion(@NotNull VirtualFileEvent event) {
        storeChanges(event, "DELETE");
    }

    @Override
    public void beforeContentsChange(@NotNull VirtualFileEvent event) {
        storeChanges(event, "CHANGE");
    }

    @Override
    public void contentsChanged(@NotNull VirtualFileEvent event) {
        VirtualFile virtualFile = event.getFile();
        if (!virtualFile.isDirectory() && Util.isInProjectDir(virtualFile.toNioPath())) {
            Path temp = VersionManager.getInstance().projectBasePath.resolve(VersionManager.versionSavePath).resolve(VersionManager.tempPath);
            Path newpath = temp.resolve(VersionManager.getInstance().changeMap.get(Paths.get(event.getFile().getPath())).getHashCode());
            try {
                if (Files.mismatch(Paths.get(virtualFile.getPath()), newpath) == -1L) {
                    VersionManager.getInstance().changeMap.remove(Paths.get(event.getFile().getPath()));
                    Files.delete(newpath);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void storeChanges(@NotNull VirtualFileEvent event, String type) {
        VirtualFile virtualFile = event.getFile();
        if (!virtualFile.isDirectory() && Util.isInProjectDir(virtualFile.toNioPath())) {
            Path filePath = Paths.get(virtualFile.getPath());
            System.out.println(type + ": " + filePath);
            HashMap<Path, FileStatus> map = VersionManager.getInstance().changeMap;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = LocalDateTime.now().format(formatter);

            if (!map.containsKey(filePath)) {
                FileStatus status = new FileStatus(type, formattedDateTime, String.valueOf(filePath.hashCode()));
                map.put(filePath, status);
            } else {
                String lastStatus = map.get(filePath).getStatus();
                if (lastStatus.equals("CREATE")) {
                    if (type.equals("DELETE")) {
                        map.remove(filePath);
                        return;
                    } else {
                        type = "CREATE";
                    }
                } else {
                    if (type.equals("CREATE")) {
                        type = "CHANGE";
                    }
                }
                map.get(filePath).setStatus(type);
                map.get(filePath).setTimestamp(formattedDateTime);
            }
            if (!type.equals("CREATE")) {
                map = VersionManager.getInstance().changeMap;
                String filename = map.get(Paths.get(event.getFile().getPath())).getHashCode();
                Path newpath = VersionManager.getInstance().projectBasePath.resolve(VersionManager.versionSavePath).resolve(VersionManager.tempPath).resolve(filename);
                if (!Files.exists(newpath)) {
                    try {
                        Files.createFile(newpath);
                        Files.write(newpath, event.getFile().contentsToByteArray(), StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }


}