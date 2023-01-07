package com.uz.kitoblar.ui.fragments

import com.uz.kitoblar.R
import com.uz.kitoblar.databinding.FragmentYouAreSellerBinding
import com.uz.kitoblar.ui.components.actionBar.BaseFragment
import com.uz.kitoblar.ui.loadLottie
import com.uz.kitoblar.ui.youAreSeller

class YouAreSellerFragment :
    BaseFragment<FragmentYouAreSellerBinding>(R.layout.fragment_you_are_seller) {

    override fun onViewCreated(binding: FragmentYouAreSellerBinding) {
        super.onViewCreated(binding)
        requireActionBar().title = "Sotuvchi"
        binding.apply {
            lottie.loadLottie(youAreSeller)
            addProduct.setOnClickListener {
                openFragment(AddBookFragment(),true)
            }
        }
    }
}