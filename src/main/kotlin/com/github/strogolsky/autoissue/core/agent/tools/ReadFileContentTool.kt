package com.github.strogolsky.autoissue.core.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.github.strogolsky.autoissue.core.context.render.PromptRenderService
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Tool for AI agent to read source file content.
 *
 * Provides file content to the AI for analysis. Content is automatically masked
 * to remove sensitive information (API keys, passwords, etc.) before being
 * sent to the AI.
 *
 * Large files are truncated to avoid overwhelming the model.
 */
@LLMDescription("Tools for reading source file content.")
class ReadFileContentTool(private val project: Project) : ToolSet {
    private val render = project.service<PromptRenderService>()
    private val codeAnalysisService = project.service<CodeAnalysisService>()

    /**
     * Reads the content of a source file.
     *
     * The file path must be project-relative (from the root directory).
     * Content is automatically masked to remove sensitive data.
     * If the file is too large, content is truncated.
     *
     * @param filePath Project-relative file path (e.g., 'src/main/kotlin/com/app/Foo.kt')
     * @return File content response, or error if file not found or is binary
     */
    @Tool
    @LLMDescription(
        "Reads the entire content of a source file. " +
            "Requires the exact project-relative path returned by searchFiles. " +
            "Cannot read binary files (images, jars, compiled artifacts).",
    )
    fun readFileContent(
        @LLMDescription("Project-relative path to the file as returned by searchFiles, e.g. 'src/main/kotlin/com/app/Foo.kt'.")
        filePath: String,
    ): ToolResponse {
        if (codeAnalysisService.isBinaryFile(filePath)) {
            return ToolErrorResponse(errorDetails = "Cannot read binary files.")
        }
        val fileInfo =
            codeAnalysisService.getWholeFileContent(filePath)
                ?: return ToolErrorResponse(errorDetails = "File not found at path: $filePath")
        val raw =
            if (fileInfo.truncated) {
                fileInfo.content + "\n\n... [CONTENT TRUNCATED DUE TO SIZE LIMIT: ${fileInfo.maxChars} CHARS] ..."
            } else {
                fileInfo.content
            }
        val maskedContent = render.mask(raw)
        return FileContentResponse(filePath = filePath, content = maskedContent)
    }
}
