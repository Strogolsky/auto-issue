package com.github.strogolsky.autoissue.ui.components

import com.intellij.ide.DataManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.ui.dsl.builder.panel
import java.awt.event.ActionEvent
import javax.swing.JComponent

/**
 * Parent/root configurable for the AutoIssue plugin in IDE Settings.
 *
 * Acts as a navigation hub to the plugin's sub-configurables:
 * - JiraSettingsConfigurable: JIRA connection and project settings
 * - LlmSettingsConfigurable: LLM provider and strategy selection
 *
 * Has no settings of its own; simply provides clickable links to navigate
 * to the detailed configuration panels.
 *
 * Appears as "AutoIssue" in the IDE Settings tree under Plugins.
 */
class AutoIssueMainConfigurable : Configurable {
    override fun getDisplayName() = "AutoIssue"

    /**
     * Creates a simple panel with navigation links to sub-configurables.
     * @return JComponent containing the link panel
     */
    override fun createComponent(): JComponent =
        panel {
            row {
                label("Main configuration for the AutoIssue plugin.")
            }

            row {
                link("Jira") { event ->
                    navigateTo(event, "com.github.strogolsky.autoissue.Jira")
                }
            }
            row {
                link("LLM") { event ->
                    navigateTo(event, "com.github.strogolsky.autoissue.LLM")
                }
            }
        }

    /**
     * Navigates to a child configurable by ID when a link is clicked.
     * Uses IDE's Settings framework to find and select the target configurable.
     *
     * @param event The action event from the link click
     * @param configurableId The configurable ID to navigate to (e.g., "com.github.strogolsky.autoissue.Jira")
     */
    private fun navigateTo(
        event: ActionEvent,
        configurableId: String,
    ) {
        val component = event.source as? JComponent ?: return

        val dataContext = DataManager.getInstance().getDataContext(component)
        val settings = Settings.KEY.getData(dataContext)

        if (settings != null) {
            val configurable = settings.find(configurableId)
            if (configurable != null) {
                settings.select(configurable)
            }
        }
    }

    /** This configurable has no settings, so isModified always returns false */
    override fun isModified() = false

    /** No-op; this configurable has no settings to apply */
    override fun apply() {}

    /** No-op; this configurable has no settings to reset */
    override fun reset() {}
}
