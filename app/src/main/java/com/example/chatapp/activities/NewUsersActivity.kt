package com.example.chatapp.activities

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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

class NewUsersActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_new_users)

		supportActionBar?.title = "Select User"
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


				p0.children.forEach{
					Log.d("NewMessage", it.toString())
					val user = it.getValue(User::class.java)
					if(user != null) {
						adapter.add(UserItem(user, applicationContext))
					}
				}

				adapter.setOnItemClickListener{ item, view ->

					val userItem = item as UserItem

					val intent = Intent(view.context, UserProfileActivity::class.java)
					intent.putExtra(USER_KEY,userItem.user)
					startActivity(intent)
				}


				recyclerview_newmessage.adapter = adapter
			}

			override fun onCancelled(p0: DatabaseError) {

			}


		})
	}

	class UserItem(val user: User, val context: Context): Item<ViewHolder>(){
		override fun bind(viewHolder: ViewHolder, position: Int) {
			viewHolder.itemView.username.text = user.username
			Glide.with(context).load(user.profileImageUrl).into(viewHolder.itemView.profile_pic)
		}

		override fun getLayout(): Int {
			return R.layout.user_row_new_message
		}
	}
}
