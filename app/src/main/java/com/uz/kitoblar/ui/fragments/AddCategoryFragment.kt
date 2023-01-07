package com.uz.kitoblar.ui.fragments

import androidx.core.view.setPadding
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.FragmentAddCategoryBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.controllers.Category
import com.uz.kitoblar.ui.toast
import com.uz.kitoblar.ui.visibleOrGone
import com.uz.kitoblar.utils.dp

class AddCategoryFragment :
    BaseFragment<FragmentAddCategoryBinding>(R.layout.fragment_add_category) {

    private var category = Category()

    override fun onResume() {
        super.onResume()
        requireBinding().editText.editText?.showKeyboard()
    }

    private fun updateImage() {
        requireBinding().photoView.apply {
            val url = category.photo
            if (url.isEmpty()) {
                setPadding(32.dp)
                setImageResource(R.drawable.add_photo_ic)
            } else {
                setPadding(0)
                setData(url)
            }
        }
    }

    private var photoLoading = false
    set(value) {
        field = value
        requireBinding().photoProgressView.visibleOrGone(value)
    }

    private var uploading = false
    set(value) {
        field = value
        requireBinding().apply {
            progressBar.visibleOrGone(value,1)
            uploadButton.visibleOrGone(!value,1, invisible = true)
        }
    }

    override fun onViewCreated(binding: FragmentAddCategoryBinding) {
        super.onViewCreated(binding)
        binding.apply {
            requireActionBar().title = "Yangi kategoriya"
            photoView.circleCrop = true
            photoView.setOnClickListener {
                openFragment(GalleryFragment({
                    val path = it.first()
                    category.photo = path
                    updateImage()
                    photoLoading = true
                    uploadImage(requireContext(), path) { url->
                        photoLoading = false
                        category.photo = url ?: ""
                        updateImage()
                    }
                }, 1))
            }
            uploadButton.setOnClickListener {
                if (uploading) return@setOnClickListener
                val name = editText.editText!!.text.ifEmpty {
                    shakeView(editText)
                    toast("Nomini kiriting")
                    return@setOnClickListener
                }.toString()
                category.name = name
                if (photoLoading) {
                    toast("Rasm yuklanyapti")
                    return@setOnClickListener
                }
                if (category.photo.isEmpty()) {
                    toast("Rasm yuklang")
                    return@setOnClickListener
                }
                uploading = true
                booksController().addNewCategory(category) {
                    uploading = false
                    closeLastFragment()
                }
            }
        }
    }
}