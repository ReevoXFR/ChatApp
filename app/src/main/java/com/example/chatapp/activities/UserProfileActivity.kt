package com.example.chatapp.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.chatapp.R
import com.example.chatapp.models.User

class UserProfileActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_user_profile)

		val user = intent.getParcelableExtra<User>(NewUsersActivity.USER_KEY)


		supportActionBar?.title = user.username

	}
}
