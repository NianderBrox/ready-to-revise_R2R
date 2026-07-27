package com.r2r.readytorevise

import android.app.Application
import com.r2r.readytorevise.di.AppContainer
import com.r2r.readytorevise.di.DefaultAppContainer

class ReadyToReviseApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
