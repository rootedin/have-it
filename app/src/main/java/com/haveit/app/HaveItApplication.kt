package com.haveit.app

import android.app.Application
import com.haveit.app.data.AppContainer
import com.haveit.app.notification.HaveItNotifications

class HaveItApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        HaveItNotifications.ensureChannel(this)
    }
}
