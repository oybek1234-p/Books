package com.uz.kitoblar.ui.fragments.sheets

import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.AddPriceLayoutBinding
import com.uz.kitoblar.shakeView
import com.uz.kitoblar.showKeyboard
import com.uz.kitoblar.ui.components.actionBar.bottomSheet.BottomSheetBase
import com.uz.kitoblar.ui.controllers.BookType
import com.uz.kitoblar.ui.controllers.getBookTypeText

class AddBookPriceSheet(val type: BookType, private val done: (price: Long) -> Unit) :
    BottomSheetBase<AddPriceLayoutBinding>(R.layout.add_price_layout) {

    var price = 0L

    override fun onResume() {
        super.onResume()
        showKeyboard(layoutBinding?.editText?.editText)
    }

    override fun onViewCreated(binding: AddPriceLayoutBinding) {
        binding.apply {
            container().titleView.text = "Narx belgilash"
            typeView.text = getBookTypeText(type)
            if (price!=0L) {
                editText.editText!!.append(price.toString())
            }
            addButton.setOnClickListener {
                val price = editText.editText!!.text!!.toString()
                if (price.isEmpty()) {
                    shakeView(editText)
                    return@setOnClickListener
                }
                done.invoke(price.toLong())
                closeSheet()
            }
        }
    }
}