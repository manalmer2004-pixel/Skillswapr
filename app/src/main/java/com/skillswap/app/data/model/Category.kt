package com.skillswap.app.data.model

import com.google.firebase.firestore.DocumentId

data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val icon: String = "",
    val order: Int = 0
)
