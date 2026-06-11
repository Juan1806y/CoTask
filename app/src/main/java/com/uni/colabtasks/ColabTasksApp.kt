package com.uni.colabtasks

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import com.uni.colabtasks.reminder.ReminderWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ColabTasksApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // setPersistenceEnabled must be called BEFORE any FirebaseDatabase reference is created.
        runCatching { FirebaseDatabase.getInstance().setPersistenceEnabled(true) }
        // Canal de notificaciones para los recordatorios de fecha límite.
        ReminderWorker.ensureChannel(this)
    }
}
