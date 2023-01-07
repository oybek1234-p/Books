package com.uz.kitoblar.ui.fragments

import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.AddBookFragmentBinding
import com.uz.kitoblar.databinding.InfoValueItemBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.components.actionBar.BaseViewHolder
import com.uz.kitoblar.ui.components.actionBar.RecyclerItemClickListener
import com.uz.kitoblar.ui.components.actionBar.SimpleAdapter
import com.uz.kitoblar.ui.controllers.*
import com.uz.kitoblar.ui.components.list.BaseAdapter
import com.uz.kitoblar.ui.components.list.MyLinearLayoutManager
import com.uz.kitoblar.ui.fragments.sheets.AddBookPriceSheet
import com.uz.kitoblar.ui.fragments.sheets.ChooseBookTypeSheet
import com.uz.kitoblar.ui.toast
import com.uz.kitoblar.ui.visibleOrGone
import com.uz.kitoblar.utils.formatCurrency

class AddBookFragment : BaseFragment<AddBookFragmentBinding>(R.layout.add_book_fragment) {
    var book = Book()

    private var uploading = false
        set(value) {
            field = value
            requireBinding().progressBar.visibleOrGone(value, 1)
            requireBinding().upload.visibleOrGone(!value, 1)
        }

    private var photoUploading = false
        set(value) {
            field = value
            requireBinding().photoProgressView.visibleOrGone(value, 1)
        }

    private var priceList = arrayListOf<Cost>()
    private var pricesCount = BookType.values().size

    private var priceAdapter: SimpleAdapter<InfoValueItemBinding,Cost>? = null

    private fun updatePhoto() {
        requireBinding().photoView.apply {
            cornerRadius = dp(8f).toFloat()
            val hasPhoto = book.photo.isNotEmpty()
            if (hasPhoto) {
                setPadding(0)
                setData(book.photo)
            } else {
                setPadding(dp(38))
                setImageResource(R.drawable.add_photo_ic)
            }
        }
    }

    private fun updateCategory() {
        requireBinding().categoryView.apply {
            book.category.apply {
                val hasCat = name.isNotEmpty()
                textView.text = if (hasCat) name else "Kategoriya tanlash"
                iconView.apply {
                    circleCrop = true
                    setPadding(if (hasCat) 0 else dp(8))
                    if (hasCat) {
                        setData(photo)
                    } else {
                        setImageResource(R.drawable.ic_add_box)
                    }
                }
            }
        }
    }

    private var isPart = false
    private var parentBookId = ""

