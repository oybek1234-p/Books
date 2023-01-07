package com.uz.kitoblar.ui.fragments.sheets

import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.ChooseBookTypeBinding
import com.uz.kitoblar.databinding.JustTextViewBinding
import com.uz.kitoblar.ui.components.actionBar.bottomSheet.BottomSheetBase
import com.uz.kitoblar.ui.controllers.BookType
import com.uz.kitoblar.ui.controllers.Cost
import com.uz.kitoblar.ui.controllers.Model
import com.uz.kitoblar.ui.controllers.getBookTypeText
import com.uz.kitoblar.ui.components.list.BaseAdapter

class ChooseBookTypeSheet(var selectedList: ArrayList<Cost>, val onDone: (type: BookType) -> Unit) :
    BottomSheetBase<ChooseBookTypeBinding>(R.layout.choose_book_type) {

    private val chooseList = arrayListOf<BookType>()

    data class TypeWrapper(val type: BookType) : Model()

    override fun onViewCreated(binding: ChooseBookTypeBinding) {
        binding.apply {
            container().titleView.text = binding.root.context.getString(R.string.kitob_tur_tanlang)
            BookType.values().forEach { type ->
                if (selectedList.find { it.bookType == type } == null) {
                    chooseList.add(type)
                }
            }
            recyclerView.apply {
                adapter = object :
                    BaseAdapter<TypeWrapper, JustTextViewBinding>(R.layout.just_text_view) {
                    override fun bind(
                        holder: BaseViewHolder<JustTextViewBinding>,
                        model: TypeWrapper
                    ) {
                        holder.binding.textView.text = getBookTypeText(model.type)
                    }
                }.apply {
                    clickListener = {
                        val item = currentList[it.layoutPosition]
                        onDone((item as TypeWrapper).type)
                        closeSheet()
                    }
                    updateList(chooseList.map { TypeWrapper(it) } as ArrayList<TypeWrapper>)
                }
                layoutManager = LinearLayoutManager(context)
            }
        }
    }

}