package com.example.VersionControlPlugin.actions;

import com.example.VersionControlPlugin.Config;
import com.example.VersionControlPlugin.ui.VersionControlUI;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

// Open the main window of the plugin
public class VersionControl extends AnAction {

    private VersionControlUI versionControlUI = new VersionControlUI();

    @Override
    public void actionPerformed(AnActionEvent e) {
        versionControlUI.setVisible(true);
        Config.versionControlUI = versionControlUI;
    }
}
