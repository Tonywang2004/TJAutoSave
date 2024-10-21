package com.example.VersionControlPlugin.activities

import com.example.VersionControlPlugin.VersionManager
import com.example.VersionControlPlugin.listener.CloseCacheListener
import com.example.VersionControlPlugin.listener.FileListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.messages.MessageBusConnection
import java.awt.event.ActionEvent
import java.io.IOException
import javax.swing.SwingUtilities
import javax.swing.Timer

class StartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val closeCacheListener = CloseCacheListener()
        val connection: MessageBusConnection = project.messageBus.connect()
        connection.subscribe(ProjectManager.TOPIC, closeCacheListener)
        VirtualFileManager.getInstance().addVirtualFileListener(FileListener())
        try {
            VersionManager.getInstance().init(project)
            Timer(100) { e: ActionEvent? ->
                SwingUtilities.invokeLater {
                    try {
                        VersionManager.getInstance().saveChanges()
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                    }
                }
            }.start()
        } catch (e: IOException) {
            println("Error initializing version control : " + e.message)
        }
    }
}