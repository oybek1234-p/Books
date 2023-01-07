package com.uz.kitoblar.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout

const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
const val MATCH_PARENT = ViewGroup.LayoutParams.MATCH_PARENT

fun FrameLayout.layParams(width: Int,height: Int) = FrameLayout.LayoutParams(width,height)

fun LinearLayout.layParams(width: Int,height: Int) = LinearLayout.LayoutParams(width,height)
