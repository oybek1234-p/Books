package com.uz.kitoblar.ui.fragments.sheets

import com.uz.kitoblar.R
import com.uz.kitoblar.booksController
import com.uz.kitoblar.databinding.AddBookSheetBinding
import com.uz.kitoblar.runOnUiThread
import com.uz.kitoblar.ui.addInfoButton
import com.uz.kitoblar.ui.components.actionBar.bottomSheet.BottomSheetBase
import com.uz.kitoblar.ui.fragments.DevicePdfsFragment

class AddBookChooser : BottomSheetBase<AddBookSheetBinding>(R.layout.add_book_sheet) {

    override fun onViewCreated(binding: AddBookSheetBinding) {
        super.onViewCreated(binding)
        container().titleView.text = "Kitob qo'shish"
        binding.apply {
            container.apply {
                addInfoButton(R.drawable.add_book_ic,"Telefon xotirasidan qo'shish") {
                    openFragment(DevicePdfsFragment())
                    closeSheet()
                }
                addInfoButton(R.drawable.ic_add_box,"Magazindan qo'shish") {
                    mainActivity()?.openHome()
                    runOnUiThread({
                        closeSheet()
                    },200)
                }
            }
        }
    }
}