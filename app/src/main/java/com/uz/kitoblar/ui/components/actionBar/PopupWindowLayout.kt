package com.uz.kitoblar.ui.components.actionBar

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.uz.kitoblar.*

class PopupWindowLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attributeSet, defStyle) {

    var items = HashMap<Int, Item>()
    var dismiss: Runnable? = null

    init {
        backgroundTint(R.attr.colorSurface)

        orientation = VERTICAL
        gravity = Gravity.START
        layoutParams = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun startChildAnimation(child: View, show: Boolean) {
        child.apply {
            if (show) {
                alpha = 0f
                translationY = -dp(6f).toFloat()
            }
            animate().alpha(if (show) 1f else 0f).translationY(if (show) 0f else -dp(6f).toFloat())
                .setDuration(if (show) 180 else 70).start()
        }
    }

    fun addItem(id: Int, title: String, iconRes: Int, onClick: (v: View) -> Unit): Item {
        val item = Item(context).apply {
            setData(title, iconRes)
            setOnClickListener {
                onClick(it)
                dismiss?.run()
            }
            items[id] = this
        }
        addView(item)
        return item
    }

    class Item(context: Context) : LinearLayout(context) {
        private val m = dp(18f)

        init {
            background = createSelectorDrawable(Color.GRAY, 4, 10)
            orientation = HORIZONTAL
            setPadding(m, dp(12f), m + 12, dp(12f))
            isClickable = true
            isFocusable = true
            gravity = Gravity.START
        }

        fun setData(title: String, iconRes: Int) {
            titleView.text = title
            iconView.setImageResource(iconRes)
        }

        var iconView = ImageView(context).apply {
            gravity = Gravity.START
            imageTint(R.attr.colorOnSurfaceMedium)
            addView(this, LayoutParams(dp(24f), dp(24f)).apply {
                gravity = Gravity.CENTER
            })
        }

        private var titleView = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            typeface = context.getFont(R.font.roboto_regular)
            textColor(R.attr.colorOnSurfaceHigh)
            addView(this, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = m
                gravity = Gravity.CENTER_VERTICAL
            })
        }
    }
}