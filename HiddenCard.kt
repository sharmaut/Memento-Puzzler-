package com.example.finalproject.initials

data class HiddenCard (
    val identifier: Int,
    val url_image: String? = null,
    var Facing_up: Boolean = false,
    var Matched: Boolean = false
)