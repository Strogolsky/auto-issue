package com.github.strogolsky.autoissue.ui.components

import com.intellij.ide.DataManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import java.awt.event.ActionEvent
import javax.swing.JComponent

class AutoIssueMainConfigurable(private val project: Project) : Configurable {
    override fun getDisplayName() = "AutoIssue"

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

    override fun isModified() = false

    override fun apply() {}

    override fun reset() {}
}
