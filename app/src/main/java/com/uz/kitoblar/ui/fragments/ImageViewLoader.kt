package com.uz.kitoblar.ui.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestListener
import com.uz.kitoblar.findActivity

class ImageViewLoader @JvmOverloads
constructor(
    context: Context,
    attr: AttributeSet? = null,
    defStyle: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attr, defStyle) {

    var fade = true
    var circleCrop = false
    private var data: Any? = null
    private var ignoreLayout = false

    var cornerRadius = 0f
        set(value) {
            field = value
            // shakeView()
            clipToOutline = !value.equals(0f)
            invalidateOutline()
        }

    private var outLineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            if (view == null) return

            outline?.setRoundRect(0, 0, view.width, view.height, cornerRadius)
        }
    }

    init {
        outlineProvider = this.outLineProvider
        scaleType = ScaleType.CENTER_CROP
    }

    private var imageSet = false
    private var imageRequest: RequestBuilder<Bitmap>? = null

    fun setData(url: Any) {
        if (url != data) {
            data = url
            imageSet = false
            requestLayout()
        }
    }

    private var drawableCallback: ((drawable: Drawable) -> Unit)? = null

    fun setDrawableCallback(callback: ((drawable: Drawable) -> Unit)? = null) {
        drawableCallback = callback
    }

    override fun setImageDrawable(drawable: Drawable?) {
        ignoreLayout = true
        super.setImageDrawable(drawable)
        drawable?.let { drawableCallback?.invoke(it) }
        ignoreLayout = false
    }

    override fun requestLayout() {
        if (ignoreLayout) return
        super.requestLayout()
    }

    var requestListener: RequestListener<Bitmap>? = null

    private fun loadImage() {
        if (measuredHeight > 0 && measuredWidth > 0) {
            if (data != null && !imageSet) {
                var glide = Glide.with(context).asBitmap().load(data)
                if (circleCrop) {
                    glide = glide.circleCrop()
                }
                glide = glide.override(measuredWidth, measuredHeight)

                imageRequest = glide
                requestListener?.let {
                    glide = glide.addListener(it)
                }
                glide.into(this)
                imageSet = true
            }

        }
    }

    private fun clearImage() {
        findActivity(context)?.let {
            Glide.with(it).clear(this)
        }
        imageSet = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!imageSet) {
            imageRequest?.into(this)
        }
    }

    override fun onDetachedFromWindow() {
        clearImage()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        loadImage()
    }
}