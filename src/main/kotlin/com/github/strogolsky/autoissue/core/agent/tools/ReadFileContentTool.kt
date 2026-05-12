package com.github.strogolsky.autoissue.core.agent.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
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
@LLMDescription(
    "Reads and displays the complete source code content of any file in the project. " +
        "This is the primary tool for examining implementation details, understanding class structure, " +
        "and analyzing code context. Use this after locating a file via class/file listing tools.",
)
class ReadFileContentTool(private val project: Project) : AgentTool {
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
        "Reads and returns the complete source code of a file. " +
            "Input: exact project-relative path (e.g., 'src/main/kotlin/com/app/Foo.kt'). " +
            "Output: full file content with syntax structure visible. " +
            "Limitations: cannot read binaries (jars, images); large files are truncated. " +
            "Sensitive data (API keys, tokens) is automatically masked. " +
            "Usage tips: " +
            "1. Read a class file to see its imports, methods, and fields " +
            "2. Examine related files in the same directory to understand context " +
            "3. Read test files to understand expected behavior " +
            "4. Inspect configuration classes to understand how features are configured",
    )
    fun readFileContent(
        @LLMDescription("Exact project-relative path from listAllClasses or listProjectFiles. Example: 'src/main/kotlin/com/github/strogolsky/autoissue/core/agent/tools/ListAllClassesTool.kt'")
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
