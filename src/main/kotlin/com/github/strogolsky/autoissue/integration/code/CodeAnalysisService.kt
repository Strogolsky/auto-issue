package com.github.strogolsky.autoissue.integration.code

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import kotlin.math.max
import kotlin.math.min

@Service(Service.Level.PROJECT)
class CodeAnalysisService(private val project: Project) {
    fun isBinaryFile(filePath: String): Boolean =
        ReadAction.compute<Boolean, Throwable> {
            val virtualFile =
                project.guessProjectDir()?.findFileByRelativePath(filePath)
                    ?: return@compute false
            virtualFile.fileType.isBinary
        }

    fun searchFilesByName(
        query: String,
        maxResults: Int = 10,
    ): List<String> =
        ReadAction.compute<List<String>, Throwable> {
            val projectDir = project.guessProjectDir()?.path ?: return@compute emptyList()
            val scope = GlobalSearchScope.projectScope(project)
            val allNames = FilenameIndex.getAllFilenames(project)
            val results = mutableListOf<String>()
            for (name in allNames) {
                if (!name.contains(query, ignoreCase = true)) continue
                for (vFile in FilenameIndex.getVirtualFilesByName(name, scope)) {
                    results.add(vFile.path.removePrefix("$projectDir/"))
                    if (results.size >= maxResults) return@compute results
                }
            }
            results
        }

    fun getWholeFileContent(
        filePath: String,
        maxChars: Int = 20_000,
    ): FileInfo? =
        ReadAction.compute<FileInfo?, Throwable> {
            val virtualFile =
                project.guessProjectDir()?.findFileByRelativePath(filePath)
                    ?: return@compute null

            val psiFile =
                PsiManager.getInstance(project).findFile(virtualFile)
                    ?: return@compute null

            val text = psiFile.text
            FileInfo(content = text.take(maxChars), truncated = text.length > maxChars, maxChars = maxChars)
        }

    fun extractDetailedContext(pointer: SmartPsiElementPointer<out PsiElement>?): DetailedFileInfo? {
        if (pointer == null) return null

        return ReadAction.compute<DetailedFileInfo?, Throwable> {
            val targetElement = pointer.element ?: return@compute null
            val file = targetElement.containingFile ?: return@compute null

            DetailedFileInfo(
                fileName = file.name,
                language = file.language.id,
                imports = extractImports(file),
                enclosingClass = extractClassInfo(targetElement),
                enclosingMethod = extractMethodInfo(targetElement),
                surroundingText = extractSurroundingLines(targetElement, lines = 5),
            )
        }
    }

    private fun extractImports(file: PsiElement): List<String> =
        buildList {
            PsiTreeUtil.processElements(file) { element ->
                if (element.javaClass.simpleName.contains("ImportStatement")) {
                    add(element.text.trim())
                }
                true
            }
        }

    private fun extractClassInfo(element: PsiElement): ClassInfo? {
        var current: PsiElement? = element

        while (current != null) {
            val typeName = current.javaClass.simpleName
            if ((typeName.contains("Class") || typeName.contains("Object")) && !typeName.contains("Reference")) {
                val className = (current as? PsiNamedElement)?.name ?: "UnknownClass"
                return ClassInfo(className, emptyList())
            }
            current = current.parent
        }
        return null
    }

    private fun extractMethodInfo(element: PsiElement): MethodInfo? {
        var current: PsiElement? = element

        while (current != null) {
            val typeName = current.javaClass.simpleName
            if (typeName.contains("Method") || typeName.contains("Function")) {
                val methodName = (current as? PsiNamedElement)?.name ?: "UnknownMethod"
                return MethodInfo(
                    name = methodName,
                    signature = methodName,
                    body = current.text,
                )
            }
            current = current.parent
        }
        return null
    }

    private fun extractSurroundingLines(
        element: PsiElement,
        lines: Int,
    ): String {
        val document = PsiDocumentManager.getInstance(project).getDocument(element.containingFile) ?: return element.text
        val lineNumber = document.getLineNumber(element.textRange.startOffset)

        val startLine = max(0, lineNumber - lines)
        val endLine = min(document.lineCount - 1, lineNumber + lines)

        return document.getText(
            TextRange(
                document.getLineStartOffset(startLine),
                document.getLineEndOffset(endLine),
            ),
        )
    }
}
