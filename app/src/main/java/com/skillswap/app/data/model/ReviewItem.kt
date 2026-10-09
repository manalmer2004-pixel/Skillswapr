package com.skillswap.app.data.model

data class ReviewItem(
    val id: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val reviewerAvatar: String = "",
    val rating: Double = 5.0,
    val comment: String = "",
    val skillName: String = "",
    val date: String = ""
)
