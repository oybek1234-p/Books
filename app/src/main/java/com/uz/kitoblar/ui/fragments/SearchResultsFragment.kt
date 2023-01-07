package com.uz.kitoblar.ui.fragments

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.SearchBookItemBinding
import com.uz.kitoblar.databinding.SearchResultsFragmentBinding
import com.uz.kitoblar.dp
import com.uz.kitoblar.statusBarHeight
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.components.actionBar.showPopup
import com.uz.kitoblar.ui.controllers.AlgoliaSearch
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.components.list.BaseAdapter
import com.uz.kitoblar.ui.myBooksLottie
import com.uz.kitoblar.ui.setData
import com.uz.kitoblar.ui.toast
import com.uz.kitoblar.ui.visibleOrGone

class SearchResultsFragment(val text: String,val new: Boolean = false) :
    BaseFragment<SearchResultsFragmentBinding>(R.layout.search_results_fragment) {

    data class List(var lastPage: Int,var arrayList: ArrayList<Book>)

    companion object {

        fun getList(text: String): List {
            return hashMap.getOrPut(text) { List(0, arrayListOf()) }
        }

        private var hashMap = hashMapOf<String, List>()
    }

    override fun getActionBar(): View? {
        return null
    }

    inner class MyAdapter : BaseAdapter<Book, SearchBookItemBinding>(R.layout.search_book_item) {

        override fun bind(holder: BaseViewHolder<SearchBookItemBinding>, model: Book) {
            holder.apply {
                binding.apply {
                    imageViewLoader.setData(model.photo)
                    titleView.text = model.name
                    infoView.text = model.getInfo()
                    authorNameView.text = model.getInfo()
                    aboutView.text = model.about
                }
                if (layoutPosition == currentList.size - 1) {
                    loadMore()
                }
            }
        }

        override fun holderCreated(holder: BaseViewHolder<SearchBookItemBinding>, itemType: Int) {
            holder.apply {
                binding.apply {
                    imageViewLoader.cornerRadius = dp(8).toFloat()
                    root.setOnClickListener {
                        openFragment(DetailsFragment(getItem(layoutPosition).id))
                    }
                }
            }
        }
    }

    private lateinit var myAdapter: MyAdapter

    private var currentList = getList(text).arrayList

    private var page : Int
    set(value) {
        getList(text).lastPage = value
    }
    get() {
        return getList(text).lastPage
    }

    private var allPage = Int.MAX_VALUE

    private var loading = false

    private fun showEmpty(s: Boolean) = requireBinding().emptyView.root.visibleOrGone(s, 1)

    fun loadMore() {
        if (page == allPage) return
        if (loading) return
        showLoading(true)
        showEmpty(false)
        AlgoliaSearch.searchBooks(
            if (new) "" else text,
            page,
            12,
            false
        ) { isSuccess: Boolean, result: ArrayList<Book>?, page: Int ->
            showLoading(false)
            if (result != null && isSuccess) {
                allPage = page
                currentList.addAll(result)
                this.page += 1
            }
            updateList(true)
        }
    }

    private fun showLoading(show: Boolean) {
        if (loading == show) return
        loading = show
        val next = currentList.isNotEmpty()
        myAdapter.loading = next && loading
        requireBinding().progressBar.visibleOrGone(!next && loading, 1)
        R.layout.books_feed_layout_medium
    }

    private fun updateList(checkEmpty: Boolean) {
        myAdapter.updateList(currentList)
        if (checkEmpty) {
            val empty = currentList.isEmpty()
            showEmpty(empty)
        }
    }

    override fun onViewCreated(binding: SearchResultsFragmentBinding) {
        super.onViewCreated(binding)
        binding.apply {
            searchContainer.setPadding(0, statusBarHeight,0,0)
            backButton.setOnClickListener {
                closeLastFragment()
            }
            searchIcon.setOnClickListener {
                showPopup(it) {
                    addItem(0, "Report", R.drawable.arrow_right) {
                        toast("Please type your report")
                    }
                }
            }
            editText.apply {
                isClickable = false
                isFocusableInTouchMode = false
                text = this@SearchResultsFragment.text
            }
            searchContainer.setOnClickListener {
                closeLastFragment()
            }
            emptyView.apply {
                actionButton.visibleOrGone(false)
                setData(myBooksLottie, "Kitob topilmadi", "Boshqa so'zlarni ishlatib ko'ring", "")
                root.visibleOrGone(false)
            }
            recyclerView.apply {
                adapter = MyAdapter().also {
                    myAdapter = it
                }
                layoutManager = LinearLayoutManager(requireContext())
                progressBar.visibleOrGone(false)
                updateList(false)
            }
            loadMore()
        }

    }
}