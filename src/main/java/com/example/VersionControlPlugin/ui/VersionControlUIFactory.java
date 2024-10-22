package com.example.VersionControlPlugin.ui;

import com.example.VersionControlPlugin.VersionManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class VersionControlUIFactory implements ToolWindowFactory {

    private static VersionControlUI versionControlUI = null;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            if (VersionManager.getInstance().changeMap == null) {
                VersionManager.getInstance().init(project);
            }
        } catch (Exception e) {
        }
        if (versionControlUI == null) {
            versionControlUI = new VersionControlUI(project);
        }
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(versionControlUI.getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);
        content.setDisposer(() -> versionControlUI = null);
    }

    public static VersionControlUI getVersionControlUI() {
        return versionControlUI;
    }
}
