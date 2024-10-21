package com.example.VersionControlPlugin.listener

import com.example.VersionControlPlugin.VersionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

class CloseCacheListener : ProjectManagerListener {
    override fun projectClosing(project: Project) {
        VersionManager.getInstance().projectCloseSave()
    }
}