package com.uz.kitoblar.ui.fragments

import android.view.View
import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.LoginFragmentBinding
import com.uz.kitoblar.shakeView
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.visibleOrGone

class LoginFragment : BaseFragment<LoginFragmentBinding>(R.layout.login_fragment) {

    override fun getActionBar(): View? {
        return null
    }

    private var isRegistered = false
    private var loading = false

    private fun showProgress(show: Boolean) {
        loading = show
        requireBinding().apply {
            progessBar.visibleOrGone(show)
            loginButton.visibleOrGone(!show, invisible = true)
        }
    }

    override fun onViewCreated(binding: LoginFragmentBinding) {
        super.onViewCreated(binding)
        binding.apply {
            progessBar.visibleOrGone(false)

            backButton.setOnClickListener {
                closeLastFragment()
            }
            loginButton.setOnClickListener {
                if (loading) return@setOnClickListener
                loading = true
                val text = emailView.editText!!.text.toString()
                if (text.isEmpty()) {
                    shakeView(emailView)
                    return@setOnClickListener
                }
                //Check user is registered
                showProgress(true)
                accountInstance.isUserRegistered(text) { s, r ->
                    showProgress(false)
                    if (s) {
                        isRegistered = r
                        openFragment(PasswordFragment(text,isRegistered),false)
                    } else {
                        checkForError()
                    }
                }
            }
        }
    }
}