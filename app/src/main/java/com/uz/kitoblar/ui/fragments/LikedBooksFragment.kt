package com.uz.kitoblar.ui.fragments

import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.kitoblar.R
import com.uz.kitoblar.booksController
import com.uz.kitoblar.databinding.LikedBookFragmentBinding
import com.uz.kitoblar.databinding.LikedBookItemBinding
import com.uz.kitoblar.shareBook
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.components.actionBar.showPopup
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.controllers.BooksController
import com.uz.kitoblar.ui.components.list.BaseAdapter
import com.uz.kitoblar.ui.visibleOrGone

class LikedBooksFragment : BaseFragment<LikedBookFragmentBinding>(R.layout.liked_book_fragment) {
    private lateinit var mAdapter: BaseAdapter<BooksController.Like, LikedBookItemBinding>

    fun updateList(empty: Boolean) {
        val list = booksController().getLikes()
        mAdapter.updateList(list)
        if (empty) {
            requireBinding().textView.visibleOrGone(list.isEmpty())
        }
    }

    override fun onViewCreated(binding: LikedBookFragmentBinding) {
        super.onViewCreated(binding)
        requireActionBar().title = "Yoqtirgan kitoblar"

        binding.apply {
            mAdapter = object :
                BaseAdapter<BooksController.Like, LikedBookItemBinding>(R.layout.liked_book_item) {
                override fun holderCreated(
                    holder: BaseViewHolder<LikedBookItemBinding>,
                    itemType: Int
                ) {
                    holder.apply {
                        this.binding.apply {
                            shareView.setOnClickListener {
                                val item = currentList[adapterPosition] as BooksController.Like
                                val book = Book().apply {
                                    this.id = item.id
                                    this.name = item.bookName
                                    this.photo = item.bookPhoto
                                    this.about = item.bookInfo
                                }
                                shareBook(it, book)
                            }
                            optionsView.setOnClickListener {
                                showPopup(it) {
                                    addItem(0, "O'chirish", R.drawable.ic_delete) {
                                        val item =
                                            currentList[adapterPosition] as BooksController.Like
                                        booksController().likeBook(Book().apply {
                                            id = item.bookId
                                        }, false) {
                                            showSnackBar("O'chirildi!")
                                            updateList(true)
                                        }
                                    }
                                }
                            }

                        }
                    }
                }

                override fun bind(
                    holder: BaseViewHolder<LikedBookItemBinding>,
                    model: BooksController.Like
                ) {
                    holder.apply {
                        this.binding.apply {
                            photoView.setData(model.bookPhoto)
                            nameView.text = model.bookName
                            aboutView.text = model.bookInfo
                        }
                    }
                }
            }
            recyclerView.adapter = mAdapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            if (booksController.likesLoaded) {
                updateList(true)
                progessBar.visibleOrGone(false)
            } else {
                progessBar.visibleOrGone(true)
                booksController.loadLikedBooks {
                    progessBar.visibleOrGone(false)
                    updateList(true)
                }
            }
        }
    }
}