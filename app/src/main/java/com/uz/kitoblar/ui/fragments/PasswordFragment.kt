package com.uz.kitoblar.ui.fragments

import android.view.View
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.PasswordFragmentBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.visibleOrGone

class PasswordFragment(val email: String, val isRegistered: Boolean = false) :
    BaseFragment<PasswordFragmentBinding>(R.layout.password_fragment) {

    override fun getActionBar(): View? {
        return null
    }

    private var loading = false

    private fun showProgress(show: Boolean) {
        loading = show
        requireBinding().apply {
            progessBar.visibleOrGone(show)
            loginButton.visibleOrGone(!show, invisible = true)
        }
    }

    override fun onResume() {
        super.onResume()
        showKeyboard(requireBinding().passwordView)
    }

    override fun onViewCreated(binding: PasswordFragmentBinding) {
        super.onViewCreated(binding)

        binding.apply {
            progessBar.visibleOrGone(false)
            loading = false

            textView2.text = email
            backButton.setOnClickListener {
                closeLastFragment()
            }
            loginButton.setOnClickListener {
                if (loading) return@setOnClickListener
                val password = passwordView.editText!!.text.toString()
                if (password.isEmpty()) {
                    shakeView(passwordView)
                    return@setOnClickListener
                }
                showProgress(true)
            }
        }
    }
}
