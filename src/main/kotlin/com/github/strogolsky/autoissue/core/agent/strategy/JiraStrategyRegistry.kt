package com.github.strogolsky.autoissue.core.agent.strategy

import com.github.strogolsky.autoissue.core.input.IssueGenerationInput
import com.github.strogolsky.autoissue.core.output.JiraIssueCandidate
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName

@Service(Service.Level.APP)
class JiraStrategyRegistry {
    companion object {
        val EP_NAME: ExtensionPointName<IssueStrategyFactory<*, *>> =
            ExtensionPointName.create("com.github.strogolsky.autoissue.issueStrategyFactory")
    }

    @Suppress("UNCHECKED_CAST")
    fun strategiesFor(providerKey: String): List<IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>> =
        EP_NAME.extensionList
            .filter { it.providerKey.equals(providerKey, ignoreCase = true) }
            .map { it as IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate> }

    fun findFactory(
        providerKey: String,
        strategyId: String,
    ): IssueStrategyFactory<IssueGenerationInput, JiraIssueCandidate>? =
        strategiesFor(providerKey).find { it.id == strategyId }
}
