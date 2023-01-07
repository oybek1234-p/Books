package com.uz.kitoblar.ui.fragments

import com.github.vacxe.phonemask.PhoneMaskManager
import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.MobileNumberFragmentBinding
import com.uz.kitoblar.handleError
import com.uz.kitoblar.shakeView
import com.uz.kitoblar.showKeyboard
import com.uz.kitoblar.ui.*
import com.uz.kitoblar.ui.components.actionBar.BaseFragment

class MobileNumberFragment :
    BaseFragment<MobileNumberFragmentBinding>(R.layout.mobile_number_fragment) {

    override fun onResume() {
        super.onResume()
        showKeyboard(requireBinding().editText.editText)
    }

    private var loading = false

    private fun showLoading(show: Boolean) {
        loading = show
        requireBinding().apply {
            progressBar.visibleOrGone(show)
            sendCodeButton.visibleOrGone(!show, invisible = true)
        }
    }

    override fun onViewCreated(binding: MobileNumberFragmentBinding) {
        super.onViewCreated(binding)
        requireActionBar().apply {
            title = "Avtorizatsiya"
        }
        lightStatusBar = false
        binding.apply {
            lottie.apply {
                loadLottie(login)
                preloadLottie(passwordPage)
            }
            val m = PhoneMaskManager()
                .withMask(" (##) ###-##-##")
                .withRegion("+998")
                .withValueListener { }
                .bindTo(editText.editText)

            sendCodeButton.setOnClickListener {
                if (loading) return@setOnClickListener
                showLoading(true)
                val number = m.phone
                if (number.isEmpty()) {
                    shakeView(editText)
                    toast("Mobil nomerni kiriting")
                    return@setOnClickListener
                }
                if (number.length < 9) {
                    shakeView(editText)
                    toast("Raqam kodini tering")
                    return@setOnClickListener
                }
                accountInstance.sendVerificationCodeWithPhoneNumber(requireActivity(),number) { vid, error,ph ->
                    showLoading(false)
                    if (error != null) {
                        toast(error.message)
                        handleError(error)
                    }
                    if (error == null && vid != null) {
                        openFragment(CodeVerifyFragment(number.toLong(),vid), false)
                    }
                    if (error == null && vid == null && ph != null) {
                        accountInstance.signInWithPhoneAuthCredential("", ph) { s, new ->
                            showLoading(false)
                            if (s) {
                                if (new) {
                                    openFragment(GetUserInfoFragment("", ""), false)
                                } else {
                                    //User already exists ->

                                }
                            }
                        }
                    }
                    if (error == null && vid == null) {
                        openFragment(GetUserInfoFragment("",""),false)
                    }
                }
            }
        }
    }
}