package com.github.strogolsky.autoissue.services

import com.github.strogolsky.autoissue.MyBundle
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {
    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    fun getRandomNumber() = (1..100).random()
}
