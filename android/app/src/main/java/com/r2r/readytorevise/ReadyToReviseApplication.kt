package com.r2r.readytorevise

import android.app.Application
import androidx.work.WorkManager
import com.r2r.readytorevise.di.AppContainer
import com.r2r.readytorevise.di.DefaultAppContainer
import com.r2r.readytorevise.data.remote.NetworkConstants
import com.r2r.readytorevise.notification.BootReceiver
import com.r2r.readytorevise.notification.NotificationHelper
import com.r2r.readytorevise.notification.NotificationPreferences
import com.r2r.readytorevise.notification.UploadNotifications
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
        UploadNotifications.createUploadChannel(this)

        applicationScope.launch {
            runCatching {
                (java.net.URL("${NetworkConstants.BASE_URL}api/v1/health").openConnection() as java.net.HttpURLConnection).run {
                    connectTimeout = 15000
                    readTimeout = 15000
                    connect()
                    responseCode
                    disconnect()
                }
            }
        }

        val prefs = NotificationPreferences(this)
        applicationScope.launch {
            WorkManager.getInstance(this@ReadyToReviseApplication)
            val enabled = prefs.notificationsEnabled.first()
            if (enabled) {
                val hour = prefs.notificationHour.first()
                val minute = prefs.notificationMinute.first()
                BootReceiver.scheduleDailyReminder(this@ReadyToReviseApplication, hour, minute)
            }
        }
    }
}
