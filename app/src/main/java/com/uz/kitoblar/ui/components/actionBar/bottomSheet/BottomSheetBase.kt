package com.uz.kitoblar.ui.components.actionBar.bottomSheet

import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.BottomSheetContainerBinding
import com.uz.kitoblar.findActivity
import com.uz.kitoblar.hideKeyboard
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.inflateBinding
import com.uz.kitoblar.ui.visibleOrGone

open class BottomSheetBase<T : ViewDataBinding>(private val layId: Int) {

    private var container: BottomSheetContainerBinding? = null
    var layoutBinding: T? = null
    var sheetController: BottomSheetController? = null

    fun rBinding() = layoutBinding!!
    private fun viewCreated() = container != null && layoutBinding != null

    open fun onResume() {}
    open fun onPause() {
        hideKeyboard(mainActivity()!!)
    }

    private var showHeader = true

    fun container() = container!!

    open fun onViewCreated(binding: T) {

    }

    fun mainActivity() = findActivity(container?.root?.context)
    var dismissRunnable: Runnable? = null

    fun destroy() {
        container = null
        layoutBinding = null
    }

    fun openFragment(fragment: BaseFragment<*>) {
        mainActivity()?.openFragment(fragment,false)
    }

     fun closeSheet() {
        dismissRunnable?.run()
    }

    fun createView(): View {
        if (viewCreated()) return container().root
        container = inflateBinding(
            null,
            R.layout.bottom_sheet_container
        )
        layoutBinding = inflateBinding<T>(null, layId)
        container?.apply {
            val view = layoutBinding!!.root
            containerView.addView(
                view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            onViewCreated(layoutBinding!!)

            headerContainer.visibleOrGone(showHeader)

            if (showHeader) {
                backButton.setOnClickListener {
                    closeSheet()
                }
            }
        }
        return container!!.root
    }
}