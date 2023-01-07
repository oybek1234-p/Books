package com.uz.kitoblar.ui.fragments

import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.UserProfileBinding
import com.uz.kitoblar.databinding.UserReadBooksBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.controllers.MyBook
import com.uz.kitoblar.ui.components.list.BaseAdapter
import com.uz.kitoblar.ui.visibleOrGone

class UserProfileFragment(private val user: User) :
    BaseFragment<UserProfileBinding>(R.layout.user_profile) {
    private lateinit var adapterR: BaseAdapter<MyBook, UserReadBooksBinding>

    override fun onViewCreated(binding: UserProfileBinding) {
        super.onViewCreated(binding)
        requireActionBar().apply {
            title = ""
        }
        binding.apply {
            photoView.apply {
                circleCrop = true
                setData(user.photo)
            }
            nameView.text = user.name
            readBooks.text = user.readingBooks.toString() + " kitob o'qilgan"
            levelView.text = user.level.toString() + " level"
            emptyView.visibleOrGone(false)
            sellerView.visibleOrGone(user.seller)
            sellerView.setOnClickListener {
              //  openFragment(AuthorBooksFragment(user.toAuthor()))
            }
            adapterR = object :
                BaseAdapter<MyBook, UserReadBooksBinding>(R.layout.user_read_books) {
                init {
                    clickListener = {
                        openFragment(DetailsFragment((currentList[it.layoutPosition] as MyBook).id))
                    }
                }

                override fun holderCreated(
                    holder: BaseViewHolder<UserReadBooksBinding>,
                    itemType: Int
                ) {
                    super.holderCreated(holder, itemType)
                    holder.apply {
                        this.binding.apply {
                            imageView2.cornerRadius = dp(8).toFloat()
                            shareView.setOnClickListener {

                            }
                        }
                    }
                }

                override fun bind(
                    holder: BaseViewHolder<UserReadBooksBinding>,
                    model: MyBook
                ) {
                    holder.apply {
                        this.binding.apply {
                            imageView2.setData(model.photo)
                            nameView.text = model.name
                        }
                    }
                }
            }
            recyclerView.adapter = adapterR
            recyclerView.layoutManager = LinearLayoutManager(context)

            progressBar.visibleOrGone(true)
            booksController().loadUserBooks(user.id,
                object :
                    com.uz.kitoblar.ui.controllers.Result<ArrayList<MyBook>>() {
                    override fun onResult(result: ArrayList<MyBook>) {
                        super.onResult(result)
                        progressBar.visibleOrGone(false)
                        adapterR.updateList(result)
                        if (result.isEmpty()) {
                            emptyView.visibleOrGone(true, 1)
                        }
                    }
                })
        }
    }
}