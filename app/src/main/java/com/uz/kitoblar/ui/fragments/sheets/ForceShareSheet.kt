package com.uz.kitoblar.ui.fragments.sheets

import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.ForceShareSheetBinding
import com.uz.kitoblar.ui.components.actionBar.bottomSheet.BottomSheetBase

class ForceShareSheet : BottomSheetBase<ForceShareSheetBinding>(R.layout.force_share_sheet) {
    override fun onViewCreated(binding: ForceShareSheetBinding) {
        binding.apply {
            container().apply {
                titleView.text = "Do'stingizga ulashing"
            }
        }
    }
}