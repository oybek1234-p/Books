package com.uz.kitoblar.ui.fragments

import androidx.core.widget.doOnTextChanged
import com.google.firebase.auth.PhoneAuthProvider
import com.uz.kitoblar.*
import com.uz.kitoblar.databinding.CodeVerifyFragmentBinding
import com.uz.kitoblar.ui.*
import com.uz.kitoblar.ui.components.actionBar.BaseFragment

class CodeVerifyFragment(val mobileNum: Long, val verificId: String) :
    BaseFragment<CodeVerifyFragmentBinding>(R.layout.code_verify_fragment) {
    private var limit = 60 * 2

    private var timerRunnable = Runnable {
        limit -= 1
        updateCountDown()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelRunOnUiThread(timerRunnable)
    }

    private fun updateCountDown() {
        requireBinding().countDown.apply {
            text = "$limit sekund qoldi"
            if (limit == 0) {
                closeLastFragment()
            }
            runOnUiThread(timerRunnable, 1000)
        }
    }

    private var loading = false

    fun showLoading(show: Boolean) {
        loading = show
        requireBinding().apply {
            progressBar.visibleOrGone(show)
            sendCodeButton.visibleOrGone(!show, invisible = true)
        }
    }

    override fun onResume() {
        super.onResume()
        showKeyboard(requireBinding().editText.editText)
    }

    fun doLogin() {
        requireBinding().apply {
            if (loading) return
            showLoading(true)
            val code = editText.editText!!.text.toString()
            if (code.isEmpty()) {
                shakeView(editText)
                toast("Kodni tering")
                return
            }
            if (verificId.isEmpty()) {
                toast("Verification is empty")
                return
            }
            val authResult = PhoneAuthProvider.getCredential(verificId, code)
            accountInstance.signInWithPhoneAuthCredential("", authResult) { s, new ->
                showLoading(false)
                if (s) {
                    if (new) {
                        openFragment(GetUserInfoFragment("", ""), true)
                    } else {
                        //User already exists
                        closeLastFragment()
                        closeLastFragment()
                    }
                }
            }
        }
    }

    override fun onViewCreated(binding: CodeVerifyFragmentBinding) {
        super.onViewCreated(binding)
        requireActionBar().apply {
            title = "Mobil nomer"
        }
        binding.apply {
            lottie.loadLottie(passwordPage)
            textView7.text = "Kod $mobileNum ga yuborildi"

            updateCountDown()

            editText.editText!!.doOnTextChanged { text, start, before, count ->
                if (text!=null && text.length == 6) {
                    doLogin()
                }
            }
            sendCodeButton.setOnClickListener {
                doLogin()
            }
        }
    }
}