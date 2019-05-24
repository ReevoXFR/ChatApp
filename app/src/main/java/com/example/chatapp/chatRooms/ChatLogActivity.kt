package com.example.chatapp.chatRooms

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.chatapp.R
import com.example.chatapp.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_from_row.view.imageView_fromRow
import kotlinx.android.synthetic.main.chat_to_row.view.*

class ChatLogActivity : AppCompatActivity() {

	companion object{
		val TAG = "ChatLogActivity"
	}

	val adapter = GroupAdapter<ViewHolder>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_chat_log)

		recyclerView_chatLog.adapter = adapter

		supportActionBar?.title = "Chat Logs v2"
		//setupFakeChat()

		listenForMessages()


		chatLog_sendText.setOnClickListener {
			Log.d(TAG, "Attempt to send message...")
			performSendMessage()
		}


	}

	private fun listenForMessages(){
		val ref = FirebaseDatabase.getInstance().getReference("/messages")



		ref.addChildEventListener(object: ChildEventListener{

			override fun onChildAdded(p0: DataSnapshot, p1: String?) {
				val chatMessage = p0.getValue(ChatMessage::class.java)
				val user = FirebaseAuth.getInstance().currentUser

				if(chatMessage != null){
					Log.d(TAG, chatMessage.text)

					if(chatMessage.fromId == FirebaseAuth.getInstance().uid){
						adapter.add(ChatToItem(chatMessage.text, user!!))
					}   else {

						adapter.add(ChatFromItem(chatMessage.text, user!!))
					}


				}

			}

			override fun onCancelled(p0: DatabaseError) {

			}

			override fun onChildChanged(p0: DataSnapshot, p1: String?) {

			}

			override fun onChildMoved(p0: DataSnapshot, p1: String?) {

			}

			override fun onChildRemoved(p0: DataSnapshot) {

			}

		})
	}

	private fun performSendMessage(){

		//val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl
		val text = chatLog_writeText.text.toString()
		val fromId = FirebaseAuth.getInstance().uid
		val toId = ChatLogActivity.toString()



		val reference = FirebaseDatabase.getInstance().getReference("/messages").push()

		if(fromId == null) return
		val chatMessage = ChatMessage(reference.key!!, text, fromId, toId, System.currentTimeMillis() / 1000)

		reference.setValue(chatMessage)
			.addOnSuccessListener {
				Log.d(TAG, "Saved message: ${reference.key }")
			}
	}

}




class ChatFromItem(private val text: String, val firebaseUser: FirebaseUser): Item<ViewHolder>() {
	override fun bind(viewHolder: ViewHolder, position: Int) {
		viewHolder.itemView.textView_from_row.text = text

		val uri = firebaseUser
		val targetImageLocation = viewHolder.itemView.imageView_fromRow

		//Picasso.get().load(photoUrl).into(targetImageLocation)
	}

	override fun getLayout(): Int {
		return R.layout.chat_from_row
	}
}

class ChatToItem(private val text: String, val firebaseUser: FirebaseUser): Item<ViewHolder>() {
	override fun bind(viewHolder: ViewHolder, position: Int) {
		viewHolder.itemView.textView_to_row.text = text

		val uri = firebaseUser.photoUrl
		val targetImageLocation = viewHolder.itemView.imageView_fromRow

		Picasso.get().load(uri).into(targetImageLocation)

	}

	override fun getLayout(): Int {
		return R.layout.chat_to_row
	}
}
