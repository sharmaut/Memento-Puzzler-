package com.example.finalproject.initials

import com.google.firebase.firestore.PropertyName

data class userimagelist(
    @PropertyName("images") val images: List<String>? = null
)
