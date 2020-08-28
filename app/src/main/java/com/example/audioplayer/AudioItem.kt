package com.example.audioplayer

import android.graphics.Bitmap

data class AudioItem(
    val idP0: Long,
    val titleP1: String,
    val sizeP2: Int,
    val durationP3: Int,
    val albumP4: String,
    val artistP5: String,
    val albumArtP6: Bitmap?
)