package com.example.VersionControlPlugin.objects;

import java.nio.file.Path;
import java.util.Map;

public class FileNode {
    public Map.Entry<Path, FileStatus> entry;
    public Integer version;

    public FileNode(Map.Entry<Path, FileStatus> entry, Integer version) {
        this.entry = entry;
        this.version = version;
    }

    @Override
    public String toString() {
        return entry.getValue().getStatus()
                + "-" + entry.getKey().getFileName().toString()
                + "-" + entry.getValue().getTimestamp();
    }
}
