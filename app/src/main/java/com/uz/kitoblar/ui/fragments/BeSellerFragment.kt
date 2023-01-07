package com.uz.kitoblar.ui.fragments

import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.FragmentBeSellerBinding
import com.uz.kitoblar.runOnUiThread
import com.uz.kitoblar.ui.*
import com.uz.kitoblar.ui.components.actionBar.BaseFragment

class BeSellerFragment : BaseFragment<FragmentBeSellerBinding>(R.layout.fragment_be_seller) {

    private var loading = false
    set(value) {
        field = value
        requireBinding().apply {
            progressBar.visibleOrGone(value,1)
            agreeButton.visibleOrGone(!value,1)
        }
    }

    override fun onViewCreated(binding: FragmentBeSellerBinding) {
        super.onViewCreated(binding)
        preloadLottie(youAreSeller)
        binding.apply {
            requireActionBar().title = "Sotuchi bo'lish"
            executePendingBindings()

            agreeButton.setOnClickListener {
                if (loading) return@setOnClickListener
                loading = true
                accountInstance.beSeller {
                    loading = false
                    if (it) {
                        runOnUiThread(400) {
                            openFragment(YouAreSellerFragment(),true)
                        }
                    }
                }
            }
        }
    }

}