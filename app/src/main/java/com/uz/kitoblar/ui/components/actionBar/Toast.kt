package com.uz.kitoblar.ui

import android.widget.Toast
import com.uz.kitoblar.BuildConfig
import com.uz.kitoblar.applicationContext

fun toast(message: String?) {
    if (BuildConfig.DEBUG) {
        Toast.makeText(applicationContext(), message, Toast.LENGTH_SHORT).show()
    }
}

fun toast(exception: Exception?) {
    exception?.message?.let {
        toast(it)
    }
}

