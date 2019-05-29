package com.example.chatapp.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.models.User
import kotlinx.android.synthetic.main.activity_user_profile.*

class UserProfileActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_user_profile)

		val user = intent.getParcelableExtra<User>(NewUsersActivity.USER_KEY)
		Glide.with(applicationContext).load(R.drawable.coming_soon).into(user_profile_background)

		supportActionBar?.title = user.username

	}
}
