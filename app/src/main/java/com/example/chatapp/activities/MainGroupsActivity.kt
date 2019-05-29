package com.example.chatapp.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.chatapp.R
import com.example.chatapp.models.Room
import com.example.chatapp.registration.SignInActivity
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_main_groups.*
import kotlinx.android.synthetic.main.group_row.view.*


class MainGroupsActivity : AppCompatActivity() {


	private val adapter = GroupAdapter<ViewHolder>()


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main_groups)
		setupUI()
		loadChatRooms()
	}


	companion object {
		const val ROOM_KEY: String = "ROOM_KEY"
		fun getLaunchIntent(from: Context) = Intent(from, MainGroupsActivity::class.java).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		}}


	/*  This method gets called only when new ChatRooms should be added to the Database.
	e.g addChatRoom("Dev Room", "by Developers, for Developers!")   */
	private fun addChatRoom(title: String, description: String){
		val reference = FirebaseDatabase.getInstance().getReference("/rooms").push()
		val roomDetails = Room(reference.key!!, title, description)
		reference.setValue(roomDetails)
	}


	private fun loadChatRooms(){
		/*  Retrieving the ChatRooms from firebaseDatabase, sorted by last message using the timestamp. */
		val ref = FirebaseDatabase.getInstance().getReference("/rooms").orderByChild("timestamp")
		ref.addListenerForSingleValueEvent(object: ValueEventListener {
			override fun onDataChange(p0: DataSnapshot) {
				val adapter = GroupAdapter<ViewHolder>()
				p0.children.reversed().forEach{
					val room = it.getValue(Room::class.java)
					if(room != null) {
						adapter.add(RoomItem(room))
					}
				}

				/*  Setting onClick on each item + putting extra info into Intent to be able to configure the
				 * newly opened Activity. */
				adapter.setOnItemClickListener{ item, _ ->
					val roomItem = item as RoomItem
					val intent = Intent(applicationContext, ChatLogActivity::class.java)
					intent.putExtra(ROOM_KEY,roomItem.room)
					startActivity(intent)
				}

				recyclerView_group_rooms.adapter = adapter
			}

			override fun onCancelled(p0: DatabaseError) {
				Toast.makeText(applicationContext, getString(R.string.loading_chat_rooms_canceled), Toast.LENGTH_LONG).show()
			}
		})
	}


	private fun setupUI(){
		/*  Setting the logo + title to the Action-Bar */
		supportActionBar?.setDisplayShowHomeEnabled(true)
		supportActionBar?.setIcon(R.drawable.logop)

		/*  Setting the adapter for the recycler-view that keeps the chat-rooms. */
		recyclerView_group_rooms.adapter = adapter

		/*  We need this HERE to save the user to database, after he signs in.
		(If it would have been called on SignInActivity it was returning Null
		 because the database connection were slower compared with the speed of execution
		 of .saveUserToFirebaseDatabase() */
		val cls2 = SignInActivity()
		cls2.saveUserToFirebaseDatabase()


		swipe_refresh_sort_groups.setOnRefreshListener {
			loadChatRooms()
			swipe_refresh_sort_groups.isRefreshing = false
		}

		recyclerView_group_rooms.setOnClickListener {
			intent = Intent(this, ChatLogActivity::class.java)
			startActivity(intent)
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
		}
	}

	private fun signOut() {
		/* Facebook Sign-Out */
		LoginManager.getInstance().logOut()
		FirebaseAuth.getInstance().signOut()

		/* Google Sign-Out */
		val mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
			.requestIdToken(getString(R.string.default_web_client_id))
			.requestEmail()
			.build()
		val mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)
		mGoogleSignInClient.signOut().addOnCompleteListener(this) { }
		Toast.makeText(this, getString(R.string.sign_out), Toast.LENGTH_LONG).show()
		val intent = Intent(this, SignInActivity::class.java)
		startActivity(intent)
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
	}


	override fun onOptionsItemSelected(item: MenuItem?): Boolean {
		when(item?.itemId){
			R.id.menu_new_message -> {
				val intent = Intent(this, NewUsersActivity::class.java)
				startActivity(intent)
			}
			R.id.menu_sign_out -> {
				signOut()
			}
		}
		return super.onOptionsItemSelected(item)
	}

	/* Implementation for the menu options from main activity ( Chat Rooms ) */
	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.nav_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}


	class RoomItem(val room: Room): Item<ViewHolder>(){
		override fun bind(viewHolder: ViewHolder, position: Int) {
			viewHolder.itemView.group_name.text = room.title
			viewHolder.itemView.group_description.text = room.description
		}

		override fun getLayout(): Int {
			return R.layout.group_row
		}
	}

}

