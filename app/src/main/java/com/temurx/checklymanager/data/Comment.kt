package com.temurx.checklymanager.data

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val text: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Timestamp = Timestamp.now()
)
