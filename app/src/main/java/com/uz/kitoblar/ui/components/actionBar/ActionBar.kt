package com.uz.kitoblar.ui.components.actionBar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import androidx.core.view.forEach
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.ActionBarBinding
import com.uz.kitoblar.ui.*

class ActionBar {
    private var actionBarBinding: ActionBarBinding? = null

    fun requireBinding() = actionBarBinding!!
    fun backButton() = requireBinding().imageView
    fun menuContainer() = requireBinding().menuContainer
    fun view() = actionBarBinding?.root

    var backButtonEnabled = false
        set(value) {
            if (value != field) {
                field = value
                backButton().visibleOrGone(value)
            }
        }

    var title = ""
        set(value) {
            field = value
            requireBinding().titleView.text = title
        }

    var backgroundColor = 0
        set(value) {
            field = value
            requireBinding().root.setBackgroundColor(value)
        }

    var iconsColor = 0
        set(value) {
            field = value
            menuContainer().forEach {
                val menuItem = it as MenuItem
                menuItem.imageTintList = ColorStateList.valueOf(value)
            }
            backButton().imageTintList = ColorStateList.valueOf(value)
            requireBinding().titleView.setTextColor(value)
        }

    init {
        actionBarBinding = inflateBinding(null,R.layout.action_bar)
        backButtonEnabled = true
        backgroundColor = menuContainer().getThemeColor(R.attr.colorPrimary)
        iconsColor = menuContainer().getThemeColor(R.attr.colorOnPrimaryMedium)
    }

    fun addMenu(icon: Int): MenuItem {
        MenuItem(menuContainer().context).apply {
            scaleType = ImageView.ScaleType.CENTER
            imageTintList = ColorStateList.valueOf(iconsColor)
            setImageResource(icon)
            menuContainer().addView(this,menuContainer().layParams(dp(48f), MATCH_PARENT))
            return this
        }
    }

    class MenuItem(context: Context) : androidx.appcompat.widget.AppCompatImageView(context) {

        class ItemData(
            var id: Int,
            var icon: Int,
            var text: String,
            var click: (view: View) -> Unit
        )

        private var popDialog: PopupDialog? = null
        private var popupWindow: PopupWindowLayout? = null

        private var menusData = arrayListOf<ItemData>()

        fun getMenu(id: Int): PopupWindowLayout.Item? = popupWindow?.items?.get(id)
        fun menus() = popupWindow?.items

        private fun createDialog(): PopupDialog {
            if (popDialog == null) {
                popupWindow = PopupWindowLayout(context).apply {
                    menusData.forEach {
                        addItem(it.id, it.text, it.icon, it.click)
                    }
                    popDialog = PopupDialog(this)
                }
            }
            return popDialog!!
        }

        fun addSubMenu(id: Int, icon: Int, text: String, click: (view: View) -> Unit) {
            if (menusData.find { it.id == id } == null) {
                menusData.add(ItemData(id, icon, text, click))
            }
            if (popupWindow != null) {
                popupWindow?.apply {
                    addItem(id, text, icon, click)
                }
            }
        }

        init {
            setOnClickListener {
                show()
            }
        }

        init {
            background = createsSelectorDrawable(context.getThemeColor(R.attr.colorSurface))
            isClickable = true
            isFocusable = true
        }

        private fun show() {
            if (menusData.size > 0) {
                createDialog().show(this)
            }
        }
    }
}