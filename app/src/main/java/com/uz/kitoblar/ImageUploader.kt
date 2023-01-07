package com.uz.kitoblar

import android.content.Context
import androidx.core.net.toUri
import id.zelory.compressor.Compressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

private var imagesRef = firebaseStorage().getReference("images")

fun uploadImage(context: Context, path: String, result: (downloadUrl: String?) -> Unit) {
    GlobalScope.launch {
        val id = randomId()
        val compressed = Compressor.compress(context, File(path), Dispatchers.IO)

        imagesRef.child(id).putFile(compressed.toUri()).addOnCompleteListener {
            if (it.isSuccessful) {
                imagesRef.child(id).downloadUrl.addOnCompleteListener {
                    if (it.result != null) {
                        result(it.result.toString())
                    } else {
                        result(null)
                    }
                }
            } else {
                result.invoke(null)
            }
        }
    }
}