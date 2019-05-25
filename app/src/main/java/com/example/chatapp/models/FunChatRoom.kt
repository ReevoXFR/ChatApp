package com.example.chatapp.models

import com.example.chatapp.R
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.latest_message_row.view.*

class FunChatRoom: Item<ViewHolder>(){

	val id = "fun_room"

	override fun bind(viewHolder: ViewHolder, position: Int) {
		viewHolder.itemView.group_name.text = " Fun Group"
		viewHolder.itemView.group_description.text = "Share your memes and tell funny stories!"
	}
	override fun getLayout(): Int {
		return R.layout.latest_message_row
	}
}