    override fun onViewCreated(binding: AddBookFragmentBinding) {
        super.onViewCreated(binding)
        binding.apply {
            requireActionBar().title = "Yangi kitob"

            val photoRun = {
                openFragment(GalleryFragment({
                    val path = it.first()
                    book.photo = path
                    updatePhoto()
                    photoUploading = true
                    uploadImage(requireContext(), path) { url ->
                        photoUploading = false
                        book.photo = url ?: ""
                        updatePhoto()
                    }
                }, 1))
            }
            photoView.setOnClickListener { photoRun.invoke() }
            photoEdit.setOnClickListener { photoRun.invoke() }

            categoryView.root.setOnClickListener {
                openFragment(CategoriesFragment({
                    book.category = it
                    updateCategory()
                }, false))
            }
            updateCategory()

            if (book.name.isNotEmpty()) {
                isPart = true
                parentBookId = book.id
                book.id = randomId()

                partTitle.visibleOrGone(true)
                partEditText.apply {
                    visibleOrGone(true)
                    editText!!.append("0")
                }
                nameView.editText!!.append(book.name)
                authorNameView.editText!!.append(book.author.name)
                updatePhoto()
            }
            priceListView.apply {
                priceAdapter = object: SimpleAdapter<InfoValueItemBinding,Cost>(R.layout.info_value_item){
                    override fun bind(
                        holder: BaseViewHolder<InfoValueItemBinding>,
                        position: Int,
                        model: Cost
                    ) {
                        holder.binding.apply {
                            nameView.text = getBookTypeText(model.bookType)
                            valueView.text = formatCurrency(model.price)
                        }
                    }

                    override fun onViewHolderCreated(
                        holder: BaseViewHolder<InfoValueItemBinding>,
                        type: Int
                    ) {
                        super.onViewHolderCreated(holder, type)
                        holder.apply {
                            this.binding.deleteButton.setOnClickListener {
                                val item = currentList[layoutPosition]
                                priceList.remove(item)
                                notifyItemRemoved(layoutPosition)
                                addPriceButton.visibleOrGone(true)
                            }
                        }
                    }
                }.apply {
                    setDataList(priceList)
                    clickListener = object : RecyclerItemClickListener{
                        override fun onClick(position: Int, viewType: Int) {
                            val item = currentList[position]
                            openSheet(AddBookPriceSheet(item.bookType) { p->
                                priceList[position].price = p
                                priceAdapter?.notifyItemChanged(position)
                            }.apply {
                                price = item.price
                            })
                        }
                    }
                }
                adapter = priceAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }
            addPriceButton.setOnClickListener {
                var price: Long
                var type: BookType
                openSheet(ChooseBookTypeSheet(priceList) {
                    type = it
                    runOnUiThread(400) {
                        openSheet(AddBookPriceSheet(it) {
                            price = it
                            val bookPrice = Cost().apply {
                                this.price = price
                                this.bookType = type
                            }
                            priceList.add(bookPrice)
                            priceAdapter?.notifyDataSetChanged()

                            if (pricesCount == priceList.size) {
                                addPriceButton.visibleOrGone(false)
                            }
                        })
                    }
                })
            }
            upload.setOnClickListener {
                if (uploading) return@setOnClickListener
                val name = nameView.editText!!.text.ifEmpty {
                    toast("Nomini kiriting")
                    shakeView(nameView)
                    return@setOnClickListener
                }.toString()
                val authorName = authorNameView.editText!!.text.ifEmpty {
                    toast("Avtorni kiriting")
                    shakeView(authorNameView)
                    return@setOnClickListener
                }.toString()
                val about = aboutView.editText!!.text.ifEmpty {
                    toast("Malumot bering")
                    shakeView(aboutView)
                    return@setOnClickListener
                }.toString()

                if (photoUploading) {
                    toast("Rasm yuklanyapti")
                    return@setOnClickListener
                }
                if (book.photo.isEmpty()) {
                    toast("Rasmni yuklang")
                    shakeView(photoView)
                    return@setOnClickListener
                }
                if (book.category.name.isEmpty()) {
                    toast("Kategoriyani belgilang")
                    shakeView(categoryView.root)
                    return@setOnClickListener
                }
                val pdfUrl = pdfView.editText!!.text.toString()
                val musicUrl = musicView.editText!!.text.toString()

                if (musicUrl.isNotEmpty()) {
                    if (!MusicController.checkMp3Url(musicUrl)) {
                        toast("Music .mp3 formatida bulishi shart")
                        shakeView(musicView)
                        return@setOnClickListener
                    }
                }
                if (pdfUrl.isEmpty() && musicUrl.isEmpty()) {
                    toast("Pdf yoki music yuklang!")
                    shakeView(pdfView)
                    shakeView(musicView)
                    return@setOnClickListener
                }
                val yUrl = youTubeView.editText!!.text.toString()

                book.apply {
                    this.id = randomId()
                    this.name = name
                    this.about = about
                    this.pdfUrl = pdfUrl
                    this.musicUrl = musicUrl
                    this.publisherId = currentUserId()
                    this.authorName = authorName
                    this.youTubeUrl = yUrl
                    if (partEditText.isVisible) {
                        val part = partEditText.editText!!.text.ifEmpty {
                            toast("Nechinchi qism?")
                            shakeView(partEditText)
                            return@setOnClickListener
                        }.toString()
                        if (part.toInt() == 0) {
                            toast("Qism 0 chi bulishi mumkin emas")
                            shakeView(partEditText)
                            return@setOnClickListener
                        }
                        this.part = part.toInt()
                    }
                    author.id = currentUserId()
                    this.parentBook = parentBookId
                    author.name = currentUser().name
                    author.photo = currentUser().photo
                    author.books = currentUser().books
                    author.phone = currentUser().number
                    uploading = true

                    if (parentBookId.isNotEmpty()) {
                        booksController().apply {
                            booksReference.document(parentBookId)
                                .update(Book::parentBook.name, parentBookId)
                            books.find { it.id == parentBookId }?.parentBook = parentBookId
                        }
                    }
                    val addRun = {
                        AlgoliaSearch.addBook(this) {}
                        val isFree = priceList.isEmpty()
                        if (!isFree) {
                            priceList.forEach {
                                it.bookId = this.id
                            }
                            booksController().addCosts(priceList) {
                                toast("Prices added")
                            }
                            book.cost = priceList.first()
                        }
                        booksController().addNewBook(this) {
                            uploading = false
                            toast("Kitob yuklandi!")
                            closeLastFragment()
                        }
                    }
                    if (musicUrl.isNotEmpty()) {
                        MusicController.getMusicDuration(requireContext(), musicUrl) {
                            this.duration = it
                            addRun.invoke()
                        }
                    } else {
                        addRun.invoke()
                    }
                }
            }
        }
    }

}