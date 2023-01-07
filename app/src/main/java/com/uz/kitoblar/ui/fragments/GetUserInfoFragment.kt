package com.uz.kitoblar.ui.fragments

import android.content.res.ColorStateList
import android.view.View
import com.google.android.material.chip.Chip
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.GetUserInfoFragmentBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.controllers.BooksController
import com.uz.kitoblar.ui.toast
import com.uz.kitoblar.ui.visibleOrGone

class GetUserInfoFragment(
    private val email: String,
    private val password: String
) :
    BaseFragment<GetUserInfoFragmentBinding>(R.layout.get_user_info_fragment) {

    private val allGenres = BooksController.getInstance().genres
    private var interests = arrayListOf<Int>()

    override fun getActionBar(): View? {
        return null
    }

    override fun onResume() {
        super.onResume()
        showKeyboard(requireBinding().textInputLayout.editText)
    }

    private var chipBackground = 0

    private fun addAllGenres() {
        requireBinding().apply {
            allGenres.forEachIndexed { index, genre ->
                val chip = Chip(context).apply {
                    id = index
                    textSize = 15f
                    setTextColor(getThemeColor(R.attr.colorOnSurfaceHigh))
                    chipBackgroundColor = ColorStateList.valueOf(chipBackground)
                    text = genre.name
                    isCheckable = true
                }
                interestsChip.addView(chip)
            }
        }
    }

    private fun showProgress(show: Boolean) {
        logging = show
        requireBinding().apply {
            progessBar.visibleOrGone(show)
            loginButton.visibleOrGone(!show, invisible = true)
        }
    }

    var logging = false

    override fun onViewCreated(binding: GetUserInfoFragmentBinding) {
        super.onViewCreated(binding)

        binding.apply {

            chipBackground = requireContext().getThemeColor(R.attr.colorBackground)

            addAllGenres()

            backButton.setOnClickListener {
                closeLastFragment()
            }
            interestsChip.setOnCheckedStateChangeListener { group, checkedIds ->
                interests = ArrayList(checkedIds)
            }
            loginButton.setOnClickListener {
                if (logging) return@setOnClickListener
                val name = textInputLayout.editText!!.text.toString()
                if (name.isEmpty()) {
                    shakeView(textInputLayout)
                    toast("Ismingizni kiriting")
                    return@setOnClickListener
                }
                if (interests.isEmpty()) {
                    toast("Kamida 1ta janr tanlang")
                    return@setOnClickListener
                }
                val list = interests.map { allGenres[it] }
                if (list.isEmpty()) {
                    throw Exception("Interests is empty")
                }
                showProgress(true)
                val onDone = {
                    closePreviousFragment()
                    closeLastFragment()
                }
                if (email.isEmpty()) {
                    accountInstance.configurePhoneSignUser(name, ArrayList(list)) {
                        showProgress(false)
                        if (it) {
                            onDone.invoke()
                        } else {
                            toast("Internetga ulanish kutilmoqda !")
                        }
                    }
                } else {
                    accountInstance.createUser(name,email,password, ArrayList(list)) {
                        showProgress(false)
                        if (it) {
                            onDone.invoke()
                        } else {
                            toast("Qayta urinib koring!")
                        }
                    }
                }
            }
        }
    }
}