package com.example.VersionControlPlugin.activities

import com.example.VersionControlPlugin.VersionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class StartupCacheActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        VersionManager.getInstance().saveProject(project)
        return
    }
}