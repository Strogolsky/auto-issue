package com.github.strogolsky.autoissue.integration.code

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
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

    fun listAllClasses(): Map<String, String> =
        ReadAction.compute<Map<String, String>, Throwable> {
            val projectDir = project.guessProjectDir()?.path ?: return@compute emptyMap()
            val scope = GlobalSearchScope.projectScope(project)
            val result = mutableMapOf<String, String>()
            for (name in FilenameIndex.getAllFilenames(project)) {
                if (!name.endsWith(".java")) continue
                for (vFile in FilenameIndex.getVirtualFilesByName(name, scope)) {
                    val psiFile = PsiManager.getInstance(project).findFile(vFile) ?: continue
                    val relativePath = vFile.path.removePrefix("$projectDir/")
                    PsiTreeUtil.findChildrenOfType(psiFile, PsiClass::class.java).forEach { psiClass ->
                        psiClass.name?.let { result[it] = relativePath }
                    }
                }
            }
            result
        }

    fun searchSymbol(query: String): List<String> =
        ReadAction.compute<List<String>, Throwable> {
            val projectDir = project.guessProjectDir()?.path ?: return@compute emptyList()
            val scope = GlobalSearchScope.projectScope(project)
            val cache = PsiShortNamesCache.getInstance(project)
            val results = mutableListOf<String>()
            for (className in cache.getAllClassNames()) {
                if (!className.contains(query, ignoreCase = true)) continue
                for (psiClass in cache.getClassesByName(className, scope)) {
                    val vFile = psiClass.containingFile?.virtualFile ?: continue
                    val relativePath = vFile.path.removePrefix("$projectDir/")
                    results.add("${psiClass.name} → $relativePath")
                }
            }
            results
        }

    fun listAllSourceFiles(extensions: List<String> = listOf("java")): List<String> =
        ReadAction.compute<List<String>, Throwable> {
            val projectDir = project.guessProjectDir() ?: return@compute emptyList()
            val projectDirPath = projectDir.path
            val scope = GlobalSearchScope.projectScope(project)
            val excludedDirs = setOf("build", "generated", ".gradle", "out")
            val results = mutableListOf<String>()
            for (name in FilenameIndex.getAllFilenames(project)) {
                val ext = name.substringAfterLast('.', "")
                if (ext !in extensions) continue
                for (vFile in FilenameIndex.getVirtualFilesByName(name, scope)) {
                    val relativePath = vFile.path.removePrefix("$projectDirPath/")
                    if (relativePath.split("/").any { it in excludedDirs }) continue
                    results.add(relativePath)
                }
            }
            results.sorted()
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
