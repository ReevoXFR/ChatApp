package com.example.chatapp.models

class ChatTextMessage(val type: String, val id: String, val text: String, val fromId: String, val toId: String, val time: Long, val photoUrl: String){
	constructor() : this ("","","","","", -1, "")
}