package com.example.fotoconnect

data class Post(
    var description: String = "",
    var image_url1: String = "",
    var creation_time_miliseconds: Long = 0,
    var user: com.example.fotoconnect.User? =null,
    var image_url2: String = "",
)
