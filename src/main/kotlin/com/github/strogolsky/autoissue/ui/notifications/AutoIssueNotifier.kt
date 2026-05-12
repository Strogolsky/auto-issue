package com.github.strogolsky.autoissue.ui.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

/**
 * Manages user-facing notifications for the AutoIssue plugin.
 *
 * Provides notification methods for:
 * - Configuration errors (with "Open Settings" action)
 * - Success/failure results
 * - General information messages
 *
 * All notifications are grouped under "AutoIssue Notifications" for
 * easy filtering in the IDE's notification center.
 */
object AutoIssueNotifier {
    private const val GROUP_ID = "AutoIssue Notifications"
    private const val TITLE = "AutoIssue"

    /**
     * Shows an error notification with an "Open Settings" action.
     *
     * Used when configuration is missing or invalid. Clicking the action
     * opens the specified settings dialog.
     *
     * @param project Current project (can be null for app-level notifications)
     * @param content Error message to display
     * @param configurableId ID of the settings configurable to open
     */
    fun notifyMissingConfig(
        project: Project?,
        content: String,
        configurableId: String,
    ) {
        val action =
            NotificationAction.createSimple("Open Settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, configurableId)
            }
        Notification(GROUP_ID, TITLE, content, NotificationType.ERROR)
            .addAction(action)
            .notify(project)
    }

    /**
     * Shows a notification with the specified type.
     *
     * @param project Current project (can be null for app-level notifications)
     * @param content Message to display
     * @param type Notification type (ERROR, WARNING, INFORMATION)
     */
    fun notify(
        project: Project?,
        content: String,
        type: NotificationType,
    ) {
        Notification(GROUP_ID, TITLE, content, type).notify(project)
    }
}
