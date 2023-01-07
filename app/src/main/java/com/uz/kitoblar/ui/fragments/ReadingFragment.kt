package com.uz.kitoblar.ui.fragments

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.ReadingFragmentBinding
import com.uz.kitoblar.databinding.UserBookItemBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.components.actionBar.showPopup
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.controllers.MyBook
import com.uz.kitoblar.ui.components.list.BaseAdapter
import com.uz.kitoblar.ui.fragments.sheets.AddBookChooser
import com.uz.kitoblar.ui.myBooksLottie
import com.uz.kitoblar.ui.setData
import com.uz.kitoblar.ui.visibleOrGone
import com.uz.kitoblar.utils.DownloadController

class ReadingFragment : BaseFragment<ReadingFragmentBinding>(R.layout.reading_fragment) {

    private lateinit var myAdapter: Adapter

    private fun updateList(checkEmpty: Boolean) {
        val myBooks = ArrayList(booksController().myBooks.sortedByDescending { it.lastReadTime })
        myAdapter.updateList(myBooks)
        if (checkEmpty) {
            requireBinding().emptyView.root.visibleOrGone(myBooks.isEmpty())
        }
    }

    inner class Adapter :
        BaseAdapter<MyBook, UserBookItemBinding>(R.layout.user_book_item) {

        override fun holderCreated(holder: BaseViewHolder<UserBookItemBinding>, itemType: Int) {
            super.holderCreated(holder, itemType)
            holder.apply {
                if (itemType == R.layout.user_book_item) {
                    binding.apply {
                        shareView.setOnClickListener {
                            val item = currentList[adapterPosition] as MyBook
                            val book = Book().apply {
                                this.id = item.id
                                this.name = item.name
                                this.photo = item.photo
                                this.about = item.about
                            }
                            shareBook(it,book)
                        }
                        optionsView.setOnClickListener {
                            showPopup(it) {
                                addItem(0, "O'chirish", R.drawable.ic_delete) {
                                    val item = currentList[adapterPosition]
                                    booksController().deleteBookFromLibrary(item.id) {
                                        showSnackBar("O'chirildi!")
                                        updateList(true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        override fun bind(
            holder: BaseViewHolder<UserBookItemBinding>,
            model: MyBook
        ) {
            super.bind(holder, model)
            holder.apply {
                binding.apply {
                    photoView.cornerRadius = dp(12).toFloat()
                    photoView.setData(model.photo)
                    playView.visibleOrGone(model.musicUrl.isNotEmpty())
                    aboutView.text = "${model.categoryName} ${model.authorName}"
                    nameView.text = model.name
                    infoView.text = "${model.authorName} "

                    //Check offline
                    val offline = DownloadController.checkOffline(model.musicUrl)
                    offlineView.visibleOrGone(offline)
                }
            }
        }
    }

    override fun getActionBar(): View? {
        return null
    }

    private fun loadList() {
        requireBinding().apply {
            if (currentUserId().isNotEmpty()) {
                val controller = booksController()
                if (!controller.myBooksGot) {
                    if (controller.myBooks.isEmpty()) {
                        progressBar.visibleOrGone(true)
                    }
                    emptyView.root.visibleOrGone(false)
                    controller.loadMyBooks {
                        progressBar.visibleOrGone(false)
                        updateList(true)
                    }
                } else {
                    updateList(true)
                }
            } else {
                updateList(true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadList()
    }

    override fun onViewCreated(binding: ReadingFragmentBinding) {
        lightStatusBar = true

        binding.apply {
            likesButton.setOnClickListener {
                openFragment(LikedBooksFragment())
            }
            addBookBotton.setOnClickListener {
                requireLogin { mainActivity().sheetController.showSheet(AddBookChooser()) }
            }
            emptyView.apply {
                setData(
                    myBooksLottie,
                    "Mening kitoblarim",
                    "Siz saqlagan kitoblar shu yerda ko'rinadi",
                    if (userLogged()) "Kitob qo'shish" else "Akkauntga kirish"
                ) {
                    //So empty
                    if (currentUserId().isEmpty()) {
                        openFragment(MobileNumberFragment())
                    } else {
                        //Open home
                        mainActivity().openHome()
                    }
                }
            }
            recyclerView.apply {
                adapter = Adapter().apply {
                    myAdapter = this
                    clickListener = {
                        val book = currentList[it.layoutPosition]
                        openFragment(DetailsFragment(book.id))
                    }
                }
                layoutManager = LinearLayoutManager(requireContext())
                updateList(false)
            }
            progressBar.visibleOrGone(false)
        }
    }
}