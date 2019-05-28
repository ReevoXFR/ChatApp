package com.example.chatapp.models

import android.net.Uri

class ChatTextMessage(val id: String, val text: String, val fromId: String, val toId: String, val time: Long, val photoUrl: String){
	constructor() : this ("","","","", -1, "")
}