package com.uz.kitoblar.ui.fragments

import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.kitoblar.ui.controllers.Category
import com.uz.kitoblar.R
import com.uz.kitoblar.booksController
import com.uz.kitoblar.databinding.CategoriesFragmentBinding
import com.uz.kitoblar.databinding.CategoryItemBinding
import com.uz.kitoblar.handleError
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.components.actionBar.showPopup
import com.uz.kitoblar.ui.components.list.BaseAdapter
import com.uz.kitoblar.ui.visibleOrGone

class CategoriesFragment(
    val onSelected: ((category: Category) -> Unit)? = null,
    private val edit: Boolean = false
) :
    BaseFragment<CategoriesFragmentBinding>(R.layout.categories_fragment) {

    private var selectedPos = -1
    private var lastSelectedPos = -1

    inner class Adapter : BaseAdapter<Category, CategoryItemBinding>(R.layout.category_item) {

        init {
            if (onSelected != null) {
                clickListener = {
                    it.apply {
                        lastSelectedPos = selectedPos
                        selectedPos = layoutPosition
                        if (lastSelectedPos == selectedPos) {
                            return@apply
                        }
                        getViewHolder(lastSelectedPos)?.binding?.checkBox?.apply {
                            isChecked = false
                        }
                        getViewHolder(selectedPos)?.binding?.checkBox?.apply {
                            isChecked = true
                        }
                        onSelected.invoke(getItem(selectedPos) as Category)
                        closeLastFragment()
                    }
                }
            }
            if (edit) {
                onLongClickListener = { it ->
                    it.apply {
                        showPopup(itemView) {
                            addItem(0, "O'chirish", R.drawable.ic_delete) {
                                val item = getItem(layoutPosition)
                                booksController().deleteCategory(item.id) {
                                    if (!it) {
                                        handleError()
                                    }
                                }
                                loadList()
                            }
                        }
                    }
                }
            }
        }

        override fun bind(holder: BaseViewHolder<CategoryItemBinding>, model: Category) {
            holder.apply {
                binding.apply {
                    photoView.circleCrop = true
                    photoView.setData(model.photo)
                    nameView.text = model.name
                    checkBox.apply {
                        val checked = layoutPosition == selectedPos
                        visibleOrGone(onSelected != null)
                        isChecked = checked
                    }
                }
            }
        }
    }

    private var myAdapter: Adapter? = null

    override fun onResume() {
        super.onResume()
        loadList()
    }

    private fun loadList() {
        requireBinding().apply {
            progressBar.visibleOrGone(true, 1)
            booksController().loadCategories(false) {
                emptyView.visibleOrGone(it.isNullOrEmpty())
                progressBar.visibleOrGone(false)
                myAdapter?.updateList(it)
            }
        }
    }

    override fun onViewCreated(binding: CategoriesFragmentBinding) {
        super.onViewCreated(binding)
        requireActionBar().apply {
            title = "Kategoriyalar"
            if (edit) {
                addMenu(R.drawable.ic_add_box).setOnClickListener {
                    openFragment(AddCategoryFragment())
                }
            }
        }
        binding.apply {
            recyclerView.apply {
                adapter = Adapter().also { myAdapter = it }
                layoutManager = LinearLayoutManager(context)
            }
        }
    }
}