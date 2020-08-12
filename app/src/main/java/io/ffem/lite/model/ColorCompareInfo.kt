package io.ffem.lite.model

class ColorCompareInfo(
    val result: Double,
    @Suppress("unused")
    val resultColor: Int,
    val matchedColor: Int,
    val distance: Double,
    val matchedIndex: Int
)