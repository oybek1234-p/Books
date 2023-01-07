package com.uz.kitoblar

import android.view.View
import com.algolia.search.saas.AlgoliaException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.DatabaseError
import com.uz.kitoblar.ui.toast

fun handleError(error: Exception?, view: View? = null): Boolean {
    error?.apply {
        when (error) {
            is FirebaseAuthException -> {
                when (error.errorCode) {
                    "ERROR_WEAK_PASSWORD" -> {
                        //Password error
                        shakeView(view)
                        toast("Parol notugri,tekshitib qayta urinib koring!")
                    }
                    "ERROR_INVALID_EMAIL" -> {
                        //Email error
                        toast("Email notugri terilgan,togirlab qayta urinib koring!")
                    }
                    else -> {
                        toast(message)
                    }
                }
            }
            is AlgoliaException -> {
                //Fill
                toast(message)
            }
            else -> {
                toast(message)
            }
        }
    }
    return error != null
}

fun handleError() {

}
fun handleError(error: DatabaseError?) {
    val mutableList = arrayListOf<Int>()
    mutableList.forEachIndexed { index, i ->
        val f = 0F
        f as? String
    }
}

fun handleError(message: String?) {

}