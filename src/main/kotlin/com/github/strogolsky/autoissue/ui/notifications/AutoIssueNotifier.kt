package com.github.strogolsky.autoissue.ui.notifications

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project

object AutoIssueNotifier {
    private const val GROUP_ID = "AutoIssue Notifications"
    private const val TITLE = "AutoIssue"

    fun notifyMissingConfig(
        project: Project?,
        content: String,
        configurableId: String,
    ) {
        val action =
            NotificationAction.createSimple("Open Settings") {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, configurableId)
            }
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(TITLE, content, NotificationType.ERROR)
            .addAction(action)
            .notify(project)
    }

    fun notify(
        project: Project?,
        content: String,
        type: NotificationType,
    ) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(TITLE, content, type)
            .notify(project)
    }
}
