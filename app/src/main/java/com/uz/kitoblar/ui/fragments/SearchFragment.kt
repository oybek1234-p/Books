package com.uz.kitoblar.ui.fragments

import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.FragmentSearchBinding
import com.uz.kitoblar.databinding.SearchItemBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.controllers.AlgoliaSearch
import com.uz.kitoblar.ui.controllers.SearchBook
import com.uz.kitoblar.ui.controllers.SearchBookDao
import com.uz.kitoblar.ui.controllers.roomDatabase
import com.uz.kitoblar.ui.components.list.BaseAdapter
import com.uz.kitoblar.ui.myBooksLottie
import com.uz.kitoblar.ui.setData
import com.uz.kitoblar.ui.toast
import com.uz.kitoblar.ui.visibleOrGone
import kotlin.collections.set

class SearchFragment : BaseFragment<FragmentSearchBinding>(R.layout.fragment_search) {
    class SearchAdapter(private val onSelect: (searchBook: SearchBook) -> Unit) :
        BaseAdapter<SearchBook, SearchItemBinding>(R.layout.search_item) {

        override fun holderCreated(holder: BaseViewHolder<SearchItemBinding>, itemType: Int) {
            holder.apply {
                binding.root.setOnClickListener {
                    val item = getItem(layoutPosition) as SearchBook
                    onSelect.invoke(item)
                }
            }
        }

        override fun bind(holder: BaseViewHolder<SearchItemBinding>, model: SearchBook) {
            super.bind(holder, model)
            holder.binding.apply {
                textView.text = model.text
                photoView.apply {
                    visibleOrGone(model.photo.isNotEmpty())
                    setData(model.photo)
                }
            }
        }
    }

    override fun getActionBar(): View? {
        return null
    }

    private var searchRunnable: Runnable? = null

    private fun search() {
        cancelRunOnUiThread(searchRunnable)
        searchRunnable = Runnable {
            showEmpty(false)
            if (searchText.isNotEmpty()) {
                AlgoliaSearch.searchBooks(searchText, 0, 12, true) { isSuccess, result, page ->
                    if (isSuccess) {
                        if (result != null) {
                            searchList = ArrayList(result.map {
                                SearchBook().apply {
                                    id = it.id
                                    text = it.name
                                    bookId = it.id
                                    photo = it.photo
                                }
                            })
                            if (searchList.isNotEmpty()) {
                                searched[searchText] = searchList
                            }
                        } else {
                            searchList.clear()
                        }
                    } else {
                        searchList.clear()
                    }
                    updateList()
                }
            }
        }
        runOnUiThread(searchRunnable!!, 400)
    }

    private fun addToSearch(searchBook: SearchBook) {
        val cached = cacheList.find { it.id == searchBook.id }
        if (cacheList.size == 12 || cached != null) {
            val delete = cached ?: cacheList.first()
            searchDao.deleteSearch(delete)
            cacheList.remove(delete)
        }
        cacheList.add(0, searchBook)
        searchDao.addBookSearch(searchBook)
    }


    private fun openSearchResult(searchBook: SearchBook) {
        searchText = searchBook.text
        if (searchText.isNotEmpty()) {
            ignoreChange = true
            requireBinding().editText.setText(searchText)
            ignoreChange = false
        }
        addToSearch(searchBook)
        //Open search results
        openFragment(SearchResultsFragment(searchBook.text),true)
    }

    private var cacheList = arrayListOf<SearchBook>()
    private var searchList = arrayListOf<SearchBook>()
    private var searchText = ""
    private lateinit var searchDao: SearchBookDao

    private lateinit var myAdapter: SearchAdapter
    private var searched = hashMapOf<String, ArrayList<SearchBook>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlgoliaSearch.init()
        searchDao = roomDatabase().searchBookDao()
    }

    private fun showEmpty(s: Boolean) {
        requireBinding().emptyView.root.visibleOrGone(s, 1)
    }

    private fun updateList() {
        var checkEmpty = false
        val list: ArrayList<SearchBook> = if (searchText.isNotEmpty()) {
            checkEmpty = true
            searchList
        } else {
            cacheList
        }
        myAdapter.updateList(list)
        showEmpty(list.isEmpty() && checkEmpty)
    }

    override fun onResume() {
        super.onResume()
        showKeyboard(requireBinding().editText)
        startAnimation(true)
    }

    override fun onPause() {
        super.onPause()
        AlgoliaSearch.cancelSearch()
        startAnimation(false)
    }

    private var ignoreChange = false

    private fun openSearchResults() {
        if (searchText.isNotEmpty()) {
            val searchBook = SearchBook().apply { text = searchText }
            openSearchResult(searchBook)
        } else {
            toast("Nimadir yozing!")
        }
    }

    override fun onViewCreated(binding: FragmentSearchBinding) {

        binding.apply {
            searchContainer.setPadding(0, statusBarHeight, 0, 0)
            backButton.setOnClickListener {
                closeLastFragment()
            }

            searchIcon.setOnClickListener {
               openSearchResults()
            }
            emptyView.apply {
                actionButton.visibleOrGone(false)
                setData(myBooksLottie, "Kitob topilmadi", "Boshqa so'zlarni ishlatib ko'ring", "")
                root.visibleOrGone(false)
            }
            editText.doOnTextChanged { text, start, before, count ->
                if (ignoreChange) return@doOnTextChanged
                AlgoliaSearch.cancelSearch()
                cancelRunOnUiThread(searchRunnable)
                if (text.isNullOrEmpty()) {
                    searchText = ""
                    searchList.clear()
                    updateList()
                } else {
                    val searchT = text.toString()
                    if (searchT != searchText) {
                        searchText = searchT
                        val list = searched[searchT]
                        if (list != null) {
                            searchList = list
                            updateList()
                        } else {
                            search()
                        }
                    }
                }
            }
            editText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    openSearchResults()
                    return@setOnEditorActionListener  true
                }
                return@setOnEditorActionListener false
            }
            recyclerView.apply {
                myAdapter = SearchAdapter {
                    openSearchResult(it)
                }
                adapter = myAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }
            cacheList = ArrayList(searchDao.getAllSearches().asReversed())
            updateList()
        }
    }


    private fun startAnimation(open: Boolean) {
        requireBinding().apply {
            val toSearchOffset = -dp(150).toFloat()
            val secondOffset = dp(48f).toFloat()
            if (open) {
                searchContainer.translationY = toSearchOffset
                recyclerView.translationY = secondOffset
                recyclerView.alpha = 0f
            }
            searchContainer.animate().translationY(if(open) 0f else toSearchOffset).setInterpolator(if (open) decelerateInterpolator else AccelerateInterpolator(2f))
                .setDuration(300).start()

            recyclerView.animate().translationY(if (open) 0f else secondOffset).alpha(if (open) 1f else 0f).setInterpolator(
                decelerateInterpolator).setDuration(200).start()
        }
    }
}