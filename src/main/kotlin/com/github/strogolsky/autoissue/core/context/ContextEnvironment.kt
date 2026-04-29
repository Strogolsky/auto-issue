package com.github.strogolsky.autoissue.core.context

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

data class ContextEnvironment(
    val project: Project,
    val pointer: SmartPsiElementPointer<out PsiElement>? = null,
)
