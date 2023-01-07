package com.uz.kitoblar.ui.fragments

import com.uz.kitoblar.R
import com.uz.kitoblar.booksController
import com.uz.kitoblar.databinding.MundarijaAddBinding
import com.uz.kitoblar.shakeView
import com.uz.kitoblar.showKeyboard
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.controllers.Book
import com.uz.kitoblar.ui.controllers.BooksController
import com.uz.kitoblar.ui.visibleOrGone

class AddMundarijaFragment(
    val book: Book,
    val result: (mundarija: BooksController.Mundarija) -> Unit
) : BaseFragment<MundarijaAddBinding>(R.layout.mundarija_add) {

    override fun onResume() {
        super.onResume()
        showKeyboard(requireBinding().nameView.editText)
    }

    override fun onViewCreated(binding: MundarijaAddBinding) {
        super.onViewCreated(binding)

        binding.apply {
            requireActionBar().apply {
                title = book.name
                addMenu(R.drawable.ic_check).apply {
                    setOnClickListener {
                        val name = nameView.editText!!.text.toString().ifEmpty {
                            shakeView(nameView)
                            return@setOnClickListener
                        }
                        val value = valueView.editText!!.text.toString().ifEmpty {
                            shakeView(valueView)
                            return@setOnClickListener
                        }.toInt()
                        val mundarija = BooksController.Mundarija().apply {
                            this.name = name
                            this.betOrMinute = value
                        }
                        progessBar.visibleOrGone(true,1)
                        booksController().addMundarija(book.id,mundarija) {
                            progessBar.visibleOrGone(false,1)
                            result.invoke(mundarija)
                            closeLastFragment()
                        }
                    }
                }
            }
        }
    }
}