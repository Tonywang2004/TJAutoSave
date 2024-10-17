package com.example.VersionControlPlugin.enums;

public enum ChangeTypeEnum {
    Unchanged(""), Changed("Modified"), New("New"), Deleted("Deleted");

    private String displayInfo;

    ChangeTypeEnum(String displayInfo) {
        this.displayInfo = displayInfo;
    }

    public String getDisplayInfo(){
        return displayInfo;
    }
}
