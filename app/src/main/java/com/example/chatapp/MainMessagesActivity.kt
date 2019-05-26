package com.example.chatapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.chatapp.chatRooms.ChatLogActivity
import com.example.chatapp.models.ChatMessage
import com.example.chatapp.models.DevChatRoom
import com.example.chatapp.models.FunChatRoom
import com.example.chatapp.models.ScienceChatRoom
import com.example.chatapp.registration.SignInActivity
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main_messages.*
import kotlinx.android.synthetic.main.latest_message_row.view.*
import android.support.annotation.NonNull
import android.util.Log
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks




class MainMessagesActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main_messages)
		setupUI()

		loadChatRooms()
		listenForLatestMessages()
		listenForDeepLinks()

		recyclerView_latest_messages.adapter = adapter


		val cls2 = SignInActivity()
		cls2.saveUserToFirebaseDatabase()
	}

	private fun listenForDeepLinks(){
		FirebaseDynamicLinks.getInstance()
			.getDynamicLink(intent)
			.addOnSuccessListener(this
				) { pendingDynamicLinkData ->
		// Get deep link from result (may be null if no link is found)
		var deepLink: Uri? = null
		if (pendingDynamicLinkData != null) {
			deepLink = pendingDynamicLinkData!!.link
		}


		Toast.makeText(this, "Deeplink: $deepLink", Toast.LENGTH_LONG).show()
	}
			.addOnFailureListener(this, object:OnFailureListener {
		override fun onFailure(e:Exception) {
		Log.w("Deep Link", "getDynamicLink:onFailure", e)
		}
		})
	}



	private fun listenForLatestMessages(){

		val toId = ChatLogActivity.toString()
		val ref = FirebaseDatabase.getInstance().getReference("/latest-messages")
		ref.addChildEventListener(object: ChildEventListener{
			override fun onChildAdded(p0: DataSnapshot, p1: String?) {
				//val chatMessage = p0.getValue(ChatMessage::class.java)
					//adapter.add(DevChatRoom(chatMessage))
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

	private val adapter = GroupAdapter<ViewHolder>()

	private fun loadChatRooms(){

		adapter.add(DevChatRoom())
		adapter.add(ScienceChatRoom())
		adapter.add(FunChatRoom())
	}

	private fun setupUI(){

		sortChats()

		recyclerView_latest_messages.setOnClickListener {
			intent = Intent(this, ChatLogActivity::class.java)
			startActivity(intent)
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
		}

		adapter.setOnItemClickListener{ item, view ->

			if(view.group_name.text == " Dev Group"){
				//Toast.makeText(this, "" + view.group_name.text.toString(), Toast.LENGTH_LONG).show()
				openRoom(view.group_name.text.toString(), view)
			}

			if(view.group_name.text == " Fun Group"){
				//Toast.makeText(this, "" + view.group_name.text.toString(), Toast.LENGTH_LONG).show()
				openRoom(view.group_name.text.toString(), view)
			}

			if(view.group_name.text == " Science Group"){
				//Toast.makeText(this, "" + view.group_name.text.toString(), Toast.LENGTH_LONG).show()
				openRoom(view.group_name.text.toString(), view)
			}
		}

	}

	private fun sortChats(){

		val chats: ArrayList<ChatMessage> = ArrayList()

		//chats.add()

	}

	private fun openRoom(roomKey: String, view: View){
		intent = Intent(this, ChatLogActivity::class.java)
		val roomKey = view.group_name.text
		intent.putExtra(room_key, roomKey)
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

	companion object {
		const val room_key = "ROOM_KEY"
		fun getLaunchIntent(from: Context) = Intent(from, MainMessagesActivity::class.java).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		}
	}
}

