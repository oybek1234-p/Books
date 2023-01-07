package com.uz.kitoblar.ui.components.actionBar.bottomSheet

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.uz.kitoblar.ui.toast

class BottomSheetController {
    private var sheets = arrayListOf<BottomSheetBase<*>>()

    private lateinit var sheetBehaviour: BottomSheetBehavior<MaterialCardView>
    private lateinit var sheetContainer: MaterialCardView
    private lateinit var container: ViewGroup

    private fun currentSheet() = sheets.lastOrNull()
    private fun lastSheet() = sheets.getOrNull(sheets.lastIndex - 1)

    var animating = false
    private var inited = false

    var slideOffsetData = MutableLiveData<Float>()
    var stateChangedData = MutableLiveData<Int>()

    private var show = false

    @SuppressLint("ClickableViewAccessibility")
    fun init(sheetView: MaterialCardView, container: ViewGroup) {
        if (inited) return
        sheetContainer = sheetView
        sheetBehaviour = BottomSheetBehavior.from(sheetView).apply {
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    slideOffsetData.postValue(slideOffset)
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    stateChangedData.postValue(newState)
                    show = sheetBehaviour.calculateSlideOffset() > 0 || sheetBehaviour.state == BottomSheetBehavior.STATE_SETTLING
                    container.requestDisallowInterceptTouchEvent(show)
                    animating = newState == BottomSheetBehavior.STATE_SETTLING
                }
            })
            peekHeight = 0
            isHideable = true
            state = BottomSheetBehavior.STATE_HIDDEN
        }
        inited = true
    }

    fun closeLastSheet() {
        showSheet(lastSheet(), true, close = true)
    }

    fun closeAllSheets() {
        if (sheets.size == 0) {
            sheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
            return
        }
        if (sheets.size == 1) {
            closeLastSheet()
        } else {
            val current = currentSheet()
            for (i in 0..sheets.lastIndex) {
                val sheet = sheets[i]
                if (sheet != current) {
                    sheet.apply {
                        destroy()
                        sheets.remove(this)
                    }
                }
            }
            closeLastSheet()
        }
    }

    private var sheetCallback: BottomSheetBehavior.BottomSheetCallback? = null

    fun showSheet(sheet: BottomSheetBase<*>?, removeLast: Boolean = true, close: Boolean = false) {
        val updateRunnable = {
            sheetCallback?.let {
                sheetBehaviour.removeBottomSheetCallback(it)
            }
            val currentSheet = currentSheet()
            currentSheet?.apply {
                onPause()
                dismissRunnable = null
                sheetContainer.removeView(container().root)
                if (removeLast) {
                    destroy()
                    sheets.remove(this)
                }
            }
            sheet?.apply {
                val view = createView()
                sheetContainer.addView(
                    view,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        if (view.height > 0) view.height else ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
                sheets.remove(this)
                sheets.add(this)
                dismissRunnable = Runnable {
                    closeLastSheet()
                }
                sheetController = this@BottomSheetController
                onResume()
            }
        }
        if (sheetBehaviour.calculateSlideOffset() != 0f) {
            sheetCallback?.let {
                sheetBehaviour.removeBottomSheetCallback(it)
            }
            sheetCallback = object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {

                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (sheetBehaviour.state == BottomSheetBehavior.STATE_HIDDEN) {
                        sheetBehaviour.removeBottomSheetCallback(this)
                        updateRunnable.invoke()
                        if (sheets.isNotEmpty()) {
                            sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }
                }
            }
            sheetBehaviour.addBottomSheetCallback(sheetCallback!!)
            sheetBehaviour.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            updateRunnable.invoke()
            if (sheets.isNotEmpty()) {
                sheetBehaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }
}