package com.uz.kitoblar.ui

import android.util.Log
import com.uz.kitoblar.BuildConfig

fun log(message: String?) {
    if (BuildConfig.DEBUG) {
        if (message!=null) {
            Log.i("BooksLog: ",message)
        }
    }
}

fun log(exception: Exception?) {
    exception?.message?.let {
        log(it)
    }
}

