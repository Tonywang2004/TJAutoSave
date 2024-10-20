package com.example.VersionControlPlugin.dto;

public class FileStatus {
    private String status;
    private String timestamp;
    final private String hashCode;

    public FileStatus(String status, String timestamp, String hashCode) {
        this.status = status;
        this.timestamp = timestamp;
        this.hashCode = hashCode;
    }

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getHashCode() {
        return hashCode;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
