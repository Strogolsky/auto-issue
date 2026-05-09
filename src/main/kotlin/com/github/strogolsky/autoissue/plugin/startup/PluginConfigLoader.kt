package com.github.strogolsky.autoissue.plugin.startup

import com.github.strogolsky.autoissue.core.masking.MaskingConfig
import com.github.strogolsky.autoissue.plugin.config.DevConfig
import com.github.strogolsky.autoissue.plugin.config.LlmDefaults
import com.github.strogolsky.autoissue.plugin.config.PluginConfig
import org.w3c.dom.Element
import java.io.File
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Loads plugin configuration from PluginConfig.xml resource file.
 *
 * Parses XML configuration containing:
 * - LLM defaults (provider, strategy, temperature, max iterations, system prompt)
 * - Prompt rendering format (XML, Markdown, Simple)
 * - Content masking settings (enabled/disabled)
 * - Development mode configuration
 *
 * The system prompt and examples can be inlined in XML or loaded from separate files.
 * Config values are used by AutoIssueSetupTool to initialize the plugin environment.
 */
object PluginConfigLoader {
    /**
     * Loads the plugin configuration from the default PluginConfig.xml resource.
     *
     * @return The loaded PluginConfig
     * @throws IllegalStateException If PluginConfig.xml is not found in resources
     */
    fun load(): PluginConfig =
        load(
            PluginConfigLoader::class.java.getResourceAsStream("/PluginConfig.xml")
                ?: error("PluginConfig.xml not found in resources"),
        )

    /**
     * Loads plugin configuration from an XML input stream.
     *
     * Parses XML elements for:
     * - LLM: default provider, strategy, temperature, max iterations, system prompt, examples
     * - Format: prompt rendering format (XML/Markdown/Simple)
     * - Local Properties: whether to load config from system properties
     * - Masking: whether content masking is enabled
     *
     * @param stream The XML input stream (PluginConfig.xml)
     * @return The parsed PluginConfig
     * @throws SAXException If XML is malformed
     * @throws FileNotFoundException If referenced files don't exist
     */
    fun load(stream: InputStream): PluginConfig {
        val doc =
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(stream)

        val llmNode = doc.getElementsByTagName("llm").item(0) as Element
        val llm =
            LlmDefaults(
                provider = llmNode.text("default-provider"),
                strategyId = llmNode.text("default-strategy"),
                temperature = llmNode.text("temperature").toDouble(),
                maxIterations = llmNode.text("max-iterations").toInt(),
                systemPrompt = resolveSystemPrompt(llmNode) + resolveExamples(llmNode),
            )

        val format = doc.getElementsByTagName("format").item(0).textContent.trim()

        val devNode = doc.getElementsByTagName("local-properties").item(0)
        val dev =
            if (devNode != null) {
                DevConfig(
                    localPropertiesEnabled = devNode.attributes.getNamedItem("enabled")?.textContent == "true",
                )
            } else {
                DevConfig(localPropertiesEnabled = false)
            }

        val maskingNode = doc.getElementsByTagName("masking").item(0) as? Element
        val masking =
            if (maskingNode != null) {
                val enabled =
                    maskingNode.getElementsByTagName("enabled").item(0)
                        ?.textContent?.trim()?.equals("true", ignoreCase = true) ?: true
                MaskingConfig(enabled = enabled)
            } else {
                MaskingConfig()
            }

        return PluginConfig(llm, format, dev, masking)
    }

    /**
     * Resolves the system prompt, either from inline XML or from a referenced file.
     *
     * The <system-prompt> element can have a "file" attribute pointing to an external file,
     * or contain the prompt text directly as element content.
     *
     * @param llmNode The <llm> element from the XML
     * @return The system prompt text, trimmed
     */
    private fun resolveSystemPrompt(llmNode: Element): String {
        val node =
            llmNode.getElementsByTagName("system-prompt").item(0) as? Element
                ?: error("Missing <system-prompt> in PluginConfig.xml")
        val filePath = node.getAttribute("file").trim()
        if (filePath.isNotEmpty()) {
            val stream =
                PluginConfigLoader::class.java.getResourceAsStream("/$filePath")
                    ?: File(filePath).takeIf { it.exists() }?.inputStream()
                    ?: error("system-prompt file not found: $filePath")
            return stream.bufferedReader().use { it.readText() }.trim()
        }
        return node.textContent.trim()
    }

    /**
     * Resolves example prompts/outputs, either from a referenced file or returns empty string.
     *
     * The <examples> element (optional) can reference an external file that contains
     * example prompts or structured outputs for the LLM.
     *
     * @param llmNode The <llm> element from the XML
     * @return The examples text prefixed with separator, or empty string if not provided
     */
    private fun resolveExamples(llmNode: Element): String {
        val node = llmNode.getElementsByTagName("examples").item(0) as? Element ?: return ""
        val filePath = node.getAttribute("file").trim()
        if (filePath.isEmpty()) return ""
        val stream =
            PluginConfigLoader::class.java.getResourceAsStream("/$filePath")
                ?: File(filePath).takeIf { it.exists() }?.inputStream()
                ?: error("examples file not found: $filePath")
        val content = stream.bufferedReader().use { it.readText() }.trim()
        return if (content.isEmpty()) "" else "\n\n---\n\n$content"
    }

    /** Helper to extract text content from a child element by tag name */
    private fun Element.text(tag: String): String = getElementsByTagName(tag).item(0).textContent.trim()
}
