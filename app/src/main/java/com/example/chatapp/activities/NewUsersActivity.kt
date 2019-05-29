package com.example.chatapp.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.models.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_new_users.*
import kotlinx.android.synthetic.main.user_row_new_message.view.*

/*  This class it's here as a bonus as I've first worked on this one before retrieving messages
from Firebase (it was easier) so I decided to leave it here. It's meant to show you the other group-rooms members.
 */

class NewUsersActivity : AppCompatActivity() {


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_new_users)
		supportActionBar?.title = getString(R.string.select_user)
		fetchUsers()
	}


	companion object{
		val USER_KEY = "USER_KEY"
	}


	private fun fetchUsers(){
		val ref = FirebaseDatabase.getInstance().getReference("/users")
		ref.addListenerForSingleValueEvent(object: ValueEventListener {
			override fun onDataChange(p0: DataSnapshot) {
				val adapter = GroupAdapter<ViewHolder>()

				/* We pass through all users and adding them to the adapter. */
				p0.children.forEach{
					Log.d("New user fetched: ", it.toString())
					val user = it.getValue(User::class.java)
					if(user != null) {
						adapter.add(UserItem(user, applicationContext))
					}
				}

				/* For each user fetched and added to the adapter, we set onClick to open new Activity with the intent's extras. */
				adapter.setOnItemClickListener{ item, view ->

					val userItem = item as UserItem

					val intent = Intent(view.context, UserProfileActivity::class.java)
					intent.putExtra(USER_KEY,userItem.user)
					startActivity(intent)
				}
				recyclerview_users.adapter = adapter
			}
			override fun onCancelled(p0: DatabaseError) {}
		})
	}

	/* This is the userItem that we're using to bind the data from dataBase with the User Design Layout. */
	class UserItem(val user: User, private val context: Context): Item<ViewHolder>(){
		override fun bind(viewHolder: ViewHolder, position: Int) {
			viewHolder.itemView.username.text = user.username
			Glide.with(context).load(user.profileImageUrl).into(viewHolder.itemView.profile_pic)
		}

		override fun getLayout(): Int {
			return R.layout.user_row_new_message
		}
	}
}

