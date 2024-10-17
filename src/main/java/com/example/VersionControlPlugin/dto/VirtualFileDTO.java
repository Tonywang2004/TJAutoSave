package com.example.VersionControlPlugin.dto;

import com.example.VersionControlPlugin.enums.ChangeTypeEnum;

public class VirtualFileDTO {
    public String url;
    public long lastModified;
    public byte[] content;
    public ChangeTypeEnum changeType;

    public VirtualFileDTO(String url, long lastModified, byte[] content, ChangeTypeEnum changeType) {
        this.url = url;
        this.lastModified = lastModified;
        this.content = content;
        this.changeType = changeType;
    }
}