package sh.bab.plugin.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class BabNotificationService(private val project: Project) {

    companion object {
        private const val NOTIFICATION_GROUP_ID = "Bab Notifications"
    }

    private val notificationGroup by lazy {
        NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
    }

    fun notifyError(title: String, content: String) {
        notificationGroup
            .createNotification(title, content, NotificationType.ERROR)
            .notify(project)
    }
}
