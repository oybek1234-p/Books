package com.uz.kitoblar.ui.fragments

import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.CommentsFragmentBinding
import com.uz.kitoblar.databinding.CommentsLayoutBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.controllers.BooksController
import com.uz.kitoblar.ui.controllers.Result
import com.uz.kitoblar.ui.components.list.BaseAdapter
import com.uz.kitoblar.ui.visibleOrGone

class CommentsFragment(val book: Book) : BaseFragment<CommentsFragmentBinding>(R.layout.comments_fragment) {

    private var adapterM = object : BaseAdapter<BooksController.Comment, CommentsLayoutBinding>(R.layout.comments_layout) {
        override fun bind(
            holder: BaseViewHolder<CommentsLayoutBinding>,
            model: BooksController.Comment
        ) {
            holder.binding.apply {
                imageView.circleCrop = true
                imageView.setData(model.userPhoto)
                nameView.text = model.userName
                textView16.text = model.comment
            }
        }
    }

    private fun loadComments() {
        val comments = booksController.getCommentsForBook(book.id)
        requireBinding().apply {
            if (comments.isNullOrEmpty()) {
                progessBar.visibleOrGone(true)
                booksController.loadComments(book.id,1,object : Result<ArrayList<BooksController.Comment>>(){
                    override fun onResult(result: ArrayList<BooksController.Comment>) {
                        progessBar.visibleOrGone(false)
                        adapterM.updateList(result)
                    }
                })
            } else {
                adapterM.updateList(comments)
            }
        }
    }

    override fun onViewCreated(binding: CommentsFragmentBinding) {
        super.onViewCreated(binding)
        binding.apply {
            requireActionBar().title = "Kommentlar"
            recyclerView.apply {
                adapter = adapterM
                layoutManager = LinearLayoutManager(requireContext())

                loadComments()
            }
        }
    }
}