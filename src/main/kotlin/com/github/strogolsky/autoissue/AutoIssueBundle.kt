package com.github.strogolsky.autoissue

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.AutoIssueBundle"

/**
 * Loads and manages localized messages for the AutoIssue plugin.
 *
 * This bundle provides access to UI strings, notifications, and error messages
 * that are defined in the `messages/AutoIssueBundle.properties` resource file.
 * It supports parameterized messages for dynamic content.
 */
object AutoIssueBundle : DynamicBundle(BUNDLE) {
    /**
     * Retrieves a localized message by key with optional parameters.
     *
     * @param key The property key to look up in the bundle
     * @param params Optional parameters to substitute in the message
     * @return The localized message string
     */
    @JvmStatic
    fun message(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any,
    ) = getMessage(key, *params)

    /**
     * Returns a lazy-initialized message pointer for deferring message lookup.
     *
     * This is useful for UI components that need messages but aren't immediately rendered.
     *
     * @param key The property key to look up in the bundle
     * @param params Optional parameters to substitute in the message
     * @return A lazy pointer to the localized message
     */
    @Suppress("unused")
    @JvmStatic
    fun messagePointer(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any,
    ) = getLazyMessage(key, *params)
}
