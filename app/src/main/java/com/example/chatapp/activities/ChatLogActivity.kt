package com.example.chatapp.activities

import android.content.Context
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bumptech.glide.Glide
import com.example.chatapp.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_from_row.view.imageView_toRow
import kotlinx.android.synthetic.main.chat_to_row.view.*
import android.widget.Toast
import android.view.KeyEvent
import android.view.View
import com.example.chatapp.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class ChatLogActivity : AppCompatActivity() {

	companion object{
		val TAG = "ChatLogActivity"
	}

	var counter = 1

	val adapter = GroupAdapter<ViewHolder>()
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_chat_log)

		val extra = intent.getParcelableExtra<Room>(MainGroupsActivity.ROOM_KEY)

		//extra = intent.getStringExtra("ROOM_KEY")!!
		recyclerView_chatLog.adapter = adapter
		supportActionBar?.title = extra.title
		listenForMessages(counter)




		chatLog_sendText.setOnClickListener {
			Log.d(TAG, "Attempt to send message...")
			performSendMessage()
		}

		chatLog_writeText.setOnKeyListener(object : View.OnKeyListener {
			override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
				// If the event is a key-down event on the "enter" button
				if (event.getAction() === KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
					// Perform action on key press
					Toast.makeText(applicationContext, chatLog_writeText.text, Toast.LENGTH_SHORT).show()
					performSendMessage()
					return true
				}
				return false
			}
		})

		swipe_load_more_messages.setOnRefreshListener {

			counter += 1
			adapter.clear()
			listenForMessages(counter)



			swipe_load_more_messages.isRefreshing = false
		}

	}


	private fun listenForMessages(counter: Int){
		val extra = intent.getParcelableExtra<Room>(MainGroupsActivity.ROOM_KEY)
		val key = extra.uid
		val ref = FirebaseDatabase.getInstance().getReference("/messages/$key").limitToLast(counter * 50)

		ref.addChildEventListener(object: ChildEventListener{

			override fun onChildAdded(p0: DataSnapshot, p1: String?) {
				val chatMessage = p0.getValue(ChatTextMessage::class.java)
				val user = FirebaseAuth.getInstance().currentUser

				if(chatMessage != null){
					Log.d(TAG, chatMessage.text)

					if(chatMessage.fromId == FirebaseAuth.getInstance().uid){
						adapter.add(ChatToItem(chatMessage.text, user!!, chatMessage.time, applicationContext))
						recyclerView_chatLog.scrollToPosition(adapter.itemCount - 1)
					}   else {
						adapter.add(ChatFromItem(chatMessage.text,user?.displayName.toString(), chatMessage.fromId, chatMessage.photoUrl, chatMessage.time, applicationContext))
						recyclerView_chatLog.scrollToPosition(adapter.itemCount - 1)

						val chatPartnerId = chatMessage.fromId

						val ref = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")
						//Toast.makeText(baseContext, "FROM: ${chatMessage.photoUrl}", Toast.LENGTH_LONG).show()
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

		val extra = intent.getParcelableExtra<Room>(MainGroupsActivity.ROOM_KEY)
		val key = extra.uid
		val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl.toString()
		val text = chatLog_writeText.text.toString()
		val fromId = FirebaseAuth.getInstance().uid.toString()
		val toId = extra.title



		val reference = FirebaseDatabase.getInstance().getReference("/messages/$key").push()

		val chatMessage = ChatTextMessage(reference.key!!, text, fromId, toId, System.currentTimeMillis() / 1000, photoUrl)

		if(chatLog_writeText.text.isNotEmpty()) {
			reference.setValue(chatMessage)
				.addOnSuccessListener {
					Log.d(TAG, "Saved message: ${reference.key}")
					chatLog_writeText.text.clear()
					recyclerView_chatLog.scrollToPosition(adapter.itemCount - 1)
				}


		val latestMessagesRef = FirebaseDatabase.getInstance().getReference("/rooms/$key/timestamp/")

		latestMessagesRef.setValue(chatMessage.time)}
	}

}




class ChatFromItem(private val text: String, val username: String, val fromId: String, private val photoUrl: String, val time: Long , private val context: Context): Item<ViewHolder>() {
	override fun bind(viewHolder: ViewHolder, position: Int) {

		val day = TimeUnit.SECONDS.toDays(time)
        val hours = TimeUnit.SECONDS.toHours(time) - (day *24)
        val minute = TimeUnit.SECONDS.toMinutes(time) - (TimeUnit.SECONDS.toHours(time)* 60)
        val second = TimeUnit.SECONDS.toSeconds(time) - (TimeUnit.SECONDS.toMinutes(time) *60)

		viewHolder.itemView.textView_from_row.text = text
		viewHolder.itemView.chat_message_username.text = username
		viewHolder.itemView.chat_message_timestamp.text = "$hours:$minute"

		val uri = Uri.parse(photoUrl)
		val targetImageLocation = viewHolder.itemView.imageView_toRow


		Glide.with(context).load(uri).into(targetImageLocation)
	}

	override fun getLayout(): Int {
		return R.layout.chat_from_row
	}
}

class ChatToItem(private val text: String, val firebaseUser: FirebaseUser, val time: Long, private val context: Context): Item<ViewHolder>() {
	override fun bind(viewHolder: ViewHolder, position: Int) {

		val day = TimeUnit.SECONDS.toDays(time)
		val hours = TimeUnit.SECONDS.toHours(time) - (day *24)
		val minute = TimeUnit.SECONDS.toMinutes(time) - (TimeUnit.SECONDS.toHours(time)* 60)
		val second = TimeUnit.SECONDS.toSeconds(time) - (TimeUnit.SECONDS.toMinutes(time) *60)


		viewHolder.itemView.textView_to_row.text = text
		viewHolder.itemView.chat_message_username_toRow.text = firebaseUser.displayName
		viewHolder.itemView.chat_message_timestamp_toRow.text = "$hours:$minute"

		val uri = firebaseUser.photoUrl
		val targetImageLocation = viewHolder.itemView.imageView_toRow

		Glide.with(context).load(uri).into(targetImageLocation)

	}

	override fun getLayout(): Int {
		return R.layout.chat_to_row
	}
}
