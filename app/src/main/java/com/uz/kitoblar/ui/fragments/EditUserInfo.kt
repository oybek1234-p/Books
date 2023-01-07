package com.uz.kitoblar.ui.fragments

import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.EditUserInfoBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.toast

class EditUserInfoFragment: BaseFragment<EditUserInfoBinding>(R.layout.edit_user_info) {

    override fun onResume() {
        super.onResume()
        showKeyboard(requireBinding().nameView.editText)
    }

    override fun onViewCreated(binding: EditUserInfoBinding) {
        super.onViewCreated(binding)
        binding.apply {
            requireActionBar().apply {
                title = "Tahrirlash"
                addMenu(R.drawable.arrow_right).setOnClickListener {
                    val text = nameView.editText!!.text.ifEmpty {
                        shakeView(nameView)
                        toast("Ismingizni kiriting")
                        return@setOnClickListener
                    }.toString()
                    if (userLogged()) {
                        accountInstance.currentUserReference().child("name").setValue(text).addOnCompleteListener {
                            if (it.isSuccessful) {
                                closeLastFragment()
                            } else {
                                toast("Internetda ulanish kutilmoqda!")
                            }
                        }
                    }
                }
            }
            val name = currentUser().name
            nameView.editText!!.append(name)
        }
    }
}