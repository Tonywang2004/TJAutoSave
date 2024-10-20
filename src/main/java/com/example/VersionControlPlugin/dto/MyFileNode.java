package com.example.VersionControlPlugin.dto;

import java.nio.file.Path;
import java.util.Map;

public class MyFileNode {
    public Map.Entry<Path, FileStatus> entry;
    public Integer version;

    public MyFileNode(Map.Entry<Path, FileStatus> entry, Integer version) {
        this.entry = entry;
        this.version = version;
    }

    @Override
    public String toString() {
        Path key = entry.getKey();
        FileStatus value = entry.getValue();
        //处理每一个键值对
        String fileName = key.getFileName().toString();
        String status = value.getStatus();
        String entry_time = value.getTimestamp();
        //String filePath = key.toString();
        //修改记录编码
        //String fileCode = value.getFilenum();
        return status + "-" + fileName + "-" + entry_time;  // 控制节点的显示内容
    }
}
