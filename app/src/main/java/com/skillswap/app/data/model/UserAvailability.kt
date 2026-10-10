package com.skillswap.app.data.model

import com.google.firebase.firestore.DocumentId

data class UserAvailability(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dayOfWeek: String = "Monday",
    val startTime: String = "09:00",
    val endTime: String = "17:00"
)
