package com.github.strogolsky.autoissue.services

import com.github.strogolsky.autoissue.ClassContext
import com.github.strogolsky.autoissue.DetailedFileContext
import com.github.strogolsky.autoissue.MethodContext
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil

@Service(Service.Level.PROJECT)
class CodeAnalysisService(private val project: Project) {

    fun extractDetailedContext(pointer: SmartPsiElementPointer<out PsiElement>?): DetailedFileContext? {
        if (pointer == null) return null

        return ReadAction.compute<DetailedFileContext?, Throwable> {
            val targetElement = pointer.element ?: return@compute null
            val file = targetElement.containingFile ?: return@compute null

            DetailedFileContext(
                fileName = file.name,
                language = file.language.id,
                imports = extractImports(file),
                enclosingClass = extractClassContext(targetElement),
                enclosingMethod = extractMethodContext(targetElement),
                surroundingText = extractSurroundingLines(targetElement, lines = 5)
            )
        }
    }

    private fun extractImports(file: PsiElement): List<String> {
        val imports = mutableListOf<String>()
        PsiTreeUtil.processElements(file) { element ->
            val typeName = element.javaClass.simpleName
            if (typeName.contains("ImportList") || typeName.contains("ImportDirective")) {
                imports.add(element.text)
            }
            true
        }
        return imports
    }

    private fun extractClassContext(element: PsiElement): ClassContext? {
        var current: PsiElement? = element

        while (current != null) {
            val typeName = current.javaClass.simpleName
            if ((typeName.contains("Class") || typeName.contains("Object")) && !typeName.contains("Reference")) {

                val className = (current as? PsiNamedElement)?.name ?: "UnknownClass"
                return ClassContext(className, emptyList())
            }
            current = current.parent
        }
        return null
    }

    private fun extractMethodContext(element: PsiElement): MethodContext? {
        var current: PsiElement? = element

        while (current != null) {
            val typeName = current.javaClass.simpleName
            if (typeName.contains("Method") || typeName.contains("Function")) {
                val methodName = (current as? PsiNamedElement)?.name ?: "UnknownMethod"
                return MethodContext(
                    name = methodName,
                    signature = methodName,
                    body = current.text
                )
            }
            current = current.parent
        }
        return null
    }

    private fun extractSurroundingLines(element: PsiElement, lines: Int): String {
        val document = PsiDocumentManager.getInstance(project).getDocument(element.containingFile) ?: return element.text
        val lineNumber = document.getLineNumber(element.textRange.startOffset)

        val startLine = kotlin.math.max(0, lineNumber - lines)
        val endLine = kotlin.math.min(document.lineCount - 1, lineNumber + lines)

        return document.getText(
            TextRange(
                document.getLineStartOffset(startLine),
                document.getLineEndOffset(endLine)
            )
        )
    }
}