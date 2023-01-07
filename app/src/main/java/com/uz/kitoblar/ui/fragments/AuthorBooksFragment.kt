package com.uz.kitoblar.ui.fragments

import androidx.recyclerview.widget.GridLayoutManager
import com.uz.kitoblar.R
import com.uz.kitoblar.booksController
import com.uz.kitoblar.databinding.AuthorFragmentBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.controllers.Author
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.controllers.Model
import com.uz.kitoblar.ui.visibleOrGone

class AuthorBooksFragment(val author: Author) :
    BaseFragment<AuthorFragmentBinding>(R.layout.author_fragment) {
    private lateinit var adapterR: BooksFragment.InnerAdapter

    override fun onViewCreated(binding: AuthorFragmentBinding) {
        super.onViewCreated(binding)
        binding.apply {
            requireActionBar().title = "Sotuvchi"
            nameView.text = author.name
          //  subtitleView.text = author.books.toString() + " kitoblar"
            photoView.apply {
                circleCrop = true
                setData(author.photo)
            }
            adapterR =  BooksFragment.InnerAdapter(false)
            recyclerView.apply {
                adapter = adapterR.apply {
                    clickListener = {
                        val item = currentList[it.layoutPosition]
                        openFragment(DetailsFragment(item.id))
                    }
                }
                layoutManager = GridLayoutManager(context, 3)
            }
            progressBar.visibleOrGone(true)
            emptyView.visibleOrGone(false)
            booksController().loadBooks(
                publisherId = author.id,
                result = object : com.uz.kitoblar.ui.controllers.Result<ArrayList<Book>>() {
                    override fun onResult(result: ArrayList<Book>) {
                        super.onResult(result)
                        progressBar.visibleOrGone(false,1)
                        adapterR.submitList(result as ArrayList<Model>)
                        if (result.isEmpty()) {
                            emptyView.visibleOrGone(true,1)
                        }
                    }
                })
        }
    }
}