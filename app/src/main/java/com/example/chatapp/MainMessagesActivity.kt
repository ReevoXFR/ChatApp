package com.example.chatapp

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.example.chatapp.chatRooms.ChatLogActivity
import com.example.chatapp.registration.SignInActivity
import com.facebook.login.LoginManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main_messages.*


class MainMessagesActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main_messages)
		setupUI()

		val cls2 = SignInActivity()
		cls2.saveUserToFirebaseDatabase()
	}

	private fun setupUI(){

		chat_room_developers.setOnClickListener {
			intent = Intent(this, ChatLogActivity::class.java)
			startActivity(intent)
			overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
		}

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
		fun getLaunchIntent(from: Context) = Intent(from, MainMessagesActivity::class.java).apply {
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		}
	}
}

