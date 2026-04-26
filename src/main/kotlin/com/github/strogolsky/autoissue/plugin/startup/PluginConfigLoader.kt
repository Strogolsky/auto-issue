package com.github.strogolsky.autoissue.plugin.startup

import com.github.strogolsky.autoissue.plugin.config.DevConfig
import com.github.strogolsky.autoissue.plugin.config.LlmDefaults
import com.github.strogolsky.autoissue.plugin.config.PluginConfig
import com.github.strogolsky.autoissue.plugin.config.RenderingFormat
import com.github.strogolsky.autoissue.core.masking.MaskingConfig
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object PluginConfigLoader {
    fun load(): PluginConfig {
        val stream =
            PluginConfigLoader::class.java.getResourceAsStream("/PluginConfig.xml")
                ?: error("PluginConfig.xml not found in resources")

        val doc =
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(stream)

        val llmNode = doc.getElementsByTagName("llm").item(0) as Element
        val llm =
            LlmDefaults(
                provider = llmNode.text("default-provider"),
                modelName = llmNode.text("default-model"),
                strategyId = llmNode.text("default-strategy"),
                temperature = llmNode.text("temperature").toDouble(),
                maxIterations = llmNode.text("max-iterations").toInt(),
                systemPrompt = resolveSystemPrompt(llmNode) + resolveExamples(llmNode),
            )

        val format =
            RenderingFormat.valueOf(
                doc.getElementsByTagName("format").item(0).textContent.trim(),
            )

        val providers =
            doc.getElementsByTagName("provider")
                .toList()
                .filter { it.attributes.getNamedItem("enabled")?.textContent == "true" }
                .map { it.textContent.trim() }

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

        return PluginConfig(llm, format, providers, dev, masking)
    }

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

    private fun Element.text(tag: String): String = getElementsByTagName(tag).item(0).textContent.trim()

    private fun NodeList.toList(): List<Node> = (0 until length).map { item(it) }
}