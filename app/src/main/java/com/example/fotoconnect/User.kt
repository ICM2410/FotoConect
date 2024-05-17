package com.example.fotoconnect

import android.provider.ContactsContract.CommonDataKinds.Email

class User {

    var name: String? = null
    var email: String? = null
    var uid: String? = null
    var latitude: Double? = null
    var longitude: Double? = null
    var contactImage: String? = null
    constructor(){}
    constructor(name: String?, email: String?, uid: String?,latitude: Double?, longitude: Double?, contactImage: String?){
        this.name = name
        this.email = email
        this.uid = uid
        this.latitude = latitude
        this.longitude = longitude
        this.contactImage = contactImage
    }
}