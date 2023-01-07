package com.uz.kitoblar.ui.components.actionBar

import android.content.Context

object AlertsCreator {
    @JvmStatic
    fun createSimpleAlert(context: Context, title: String?, message: String) =
        AlertDialog(context).apply {
            setTitle(title ?: "Title")
            setMessage(message)
            setPositiveButton("OK", null)
        }

    @JvmStatic
    fun showSimpleAlert(
        context: Context,
        title: String,
        message: String
    ): AlertDialog {
        return createSimpleAlert(context, title, message).show()
    }

    @JvmStatic
    fun showSimpleAlert(
        context: Context,
        message: String
    ): AlertDialog {
        return createSimpleAlert(context, null, message).show()
    }

    @JvmStatic
    fun showAlert(
        context: Context,
        title: String,
        message: String,
        cancelName: String,
        actionName: String,
        cancelOnClick: (() -> Unit)?,
        actionOnClick: (() -> Unit)?,
        imageUrl: String? = null,
        imageResource: Int? = null,
        isPhoto: Boolean = true
    ): AlertDialog {
        return AlertDialog(context).apply {
            setTitle(title)
            setMessage(message)
            setNegativeButton(cancelName, cancelOnClick)
            setPositiveButton(actionName, actionOnClick)
            imageUrl?.let { setImageUrl(it) }
            imageResource?.let { setImageResource(imageResource) }
            setIsPhoto(isPhoto)
        }.show()
    }
}