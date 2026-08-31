package com.r2r.readytorevise

import android.app.Application
import com.r2r.readytorevise.di.AppContainer
import com.r2r.readytorevise.di.DefaultAppContainer
import com.r2r.readytorevise.notification.BootReceiver
import com.r2r.readytorevise.notification.NotificationHelper
import com.r2r.readytorevise.notification.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReadyToReviseApplication : Application() {
    lateinit var container: AppContainer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        NotificationHelper.createNotificationChannel(this)

        val prefs = NotificationPreferences(this)
        applicationScope.launch {
            val enabled = prefs.notificationsEnabled.first()
            if (enabled) {
                val hour = prefs.notificationHour.first()
                val minute = prefs.notificationMinute.first()
                BootReceiver.scheduleDailyReminder(this@ReadyToReviseApplication, hour, minute)
            }
        }
    }
}
