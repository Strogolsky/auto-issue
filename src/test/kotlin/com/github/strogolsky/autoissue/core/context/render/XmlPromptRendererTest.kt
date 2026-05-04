package com.github.strogolsky.autoissue.core.context.render

import com.github.strogolsky.autoissue.core.context.components.FileContextComponent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XmlPromptRendererTest {

    private val renderer = XmlPromptRenderer()

    @Test // UC-R5
    fun should_WrapCodeInCdataAndUseAttributes_When_RenderingFileContextToXml() {
        // --- TEST FLOW ---
        // 1. ARRANGE: Create context with code containing XML-like characters.
        val fileContext = FileContextComponent(
            fileName = "XmlParser.kt",
            language = "Kotlin",
            imports = emptyList(),
            className = "XmlParser",
            classFields = emptyList(),
            methodSignature = "fun parse()",
            methodBody = "fun parse() { println(\"<test>\") }"
        )

        // 2. ACT: Build XML prompt.
        val result = renderer.buildPrompt {
            components(listOf(fileContext))
        }

        // 3. ASSERT: Verify XML tags, attributes, and CDATA wrapping.
        assertTrue("Should contain root tag", result.contains("<prompt>"))
        assertTrue("Should contain file attributes", result.contains("<file name=\"XmlParser.kt\" language=\"Kotlin\">"))
        assertTrue("Should contain class tag", result.contains("<class name=\"XmlParser\""))
        assertTrue("Should wrap code in CDATA", result.contains("<![CDATA[fun parse() { println(\"<test>\") }]]>"))
    }

    @Test // UC-R6
    fun should_OmitContextTag_When_ComponentListIsEmptyInXml() {
        // --- TEST FLOW ---
        // 1. ARRANGE: No components.
        // 2. ACT: Build prompt.
        val result = renderer.buildPrompt {
            components(emptyList())
            instruction("Do something")
        }

        // 3. ASSERT: Verify context block is skipped entirely.
        assertFalse("Should not contain empty context tag", result.contains("<context>"))
        assertTrue("Should contain instructions tag", result.contains("<instructions>"))
    }
}