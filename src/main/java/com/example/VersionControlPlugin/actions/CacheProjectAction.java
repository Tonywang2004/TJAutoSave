package com.example.VersionControlPlugin.actions;

import com.example.VersionControlPlugin.VersionManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class CacheProjectAction extends AnAction {
    public void actionPerformed(AnActionEvent e){
        Project project = e.getProject();
        VersionManager.getInstance().cacheProject(project);
        Notification notification = new Notification(
                "ProjectCache",
                "TJAutoSave",
                "Progress saved!.",
                NotificationType.INFORMATION
        );

        Notifications.Bus.notify(notification, project);
    }

}
