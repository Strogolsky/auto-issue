package com.github.strogolsky.autoissue.core.context.providers

import com.github.strogolsky.autoissue.core.context.ContextEnvironment
import com.github.strogolsky.autoissue.core.context.components.ContextComponent

/**
 * Interface for contributing context information to the AI agent.
 *
 * Implementations are discovered via the extension point system and registered
 * in the ContextRegistry. Each provider can gather specific information about
 * the current code context (file, project, issues, etc.) and return it as
 * a ContextComponent.
 *
 * Providers are called during issue generation to build up the complete context
 * that will be sent to the LLM. Multiple providers contribute different aspects
 * of the context (file source, JIRA metadata, etc.).
 *
 * Implementations:
 * - FileContextComponentProvider: Extracts source code context (imports, methods, fields)
 * - JiraMetadataProvider: Fetches JIRA project metadata (issue types, priorities, assignees)
 */
interface ContextComponentProvider {
    /**
     * Provides context information based on the environment (project, file, cursor position).
     *
     * Called during issue generation to gather specific information. Should return null
     * if the provider cannot provide context for the given environment (e.g., no file open).
     *
     * @param env The context environment containing project, PSI element pointer, etc.
     * @return A ContextComponent with gathered information, or null if not applicable
     */
    suspend fun provide(env: ContextEnvironment): ContextComponent?
}
