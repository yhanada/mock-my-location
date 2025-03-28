package com.github.yhanada.mockmylocation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MyLocation(
    val name: String,
    val lat: Double,
    val lng: Double,
) : Parcelable
