package com.github.strogolsky.autoissue.core.context

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

/**
 * Context information passed to context component providers.
 *
 * Provides:
 * - The project being worked on
 * - Optional pointer to the specific code element (TODO location, selected code, etc.)
 *
 * Context providers use this to gather information about the project and the
 * specific location where the issue should be created.
 *
 * @param project The IntelliJ project
 * @param pointer Optional smart pointer to a code element (preserves validity across refactoring)
 */
data class ContextEnvironment(
    val project: Project,
    val pointer: SmartPsiElementPointer<out PsiElement>? = null,
)
