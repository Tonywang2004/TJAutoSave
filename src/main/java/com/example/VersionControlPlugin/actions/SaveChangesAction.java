package com.example.VersionControlPlugin.actions;

import com.example.VersionControlPlugin.VersionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class SaveChangesAction extends AnAction {
    public void actionPerformed(AnActionEvent e){
        Project project = e.getProject();
        VersionManager.getInstance().saveChanges(project);
    }

}
