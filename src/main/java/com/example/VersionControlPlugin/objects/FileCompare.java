package com.example.VersionControlPlugin.objects;

import java.util.List;

public class FileCompare {
    public List<String> before;
    public List<String> after;

    public FileCompare(List<String> before, List<String> after) {
        this.before = before;
        this.after = after;
    }

}
