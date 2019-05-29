package com.example.chatapp.models

import com.example.chatapp.R
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.latest_message_row.view.*

class ScienceChatRoom: Item<ViewHolder>(){

	val id = "science_room"

	override fun bind(viewHolder: ViewHolder, position: Int) {
		viewHolder.itemView.group_name.text = " Science Group"
		viewHolder.itemView.group_description.text = "Any ideas how to conquer Mars? Tell us!"
	}
	override fun getLayout(): Int {
		return R.layout.latest_message_row
	}
}