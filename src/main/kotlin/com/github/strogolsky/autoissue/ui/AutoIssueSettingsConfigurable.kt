package com.github.strogolsky.autoissue.ui

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent

class AutoIssueSettingsConfigurable : Configurable {
    override fun getDisplayName() = "AutoIssue"

    override fun createComponent(): JComponent? = null

    override fun isModified() = false

    override fun apply() {}
}
