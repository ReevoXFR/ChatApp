package com.example.chatapp.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Room(val uid: String, val title: String, val description: String): Parcelable {
  constructor() : this("", "", "")
}
