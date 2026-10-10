package com.skillswap.app.data.model

import com.google.firebase.firestore.DocumentId

data class UserFavorite(
    @DocumentId val id: String = "",
    val userId: String = "",
    val skillId: String = ""
)
