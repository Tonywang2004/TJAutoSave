package com.example.VersionControlPlugin.actions;

import com.example.VersionControlPlugin.VersionManager;
import com.example.VersionControlPlugin.ui.VersionControlUI;
import com.example.VersionControlPlugin.ui.VersionControlUIFactory;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;

import java.io.IOException;

public class SaveChangesAction extends AnAction {
    public void actionPerformed(AnActionEvent e){
        Project project = e.getProject();
        try {
            FileDocumentManager.getInstance().saveAllDocuments();
            if (VersionManager.getInstance().saveChanges()) {
                Notifications.Bus.notify(new Notification("TJAutoSave", "TJAutoSave",
                        "Progress saved!", NotificationType.INFORMATION), project);

                // refresh UI
                VersionControlUI ui = VersionControlUIFactory.getVersionControlUI();
                if (ui != null){
                    ui.clearListPanel();
                    ui.getSavedVersion();
                }

            } else {
                Notifications.Bus.notify(new Notification("TJAutoSave", "TJAutoSave",
                        "No changes detected!", NotificationType.WARNING), project);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
