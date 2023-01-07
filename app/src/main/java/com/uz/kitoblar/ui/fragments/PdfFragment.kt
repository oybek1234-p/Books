package com.uz.kitoblar.ui.fragments

import android.graphics.Color
import android.net.Uri
import androidx.core.net.toFile
import com.danjdt.pdfviewer.PdfViewer
import com.github.barteksc.pdfviewer.util.FitPolicy
import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.PdfFragmentBinding
import com.uz.kitoblar.dp
import com.uz.kitoblar.setLightStatusBar
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.visibleOrGone

class PdfFragment(private val uri: Uri, val book: Book, val bet: Int = 0) : BaseFragment<PdfFragmentBinding>(R.layout.pdf_fragment) {

    private var showOnScreen = true
    set(value) {
        if (value != field) {
            field = value
            requireBinding().apply {
                leftButton.visibleOrGone(value,1)
                rightButton.visibleOrGone(value,1)
                pageCountv.visibleOrGone(value,1)
                requireActionBar().view()?.visibleOrGone(value,2)
                mainActivity().setLightStatusBar(!showOnScreen)
                lightStatusBar = !showOnScreen
            }
        }
    }

    override fun onViewCreated(binding: PdfFragmentBinding) {
        super.onViewCreated(binding)
        requireActionBar().apply {
            backgroundColor = Color.BLACK
            title = book.name
        }
        binding.apply {
            renderView.apply {
               fromUri(uri).defaultPage(bet)
                    .onPageChange { page, pageCount ->
                        val p = page + 1
                        pageCountv.text = "$p/$pageCount"
                    }
                    .onTap {
                        showOnScreen = !showOnScreen
                        return@onTap true
                    }
                   .spacing(dp(4))
                   .fitEachPage(true)
                   .pageFitPolicy(FitPolicy.WIDTH)
                   .pageFling(true)
                   .pageSnap(true)
                   .load()

                leftButton.setOnClickListener {
                    jumpTo(currentPage - 1,true)
                }
                rightButton.setOnClickListener {
                    jumpTo(currentPage + 1,true)
                }
            }
        }
    }
}