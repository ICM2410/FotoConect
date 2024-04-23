package com.example.fotoconnect.model

import com.google.firebase.firestore.PropertyName

data class Post (
    var description: String = "",
    var image_url: String = "",
    var creation_time_miliseconds: Long = 0,
    var user: User? =null)
