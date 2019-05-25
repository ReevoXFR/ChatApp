package com.example.chatapp.models

import com.example.chatapp.R
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.latest_message_row.view.*

class DevChatRoom: Item<ViewHolder>(){

	override fun getId(): Long {
		return super.getId()
	}

	override fun bind(viewHolder: ViewHolder, position: Int) {
		viewHolder.itemView.group_name.text = " Dev Group"
		viewHolder.itemView.group_description.text = "by Developers, for Developers!"
	}
	override fun getLayout(): Int {
		return R.layout.latest_message_row
	}
}