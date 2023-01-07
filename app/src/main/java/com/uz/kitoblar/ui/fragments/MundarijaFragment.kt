package com.uz.kitoblar.ui.fragments

import androidx.recyclerview.widget.LinearLayoutManager
import com.uz.kitoblar.R
import com.uz.kitoblar.booksController
import com.uz.kitoblar.currentUserId
import com.uz.kitoblar.databinding.MundarijaFragmentBinding
import com.uz.kitoblar.databinding.MundarijaItemBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.controllers.BooksController
import com.uz.kitoblar.ui.components.list.BaseAdapter
import com.uz.kitoblar.ui.visibleOrGone

open class MundarijaAdapter(val bet: Boolean,val delete: Boolean = false,val deleteR: ((id: String) -> Unit)?=null): BaseAdapter<BooksController.Mundarija, MundarijaItemBinding>(R.layout.mundarija_item) {
    override fun holderCreated(holder: BaseViewHolder<MundarijaItemBinding>, itemType: Int) {
        super.holderCreated(holder, itemType)
        holder.binding.apply {
            if (delete) {
                deleteItem.visibleOrGone(true)
                deleteItem.setOnClickListener {
                    val item = getItem(holder.layoutPosition) as BooksController.Mundarija
                    booksController().deleteMundarija(item) {
                        deleteR?.invoke(item.id)
                    }
                }
            }
        }
    }
    override fun bind(
        holder: BaseViewHolder<MundarijaItemBinding>,
        model: BooksController.Mundarija
    ) {
        super.bind(holder, model)
        holder.apply {
            binding.apply {
                textView.text = model.name
                infoView.text = model.betOrMinute.toString() + if (bet) "-bet" else "-minut"
            }
  }
    }
}

class MundarijaFragment(val book: Book) : BaseFragment<MundarijaFragmentBinding>(R.layout.mundarija_fragment) {

    private var mundarijalar = arrayListOf<BooksController.Mundarija>()

    private fun update() {
        baseAdapter.updateList(mundarijalar)
    }

    private var baseAdapter = MundarijaAdapter(!book.musicOrPdf(),(currentUserId() == book.publisherId)) { s->
        mundarijalar.removeIf {it.id == s}
        update()
    }

    override fun onViewCreated(binding: MundarijaFragmentBinding) {
        super.onViewCreated(binding)
        requireActionBar().apply {
            title = book.name
            if (currentUserId() == book.publisherId) {
                addMenu(R.drawable.ic_add_box).apply {
                    setOnClickListener {
                        openFragment(AddMundarijaFragment(book) {
                            mundarijalar.add(it)
                            baseAdapter.updateList(mundarijalar)
                        })
                    }
                }
            }
        }
        binding.apply {
            recyclerView.apply {
                adapter = baseAdapter
                layoutManager = LinearLayoutManager(requireContext())

                booksController().mundarijalar[book.id]?.let {
                    mundarijalar.addAll(it)
                }
                if (mundarijalar.isEmpty()) {
                    progessBar.visibleOrGone(true)
                    booksController().loadMundarijalar(book.id,object :
                        com.uz.kitoblar.ui.controllers.Result<ArrayList<BooksController.Mundarija>>() {
                        override fun onResult(result: ArrayList<BooksController.Mundarija>) {
                            progessBar.visibleOrGone(false,1)
                            mundarijalar = result
                            baseAdapter.updateList(mundarijalar)
                        }
                    })
                } else {
                    baseAdapter.updateList(mundarijalar)
                }
            }
        }
    }
}