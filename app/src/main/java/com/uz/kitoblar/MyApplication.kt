package com.uz.kitoblar

import android.app.Application
import android.content.Context
import android.os.Looper
import android.util.JsonReader
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.uz.kitoblar.ui.controllers.toGsonObject


class MyApplication : Application() {

    companion object {
        lateinit var applicationContext: Context
        lateinit var handler: android.os.Handler

        fun initFirebaseAppCheck(context: Context) {
            runOnUiThread(2000){
                FirebaseApp.initializeApp(context)
                val firebaseAppCheck: FirebaseAppCheck = FirebaseAppCheck.getInstance()
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        MyApplication.applicationContext = applicationContext
        handler = android.os.Handler(Looper.getMainLooper())

        initUtils()
        initFirebaseAppCheck(this)
        booksController()
    }
}