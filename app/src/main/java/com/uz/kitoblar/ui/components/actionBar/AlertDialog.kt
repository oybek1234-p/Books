package com.uz.kitoblar.ui.components.actionBar

import android.content.Context
import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.AlertDialogBinding
import com.uz.kitoblar.ui.inflateBinding
import com.uz.kitoblar.ui.visibleOrGone
import java.nio.ByteBuffer

data class Alert(
    var title: String,
    var message: String,
    var cancelName: String,
    var actionName: String,
    var cancelOnClick: (() -> Unit)?,
    var actionOnClick: (() -> Unit)?,
    var iconUrl: String? = null,
    var iconResource: Int? = null,
    var isPhoto: Boolean = true
)

open class AlertDialog(var context: Context) {
    private var binding: AlertDialogBinding? = null
    private var popupWindowLayout: PopupWindowLayout? = null
    private var popDialog: PopupDialog? = null
    private var isCreated = false
    var destroyOnDismiss = true
    var dismissOnButtonClick = true

    private var alert = Alert(
        "Title",
        "Message",
        "",
        "", null, null
    )

    fun setCustomAlert(alert: Alert) {
        this.alert = alert
        notifyUi()
    }

    private fun isShowing() = popDialog?.isShowing == true

    private fun notifyUi(forceBind: Boolean = false) {
        binding?.apply {
            data = alert
            iconView.apply {
                var visible = true
                if (alert.isPhoto && !alert.iconUrl.isNullOrEmpty()) {
                    setData(alert.iconUrl!!)
                } else if (alert.iconResource!=null) {
                    setImageResource(alert.iconResource!!)
                } else {
                    visible = false
                }
                visibleOrGone(visible)
            }
            if (forceBind || isShowing()) {
                executePendingBindings()
            }
        }
    }

    fun setAlert(alert: Alert) {
        this.alert = alert
        notifyUi()
    }

    fun setIsPhoto(isPhoto: Boolean) {
//        alert.isPhoto = isPhoto
//        notifyUi()
    }

    fun setImageUrl(url: String) {
//        alert.iconUrl = url
//        notifyUi()
    }

    fun setImageResource(res: Int) {
//        alert.iconResource = res
//        notifyUi()
    }

    fun setPositiveButton(name: String, onClick: (() -> Unit)?) {
        alert.apply {
            actionName = name
            actionOnClick = onClick
        }
        notifyUi()
    }

    fun setNegativeButton(name: String, onClick: (() -> Unit)?) {
        alert.apply {
            cancelName = name
            cancelOnClick = onClick
        }
        notifyUi()
    }

    fun setTitle(title: String) {
        alert.title = title
        notifyUi()
    }

    fun setMessage(message: String) {
        alert.message = message
        notifyUi()
    }

    fun destroy() {
        popDialog?.apply {
            isCreated = false
            if (isShowing) {
                dismiss()
            }
            popupWindowLayout = null
            binding = null
            popDialog = null
        }
    }

    protected open fun create() {
        if (isCreated) return
        binding = inflateBinding<AlertDialogBinding>(null, R.layout.alert_dialog).also { binding ->
            popupWindowLayout = PopupWindowLayout(context).apply {
                addView(binding.root)
                binding.apply {
                    cancelButton.setOnClickListener {
                        data?.cancelOnClick?.invoke()
                        dismiss()
                    }
                    actionButton.setOnClickListener {
                        data?.actionOnClick?.invoke()
                        dismiss()
                    }
                }
                popDialog = PopupDialog(this, DIALOG_ANIMATION_ALERT_DIALOG).apply {
                    setOnDismissListener {
                        if (destroyOnDismiss) {
                            destroy()
                        }
                    }
                }

                isCreated = true
                onCreateView(binding)
            }
        }
    }

    fun dismiss() {
        popDialog?.dismiss()
    }

    fun check() {
        binding?.apply {
            alert.apply {
                val needIcon = iconResource == null && iconUrl == null
                iconView.visibleOrGone(!needIcon)
                cancelButton.visibleOrGone(actionName.isNotEmpty())
                if (actionName.isEmpty()) {
                    actionButton.text = "Ok"
                }
            }
        }
    }

    fun show(): AlertDialog {
        create()
        notifyUi(true)
        check()
        popDialog?.show()
        return this
    }

    open fun onCreateView(binding: AlertDialogBinding) {
        //
    }
}
