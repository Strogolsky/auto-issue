package com.github.strogolsky.autoissue.ui.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AutoIssueNotifierTest : BasePlatformTestCase() {

    private val capturedNotifications = mutableListOf<Notification>()

    override fun setUp() {
        super.setUp()
        capturedNotifications.clear()

        project.messageBus.connect(testRootDisposable).subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    capturedNotifications.add(notification)
                }
            }
        )
    }

    fun test_should_SendBasicNotification() {
        AutoIssueNotifier.notify(project, "Task successfully created", NotificationType.INFORMATION)

        assertEquals(1, capturedNotifications.size)

        val notification = capturedNotifications.first()

        assertEquals("AutoIssue Notifications", notification.groupId)
        assertEquals("AutoIssue", notification.title)
        assertEquals("Task successfully created", notification.content)
        assertEquals(NotificationType.INFORMATION, notification.type)
        assertTrue(notification.actions.isEmpty())
    }

    fun test_should_SendMissingConfigNotificationWithAction() {
        AutoIssueNotifier.notifyMissingConfig(project, "API Token is missing", "jira.settings.id")

        assertEquals(1, capturedNotifications.size)

        val notification = capturedNotifications.first()

        assertEquals("AutoIssue", notification.title)
        assertEquals("API Token is missing", notification.content)
        assertEquals(NotificationType.ERROR, notification.type)

        assertEquals(1, notification.actions.size)

        val action = notification.actions.first()
        assertEquals("Open Settings", action.templatePresentation.text)

    }
}