package com.example.chatapp.models

class User(val uid: String, val username: String, val profileImageUrl: String, val provider: String) {
  constructor() : this("", "", "", "")
}