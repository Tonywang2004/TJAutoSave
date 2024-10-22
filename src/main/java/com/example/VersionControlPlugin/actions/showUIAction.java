package com.example.VersionControlPlugin.actions;

import com.example.VersionControlPlugin.Config;
import com.example.VersionControlPlugin.ui.VersionControlUI;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

// Open the main window of the plugin
public class showUIAction extends AnAction {

    private VersionControlUI versionControlUI = null;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (versionControlUI == null) {
            versionControlUI = new VersionControlUI(e.getProject());
        }
        // versionControlUI.setVisible(true);
        // Config.versionControlUI = versionControlUI;
    }
}
