package com.uz.kitoblar.ui

import android.content.Context
import com.airbnb.lottie.LottieCompositionFactory

var passwordPage = "https://assets8.lottiefiles.com/packages/lf20_jcpyfbsi.json"
var login = "https://assets8.lottiefiles.com/private_files/lf30_uvrwjrrs.json"
var userNotLogged = "https://assets7.lottiefiles.com/packages/lf20_gjmecwii.json"
var changePhoto = "https://assets4.lottiefiles.com/packages/lf20_pyoxvlmb.json"
var youAreSeller = "https://assets4.lottiefiles.com/private_files/lf30_t8f3t4.json"
var myBooksLottie = "https://assets3.lottiefiles.com/packages/lf20_szrbrL.json"
var playingLottie = "https://assets10.lottiefiles.com/packages/lf20_txqgnzpz.json"

fun preloadLottie(context: Context, url: String) {
    LottieCompositionFactory.fromUrl(context, url)
}

fun Context.preloadLot(url: String) {
    preloadLottie(this,url)
}