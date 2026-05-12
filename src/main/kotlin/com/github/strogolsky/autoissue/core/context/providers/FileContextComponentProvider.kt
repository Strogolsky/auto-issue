package com.github.strogolsky.autoissue.core.context.providers

import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service

/**
 * Context provider that extracts source code information from the file at the cursor position.
 *
 * Uses CodeAnalysisService to read the PSI tree and extract:
 * - File name and language
 * - Imports
 * - Enclosing class (name, fields)
 * - Enclosing method (signature, body)
 * - Surrounding code context
 *
 * This information helps the AI understand the code context in which a TODO comment or issue
 * has been created, allowing it to generate more contextually accurate issues.
 */
class FileContextComponentProvider : ContextComponentProvider {
    /**
     * Extracts detailed source code context from the file at the given pointer location.
     *
     * @param env The context environment containing the project and PSI element pointer
     * @return FileContextComponent with extracted code information, or null if extraction fails
     */
    override suspend fun provide(env: ContextEnvironment): ContextComponent? {
        val psiService = env.project.service<CodeAnalysisService>()

        val detailedData = psiService.extractDetailedFileInfo(env.pointer) ?: return null
        return FileContextComponent(
            fileName = detailedData.fileName,
            language = detailedData.language,
            imports = detailedData.imports,
            className = detailedData.enclosingClass?.name,
            classFields = detailedData.enclosingClass?.fields ?: emptyList(),
            methodSignature = detailedData.enclosingMethod?.signature,
            methodBody = detailedData.enclosingMethod?.body ?: detailedData.surroundingText,
        )
    }
}
