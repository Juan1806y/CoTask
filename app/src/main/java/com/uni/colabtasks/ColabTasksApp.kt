package com.uni.colabtasks

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ColabTasksApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // setPersistenceEnabled must be called BEFORE any FirebaseDatabase reference is created.
        runCatching { FirebaseDatabase.getInstance().setPersistenceEnabled(true) }
    }
}
