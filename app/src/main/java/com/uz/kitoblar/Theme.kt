package com.uz.kitoblar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.util.StateSet
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doOnTextChanged
import androidx.databinding.BindingAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import com.uz.kitoblar.ui.log
import com.uz.kitoblar.utils.SharedModel
import kotlin.math.ceil
import kotlin.math.sqrt

@BindingAdapter("backgroundColor")
fun View.backgroundColor(@AttrRes attrRes: Int) {
    setBackgroundColor(getThemeColor(attrRes))
}

@BindingAdapter("backgroundTint", "alpha", requireAll = false)
fun View.backgroundTint(@AttrRes attrRes: Int, alpha: Float = 1f) {
    backgroundTintList = ColorStateList.valueOf(
        if (alpha != 1f) ColorUtils.setAlphaComponent(
            getThemeColor(attrRes),
            (alpha * 255).toInt()
        ) else getThemeColor(attrRes)
    )
}

@BindingAdapter("drawableTint")
fun TextView.drawableTint(@AttrRes attrRes: Int) {
    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(getThemeColor(attrRes)))
}

@BindingAdapter("foregroundColor")
fun View.foregroundTint(@AttrRes attrRes: Int) {
    foregroundTintList = ColorStateList.valueOf(getThemeColor(attrRes))
}

@BindingAdapter("textColor", "fromTheme", requireAll = false)
fun TextView.textColor(color: Int, fromTheme: Boolean? = null) {
    setTextColor(if (fromTheme == null || fromTheme) getThemeColor(color) else color)
}

@BindingAdapter("textColorHint")
fun TextView.textColorHint(@AttrRes attrRes: Int) {
    setHintTextColor(getThemeColor(attrRes))
}

@BindingAdapter("progressColor")
fun ProgressBar.progressColor(@AttrRes attrRes: Int) {
    indeterminateTintList = ColorStateList.valueOf(getThemeColor(attrRes))
}

@BindingAdapter(
    "thumbChecked",
    "thumbUnchecked",
    "trackChecked",
    "trackUnchecked"
)
fun SwitchMaterial.switchMaterialColors(
    @AttrRes thumbChecked: Int,
    @AttrRes thumbUnchecked: Int,
    @AttrRes trackChecked: Int,
    @AttrRes trackUnchecked: Int,
) {
    thumbTintList = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        ),
        intArrayOf(getThemeColor(thumbChecked), getThemeColor(thumbUnchecked))
    )

    trackTintList = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        ),
        intArrayOf(getThemeColor(trackChecked), getThemeColor(trackUnchecked))
    )
}

@BindingAdapter("applyTintList")
fun Button.applyTintList(attr: Any? = null) {
    backgroundTintState(
        R.attr.colorSecondary,
        R.attr.colorBackground,
        android.R.attr.state_enabled
    )
    textColorList(
        R.attr.colorOnSecondary,
        R.attr.colorOnSurfaceLow,
        android.R.attr.state_enabled
    )
}

fun View.backgroundTintState(
    activeColor: Int,
    unActiveColor: Int,
    state: Int
) {
    backgroundTintList = ColorStateList(
        arrayOf(
            intArrayOf(state),
            intArrayOf(-state)
        ),
        intArrayOf(getThemeColor(activeColor), getThemeColor(unActiveColor))
    )
}

fun TextView.textColorList(
    activeColor: Int,
    unActiveColor: Int,
    state: Int
) {
    setTextColor(
        ColorStateList(
            arrayOf(
                intArrayOf(state),
                intArrayOf(-state)
            ),
            intArrayOf(getThemeColor(activeColor), getThemeColor(unActiveColor))
        )
    )
}

@BindingAdapter(
    "itemsTextColorSelected",
    "itemsIconTintSelected",
    "itemsTextColorUnSelected",
    "itemsIconTintUnSelected"
)
fun BottomNavigationView.itemsColor(
    @AttrRes itemsTextColorSelected: Int,
    @AttrRes itemsIconTintSelected: Int,
    @AttrRes itemsTextColorUnSelected: Int,
    @AttrRes itemsIconTintUnSelected: Int,
) {
    itemIconTintList = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        ),
        intArrayOf(getThemeColor(itemsIconTintSelected), getThemeColor(itemsIconTintUnSelected))
    )

    itemTextColor = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        ),
        intArrayOf(getThemeColor(itemsTextColorSelected), getThemeColor(itemsTextColorUnSelected))
    )

}

