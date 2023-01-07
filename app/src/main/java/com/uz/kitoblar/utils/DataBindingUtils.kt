package com.uz.kitoblar.ui

import android.animation.Animator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.airbnb.lottie.LottieAnimationView
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.EmtyViewBinding
import com.uz.kitoblar.databinding.InfoLayoutBinding
import com.uz.kitoblar.ui.fragments.ImageViewLoader

fun <T : ViewDataBinding> inflateBinding(
    parent: ViewGroup? = null,
    layoutId: Int,
    attach: Boolean = false
): T {
    return DataBindingUtil.inflate(appInflater, layoutId, parent, attach)
}

val overShootInterpolator = OvershootInterpolator(2.5f)
val decelerateInterpolator = DecelerateInterpolator(1.5f)

@BindingAdapter("visibleOrGone", "type", "onAnimEnd", "duration", "invisible", requireAll = false)
fun View.visibleOrGone(
    visibleOrGone: Boolean,
    type: Int = -1,
    onAnimEnd: (() -> Unit)? = null,
    duration: Long = 300,
    invisible: Boolean = false
) {
    animate().cancel()
    if (isVisible == visibleOrGone) return
    if (type != -1) {
        visibility = View.VISIBLE
        if (visibleOrGone) {
            alpha = 0f
            if (type == 1) {
                scaleY = 0.8f
                scaleX = 0.8f
            }
        }
        val onEnd = {
            if (!visibleOrGone) {
                visibility = if (invisible) View.INVISIBLE else View.GONE
            } else {
                alpha = 1f
                scaleY = 1f
                scaleX = 1f
            }
        }
        var anim = animate().alpha(if (visibleOrGone) 1f else 0f).setDuration(if (visibleOrGone) duration else 200)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    onEnd.invoke()
                    onAnimEnd?.invoke()
                }

                override fun onAnimationCancel(animation: Animator) {
                    onEnd.invoke()
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
        if (type == 1) {
            val scale = if (visibleOrGone) 1f else 0.8f
            anim = anim.scaleX(scale).scaleY(scale).setInterpolator(if (visibleOrGone) overShootInterpolator else decelerateInterpolator)
        }
        anim.start()

    } else {
        visibility =
            if (visibleOrGone) View.VISIBLE else if (invisible) View.INVISIBLE else View.GONE
        onAnimEnd?.invoke()
    }
}

fun View.tapScale(scale: Boolean = true) {
    animate().scaleX(if (scale) 1.2f else 1f).scaleY(if (scale) 1.2f else 1f).setDuration(300)
        .setInterpolator(
            DecelerateInterpolator(1f)
        )
        .withEndAction {
            if (scale) {
                tapScale(false)
            }
        }.start()
}

fun EmtyViewBinding.setData(
    lottieUrl: String,
    title: String,
    subtitle: String,
    buttonText: String,
    buttonOnClick: ((view: View?) -> Unit)? = null
) {
    lottieView.apply {
        if (lottieUrl.isNotEmpty()) {
            loadLottie(lottieUrl)
        }
    }
    titleView.text = title
    subtitleView.text = subtitle

    actionButton.apply {
        if (buttonText.isNotEmpty().also { visibleOrGone(it) }) {
            text = buttonText
            setOnClickListener(buttonOnClick)
        }
    }
    executePendingBindings()
}

fun LinearLayout.addBackgroundText(str: String) {
    val textView = TextView(context).apply {
        textSize = 15f
        setTextColor(getThemeColor(R.attr.colorOnBackground))
        backgroundColor(R.attr.colorBackground)
        val left = dp(12f)
        val v = dp(6)
        setPadding(left, v, left, v)
        text = str
    }
    addView(textView, layParams(MATCH_PARENT, WRAP_CONTENT))
}

fun LinearLayout.addInfoButton(icon: Int, text: String, onClick: (view: View) -> Unit): InfoLayoutBinding{
    val button = inflateBinding<InfoLayoutBinding>(this, R.layout.info_layout).apply {
        if (icon != 0) {
            iconView.setImageResource(icon)
        }
        root.apply {
            foreground = createsSelectorDrawable(context.getColor(R.color.ripple))
            isClickable = true
            isFocusable = true
            setOnClickListener(onClick)
        }

        titleView.text = text
        executePendingBindings()
    }
    addView(button.root,layParams(MATCH_PARENT, dp(50)))
    return button
}

fun ImageView.load(url: String,fade: Boolean = true, circle: Boolean = false,placeHolder: Int?=null,) {
    val request = ImageRequest.Builder(context).apply {
        if (circle) {
            transformations(CircleCropTransformation())
        }
        crossfade(fade)
        placeHolder?.let { placeholder(it) }
        data(url)
        target(this@load)
    }.build()
    context.imageLoader.enqueue(request)
}

fun LottieAnimationView.loadLottie(url: String) {
    repeatCount = 4
    setOnClickListener {
        playAnimation()
    }
    setAnimationFromUrl(url)
    playAnimation()
}

@BindingAdapter("url","circleCrop", requireAll = false)
fun ImageViewLoader.load(url: String,circleCrop: Boolean) {
    this.circleCrop = circleCrop
    scaleType = ImageView.ScaleType.CENTER_CROP
    setData(url)
}