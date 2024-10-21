package com.example.VersionControlPlugin.actions;

import com.example.VersionControlPlugin.VersionManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class ClearCacheAction extends AnAction {
    public void actionPerformed(AnActionEvent e){
        VersionManager.getInstance().clearCache(e.getProject());
        Notifications.Bus.notify(new Notification("TJAutoSave", "TJAutoSave",
                "Cache cleared!", NotificationType.INFORMATION), e.getProject());
    }
}
