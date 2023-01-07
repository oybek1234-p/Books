package com.uz.kitoblar.ui.fragments

import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.DevicePdfsFragmentBinding
import com.uz.kitoblar.launchOnBack
import com.uz.kitoblar.runOnUiThread
import com.uz.kitoblar.ui.components.actionBar.BaseFragment

open class DevicePdfsFragment : BaseFragment<DevicePdfsFragmentBinding>(R.layout.device_pdfs_fragment) {

    private val pdfPattern = ".pdf"
    private var pdfFilesPath = arrayListOf<String>()

    override fun onViewCreated(binding: DevicePdfsFragmentBinding) {
        super.onViewCreated(binding)

        requireActionBar().title = "Kitoblar"

        launchOnBack {
            runOnUiThread({

            })
        }

    }
}