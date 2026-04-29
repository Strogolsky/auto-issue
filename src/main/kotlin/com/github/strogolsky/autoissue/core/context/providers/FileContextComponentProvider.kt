package com.github.strogolsky.autoissue.core.context.providers

import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.components.ContextComponent
import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import com.github.strogolsky.autoissue.integration.code.CodeAnalysisService
import com.intellij.openapi.components.service

class FileContextComponentProvider : ContextComponentProvider {
    override suspend fun provide(env: ContextEnvironment): ContextComponent? {
        val psiService = env.project.service<CodeAnalysisService>()

        val detailedData = psiService.extractDetailedContext(env.pointer) ?: return null
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
