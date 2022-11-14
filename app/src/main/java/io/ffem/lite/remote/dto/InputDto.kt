package io.ffem.lite.remote.dto

import io.ffem.lite.model.Input

data class InputDto(
    val id: Int = 0,
    val name: String = ""
) {
    fun toInput(): Input {
        return Input(
            id = id,
            name = name
        )
    }
}