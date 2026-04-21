package com.github.strogolsky.autoissue.config

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File
import java.util.Properties

object LocalPropertiesLoader {

    fun load(
        project: Project,
        fileName: String,
    ): Map<String, String> {
        val projectRoot =
            project.basePath
                ?: run {
                    logger.warn("AutoIssue: project.basePath is null, skipping $fileName")
                    return emptyMap()
                }

        val file = File("$projectRoot/$fileName")
        if (!file.exists()) {
            logger.warn("AutoIssue: $fileName not found at ${file.absolutePath}, skipping")
            return emptyMap()
        }

        return Properties()
            .apply { load(file.inputStream()) }
            .entries
            .associate { it.key.toString() to it.value.toString() }
    }

    private val logger = Logger.getInstance(LocalPropertiesLoader::class.java)
}
