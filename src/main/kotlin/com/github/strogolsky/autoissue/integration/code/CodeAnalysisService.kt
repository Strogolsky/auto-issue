package com.github.strogolsky.autoissue.integration.code

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.toUElementOfType
import kotlin.math.max
import kotlin.math.min

/**
 * Service for analyzing and extracting information from project source code.
 *
 * Provides methods to:
 * - Search files and symbols in the project
 * - Extract class and method information
 * - Read file contents and surrounding context
 * - List all classes and source files
 *
 * All operations run in read-only mode using ReadAction to ensure thread safety.
 */
@Service(Service.Level.PROJECT)
class CodeAnalysisService(private val project: Project) {
    /**
     * Checks if a file is a binary file (non-text).
     *
     * @param filePath Relative path to the file from project root
     * @return true if file is binary, false if text or not found
     */
    fun isBinaryFile(filePath: String): Boolean =
        ReadAction.compute<Boolean, Throwable> {
            val virtualFile =
                project.guessProjectDir()?.findFileByRelativePath(filePath)
                    ?: return@compute false
            virtualFile.fileType.isBinary
        }

    /**
     * Searches for files by name pattern.
     *
     * @param query The filename or pattern to search for (case-insensitive)
     * @param maxResults Maximum number of results to return (default 10)
     * @return List of relative file paths matching the query
     */
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

    /**
     * Lists all classes in the project across JVM languages (Java, Kotlin, Groovy, Scala).
     *
     * Maps class names to their file paths for quick lookup.
     * Only includes top-level classes, excludes inner/anonymous classes.
     *
     * @return Map of class name → relative file path
     */
    fun listAllClasses(): Map<String, String> =
        ReadAction.compute<Map<String, String>, Throwable> {
            val projectDirPath = project.guessProjectDir()?.path ?: return@compute emptyMap()
            val scope = GlobalSearchScope.projectScope(project)
            val result = mutableMapOf<String, String>()
            val jvmExtensions = setOf("java", "kt", "groovy", "scala")
            for (name in FilenameIndex.getAllFilenames(project)) {
                val ext = name.substringAfterLast('.', "")
                if (ext !in jvmExtensions) continue
                for (vFile in FilenameIndex.getVirtualFilesByName(name, scope)) {
                    val psiFile = PsiManager.getInstance(project).findFile(vFile) ?: continue
                    val uFile = psiFile.toUElementOfType<UFile>() ?: continue
                    val relativePath = vFile.relativePathIn(projectDirPath)
                    uFile.classes.forEach { uClass ->
                        uClass.name?.let { result[it] = relativePath }
                    }
                }
            }
            result
        }

    /**
     * Lists all source files in the project with specified extensions.
     *
     * Excludes common build/generated directories: build, generated, .gradle, out
     *
     * @param extensions File extensions to include (default: ["java"])
     * @return Sorted list of relative file paths
     */
    fun listAllSourceFiles(extensions: List<String> = listOf("java", "kt")): List<String> =
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
                    val relativePath = vFile.relativePathIn(projectDirPath)
                    if (relativePath.split("/").any { it in excludedDirs }) continue
                    results.add(relativePath)
                }
            }
            results.sorted()
        }

    /**
     * Reads the entire content of a file.
     *
     * Content is truncated to maxChars to avoid overwhelming the AI model.
     * The truncated flag indicates if the file was larger than maxChars.
     *
     * @param filePath Relative path to the file from project root
     * @param maxChars Maximum characters to return (default 20,000)
     * @return FileInfo with content and truncation metadata, or null if file not found
     */
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

    /**
     * Extracts detailed context information around a code element.
     *
     * Gathers:
     * - File name and language
     * - Import statements
     * - Enclosing class and method
     * - Surrounding code lines (context)
     *
     * This is used to provide rich context to the AI agent about where
     * the TODO or issue was found in the code.
     *
     * @param pointer Smart pointer to the target code element
     * @return DetailedFileInfo with all context, or null if pointer is null
     */
    fun extractDetailedFileInfo(pointer: SmartPsiElementPointer<out PsiElement>?): DetailedFileInfo? {
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

    /**
     * Extracts all import statements from a file using UAST.
     *
     * @param file The PSI file element
     * @return List of import statement strings
     */
    private fun extractImports(file: PsiElement): List<String> {
        val uFile = file.toUElementOfType<UFile>() ?: return emptyList()
        return uFile.imports.map { it.asSourceString() }
    }

    /**
     * Finds the enclosing class of a code element using UAST.
     *
     * @param element The element to find the class for
     * @return ClassInfo with class name and fields, or null if not in a class
     */
    private fun extractClassInfo(element: PsiElement): ClassInfo? {
        val uElement = element.toUElement() ?: return null
        val uClass = uElement.getParentOfType<UClass>() ?: return null
        return ClassInfo(name = uClass.name ?: "UnknownClass", fields = uClass.fields.mapNotNull { it.name })
    }

    /**
     * Finds the enclosing method of a code element using UAST.
     *
     * @param element The element to find the method for
     * @return MethodInfo with method name and body, or null if not in a method
     */
    private fun extractMethodInfo(element: PsiElement): MethodInfo? {
        val uElement = element.toUElement() ?: return null
        val uMethod = uElement.getParentOfType<UMethod>() ?: return null
        return MethodInfo(
            name = uMethod.name,
            signature = uMethod.name,
            body = uMethod.sourcePsi?.text ?: uMethod.asSourceString(),
        )
    }

    /**
     * Extracts source code lines surrounding the target element.
     *
     * Returns `lines` lines before and after the element for context.
     * Used to give the AI model code context without including the entire file.
     *
     * @param element The target element
     * @param lines Number of lines to include before and after (default 5)
     * @return String containing surrounding code lines
     */
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

    private fun VirtualFile.relativePathIn(projectDirPath: String): String = path.removePrefix("$projectDirPath/")
}