fun Context.getThemeColor(attrRes: Int): Int {
    val typedValue = TypedValue()
    if (!theme.resolveAttribute(attrRes, typedValue, true)) {
        log("Color not found attr id $attrRes")
    }
    return typedValue.data
}

fun View.getThemeColor(attr: Int): Int {
    return context.getThemeColor(attr)
}

@BindingAdapter("imageTint")
fun ImageView.imageTint(@AttrRes attrRes: Int) {
    imageTintList = ColorStateList.valueOf(getThemeColor(attrRes))
}

fun TextInputLayout.clearErrorOnTextChange() {
    editText!!.doOnTextChanged { text, start, before, count ->

    }
}

//Theme
data class Theme(
    var themeResId: Int = 0,
    var name: String = "",
    var color: Int = 0,
    var imageUrl: String = ""
)

var themes = arrayListOf(
    Theme(R.style.Theme_Books, "Pink", 0, "url")
)

var currentTheme = themes.first()
var currentThemeId = currentTheme.themeResId

val themeConfig = SharedModel("themeConfig", Theme()).apply {
    if (model.themeResId != 0) {
        setCurrentTheme(this.model.themeResId, true)
    } else {
        saveModel(currentTheme)
    }
}

fun setCurrentTheme(themeId: Int, fromConfig: Boolean) {
    if (themeId != 0) {
        val themeInfo = themes.find { it.themeResId == themeId }
        if (themeInfo != null) {
            currentTheme = themeInfo
            currentThemeId = themeId
            if (!fromConfig) {
                themeConfig.saveModel(currentTheme)
            }
        }
    }
}

fun Context.getFont(id: Int) = ResourcesCompat.getFont(this, id)

fun createsSelectorDrawable(color: Int): Drawable {
    return createSelectorDrawable(color, 2, -1)
}

private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)

fun createSelectorDrawable(color: Int, maskType: Int, radius: Int): Drawable {
    var maskDrawable: Drawable? = null
    if ((maskType == 1 || maskType == 5)) {
        maskDrawable = null
    } else if (maskType == 1 || maskType == 3 || maskType == 4 || maskType == 5 || maskType == 6 || maskType == 7) {
        maskPaint.color = -0x1
        maskDrawable = object : Drawable() {
            var rect: RectF? = null
            override fun draw(canvas: Canvas) {
                val bounds = bounds
                if (maskType == 7) {
                    if (rect == null) {
                        rect = RectF()
                    }
                    rect!!.set(bounds)
                    canvas.drawRoundRect(
                        rect!!,
                        dp(6f).toFloat(),
                        dp(6f).toFloat(),
                        maskPaint
                    )
                } else {
                    val rad: Int = if (maskType == 1 || maskType == 6) {
                        dp(20f)
                    } else if (maskType == 3) {
                        bounds.width().coerceAtLeast(bounds.height()) / 2
                    } else {
                        ceil(sqrt(((bounds.left - bounds.centerX()) * (bounds.left - bounds.centerX()) + (bounds.top - bounds.centerY()) * (bounds.top - bounds.centerY())).toDouble()))
                            .toInt()
                    }
                    canvas.drawCircle(bounds.centerX().toFloat(),
                        bounds.centerY().toFloat(),
                        rad.toFloat(),
                        maskPaint)
                }
            }

            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            override fun getOpacity(): Int {
                return PixelFormat.UNKNOWN
            }
        }
    } else if (maskType == 2) {
        maskDrawable = ColorDrawable(-0x1)
    }
    val colorStateList = ColorStateList(arrayOf(StateSet.WILD_CARD), intArrayOf(color))
    val rippleDrawable = RippleDrawable(colorStateList, null, maskDrawable)

    if (maskType == 1) {
        rippleDrawable.radius = if (radius <= 0) dp(20f) else radius
    } else if (maskType == 5) {
        rippleDrawable.radius = RippleDrawable.RADIUS_AUTO
    }
    return rippleDrawable
}