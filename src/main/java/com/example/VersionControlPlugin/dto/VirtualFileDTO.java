package com.example.VersionControlPlugin.dto;

public class VirtualFileDTO {
    private String url;
    private long lastModified;
    private byte[] content;

    public VirtualFileDTO(String url, long lastModified, byte[] content) {
        this.url = url;
        this.lastModified = lastModified;
        this.content = content;
    }
}