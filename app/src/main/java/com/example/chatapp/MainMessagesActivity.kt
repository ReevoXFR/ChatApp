package com.example.chatapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.chatapp.chatRooms.ChatLogActivity
import com.example.chatapp.models.*
import com.example.chatapp.registration.SignInActivity
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main_messages.*
import kotlinx.android.synthetic.main.latest_message_row.view.*
import com.google.firebase.database.FirebaseDatabase
import android.support.v4.widget.SwipeRefreshLayout







class MainMessagesActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main_messages)
		//addChatRoom("Dev Room", "by Developers, for Developers!")
		setupUI()

		loadChatRooms()
		//listenForLatestMessages()



		recyclerView_latest_messages.adapter = adapter

		val cls2 = SignInActivity()
		cls2.saveUserToFirebaseDatabase()

		swipe_refresh.setOnRefreshListener {
			loadChatRooms()
			swipe_refresh.isRefreshing = false
		}
	}



	companion object {
		val ROOM_KEY = "ROOM_KEY"
		fun getLaunchIntent(from: Context) = Intent(from, MainMessagesActivity::class.java).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		}}

//	private fun listenForLatestMessages(){
//
//		val toId = ChatLogActivity.toString()
//		val ref = FirebaseDatabase.getInstance().getReference("/latest-messages")
//		ref.addChildEventListener(object: ChildEventListener{
//			override fun onChildAdded(p0: DataSnapshot, p1: String?) {
//				//val chatMessage = p0.getValue(ChatMessage::class.java)
//					//adapter.add(DevChatRoom(chatMessage))
//			}
//
//			override fun onCancelled(p0: DatabaseError) {
//
//			}
//
//			override fun onChildChanged(p0: DataSnapshot, p1: String?) {
//
//			}
//
//			override fun onChildMoved(p0: DataSnapshot, p1: String?) {
//
//			}
//
//			override fun onChildRemoved(p0: DataSnapshot) {
//
//			}
//		})
//
//	}

	private val adapter = GroupAdapter<ViewHolder>()

	private fun addChatRoom(title: String, description: String){

		val reference = FirebaseDatabase.getInstance().getReference("/rooms").push()

		val roomDetails = Room(reference.key!!, title, description)
		reference.setValue(roomDetails)

		//val uid: String, val username: String, val profileImageUrl: String
	}

	private fun loadChatRooms(){

		val ref = FirebaseDatabase.getInstance().getReference("/rooms").orderByChild("lastmessage")
		ref.addListenerForSingleValueEvent(object: ValueEventListener {
			override fun onDataChange(p0: DataSnapshot) {
				val adapter = GroupAdapter<ViewHolder>()

				p0.children.reversed().forEach{
					val room = it.getValue(Room::class.java)
					if(room != null) {
						adapter.add(RoomItem(room))
					}
				}

				adapter.setOnItemClickListener{ item, view ->
					val roomItem = item as RoomItem
					val intent = Intent(applicationContext, ChatLogActivity::class.java)
					intent.putExtra(ROOM_KEY,roomItem.room)
					startActivity(intent)
				}

				recyclerView_latest_messages.adapter = adapter
			}

			override fun onCancelled(p0: DatabaseError) {}
		})
	}

	class RoomItem(val room: Room): Item<ViewHolder>(){
		override fun bind(viewHolder: ViewHolder, position: Int) {
			viewHolder.itemView.group_name.text = room.title
			viewHolder.itemView.group_description.text = room.description

			//Picasso.get().load(user.profileImageUrl).into(viewHolder.itemView.profile_pic)
		}

		override fun getLayout(): Int {
			return R.layout.latest_message_row
		}
	}

	private fun setupUI(){

		sortChats()

		recyclerView_latest_messages.setOnClickListener {
			intent = Intent(this, ChatLogActivity::class.java)
			startActivity(intent)
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
		}
	}

	private fun sortChats(){

		val rootRef = FirebaseDatabase.getInstance().reference
		val uid = FirebaseAuth.getInstance().currentUser!!.uid
		val query = rootRef.child("patient's_heart_rate").child(uid).orderByChild("heartRateTimeStamp")

	}

	private fun openRoom(roomKey: String, view: View){
		intent = Intent(this, ChatLogActivity::class.java)
		val roomKey = view.group_name.text
		intent.putExtra(ROOM_KEY, roomKey)
		startActivity(intent)
	}

	private fun signOut() {
		Toast.makeText(this, "Signing out...", Toast.LENGTH_LONG).show()
		LoginManager.getInstance().logOut()
		FirebaseAuth.getInstance().signOut()
		val intent = Intent(this, SignInActivity::class.java)
		startActivity(intent)
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		when(item?.itemId){
			R.id.menu_new_message -> {
				val intent = Intent(this, NewUsersActivity::class.java)
				startActivity(intent)
				overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
			}
			R.id.menu_sign_out -> {
				signOut()
			}
		}
		return super.onOptionsItemSelected(item)
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.nav_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

}

