package com.eliya.callguard

import android.app.Application
import android.content.Context
import com.eliya.callguard.data.GuardRepository
import com.eliya.callguard.screening.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CallGuardApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var repository: GuardRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = GuardRepository(this, appScope)
        Notifications.createChannels(this)
    }
}

val Context.guardApp: CallGuardApp
    get() = applicationContext as CallGuardApp
