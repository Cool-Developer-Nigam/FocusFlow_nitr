package com.nigdroid.focusflow_nitr

import android.app.Application
import com.google.firebase.FirebaseApp

class FocusFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}