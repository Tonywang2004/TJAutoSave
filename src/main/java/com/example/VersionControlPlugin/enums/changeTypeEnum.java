package com.example.VersionControlPlugin.enums;

public enum changeTypeEnum {
    Unchanged(""), Changed("Modified"), New("New"), Deleted("Deleted");

    private String displayInfo;

    changeTypeEnum(String displayInfo) {
        this.displayInfo = displayInfo;
    }

    public String getDisplayInfo(){
        return displayInfo;
    }
}
