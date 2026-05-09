package com.github.strogolsky.autoissue.integration.code

/**
 * File content with truncation metadata.
 *
 * Used when reading file contents to avoid overwhelming the AI model
 * with very large files.
 *
 * @param content The file content (truncated if larger than maxChars)
 * @param truncated Whether the file was truncated (true if file size > maxChars)
 * @param maxChars The maximum number of characters returned
 */
data class FileInfo(
    val content: String,
    val truncated: Boolean,
    val maxChars: Int,
)
