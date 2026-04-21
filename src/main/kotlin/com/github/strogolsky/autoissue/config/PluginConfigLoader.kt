package com.github.strogolsky.autoissue.config

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
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
                systemPrompt = llmNode.text("system-prompt"),
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
                    localPropertiesFile = devNode.textContent.trim(),
                )
            } else {
                DevConfig(localPropertiesEnabled = false, localPropertiesFile = "local.properties")
            }

        return PluginConfig(llm, format, providers, dev)
    }

    private fun Element.text(tag: String): String = getElementsByTagName(tag).item(0).textContent.trim()

    private fun NodeList.toList(): List<Node> = (0 until length).map { item(it) }
}
