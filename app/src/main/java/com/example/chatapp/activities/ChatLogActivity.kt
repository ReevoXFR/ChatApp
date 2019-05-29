@file:Suppress("DEPRECATION")

package com.example.chatapp.activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.chatapp.R
import com.example.chatapp.models.ChatMessage
import com.example.chatapp.models.Room
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_from_row.view.chat_message_timestamp
import kotlinx.android.synthetic.main.chat_from_row.view.chat_message_username
import kotlinx.android.synthetic.main.chat_from_row.view.imageView_toRow
import kotlinx.android.synthetic.main.chat_to_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.chat_message_timestamp_toRow
import kotlinx.android.synthetic.main.chat_to_row.view.chat_message_username_toRow
import kotlinx.android.synthetic.main.image_from_row.view.*
import kotlinx.android.synthetic.main.image_to_row.view.*
import org.jetbrains.anko.design.snackbar
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


@Suppress("DEPRECATED_IDENTITY_EQUALS", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
class ChatLogActivity : AppCompatActivity() {


	private lateinit var filePath: Uri
	private lateinit var imageUri: Uri
	private lateinit var storage: FirebaseStorage
	private lateinit var storageReference: StorageReference
	private var PICK_IMAGE_REQUEST: Int = 71
	private val TAKE_PICTURE = 1
	val adapter = GroupAdapter<ViewHolder>()
	var counter = 1


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_chat_log)

		val extra = intent.getParcelableExtra<Room>(MainGroupsActivity.ROOM_KEY)

		recyclerView_chatLog.adapter = adapter
		supportActionBar?.title = extra.title
		listenForMessages()

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

		gallery.setOnClickListener {
			chooseImage()
		}

		camera.setOnClickListener {
			openCamera()
		}

	}


	companion object {
		const val TAG = "ChatLogActivity"
	}


	private fun openCamera() {
		val builder: StrictMode.VmPolicy.Builder = StrictMode.VmPolicy.Builder()
		StrictMode.setVmPolicy(builder.build())
		val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
		val photo = File(Environment.getExternalStorageDirectory(), "Pic.jpg")
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo))
		imageUri = Uri.fromFile(photo)
		startActivityForResult(intent, TAKE_PICTURE)
	}


	private fun chooseImage() {
		val intent: Intent = Intent()
		intent.type = "image/*"
		intent.action = Intent.ACTION_GET_CONTENT

		startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
	}


	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
			filePath = data.data
			uploadImage()
		}

		// Trying to send camera pictures will result in a crash -
		//Todo: Research what's the problem - after taking picture with camera, the intent.extra it's null.
		if (requestCode == TAKE_PICTURE) {
			filePath = data?.extras?.get("data") as Uri
			uploadImage()
		}
	}

	private fun uploadImage() {

		val progressDialog = ProgressDialog(this)
		progressDialog.setTitle("Uploading...")
		progressDialog.show()
		storage = FirebaseStorage.getInstance()
		storageReference = storage.reference

		val ref: StorageReference = storageReference.child("images/" + UUID.randomUUID().toString())
		ref.putFile(filePath)



		var uploadTask = ref.putFile(filePath)
		var downloadUri = ""
		val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
			if (!task.isSuccessful) {
				task.exception?.let {
					throw it
				}
			}
			return@Continuation ref.downloadUrl
		}).addOnCompleteListener { task ->
			if (task.isSuccessful) {
				downloadUri = task.result.toString()
			}

			progressDialog.dismiss()

			val extra = intent.getParcelableExtra<Room>(MainGroupsActivity.ROOM_KEY)
			val key = extra.uid
			val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl.toString()
			val fromId = FirebaseAuth.getInstance().uid.toString()
			val toId = extra.title

			val reference = FirebaseDatabase.getInstance().getReference("/messages/$key").push()
			val chatMessage = ChatMessage("Image", reference.key!!, downloadUri, fromId, toId, System.currentTimeMillis() / 1000, photoUrl)

			reference.setValue(chatMessage)
				.addOnSuccessListener {
					Log.d(TAG, "Saved message: ${reference.key}")
					recyclerView_chatLog.scrollToPosition(adapter.itemCount - 1)

					val latestMessagesRef = FirebaseDatabase.getInstance().getReference("/rooms/$key/timestamp/")

					latestMessagesRef.setValue(chatMessage.time)
				}
		}


	}


	private fun listenForMessages() {

		val extra = intent.getParcelableExtra<Room>(MainGroupsActivity.ROOM_KEY)
		val key = extra.uid
		val ref = FirebaseDatabase.getInstance().getReference("/messages/$key").limitToLast(counter * 50)
		val childListener = ref.addChildEventListener(object : ChildEventListener {
			override fun onChildAdded(p0: DataSnapshot, p1: String?) {
				val chatMessage = p0.getValue(ChatMessage::class.java)
				val user = FirebaseAuth.getInstance().currentUser

				if (chatMessage != null && chatMessage.type == "String") {
					Log.d(TAG, chatMessage.type)

					if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
						adapter.add(TextToItem(chatMessage.text, user!!, chatMessage.time, applicationContext))
						recyclerView_chatLog.scrollToPosition(adapter.itemCount - 1)
					} else {
						adapter.add(TextFromItem(chatMessage.text, user?.displayName.toString(), chatMessage.fromId, chatMessage.photoUrl, chatMessage.time, applicationContext
						)
						)
						recyclerView_chatLog.scrollToPosition(adapter.itemCount - 1)
					}
				} else if (chatMessage != null && chatMessage.type == "Image") {
					Log.d(TAG, chatMessage.text)

					if (chatMessage.fromId == FirebaseAuth.getInstance().uid) {
						adapter.add(ImageToItem(chatMessage.text, user!!, chatMessage.time, applicationContext))
						recyclerView_chatLog.scrollToPosition(adapter.itemCount - 1)
					} else {
						adapter.add(ImageFromItem(chatMessage.text, user?.displayName.toString(), chatMessage.fromId, chatMessage.photoUrl, chatMessage.time, applicationContext))
						recyclerView_chatLog.scrollToPosition(adapter.itemCount - 1)
					}
				}
			}

			override fun onCancelled(p0: DatabaseError) {}
			override fun onChildChanged(p0: DataSnapshot, p1: String?) {}
			override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
			override fun onChildRemoved(p0: DataSnapshot) {}
		})

		swipe_load_more_messages.setOnRefreshListener {
			counter += 1
			ref.removeEventListener(childListener)
			adapter.clear()
			listenForMessages()
			recyclerView_chatLog.scrollToPosition(recyclerView_chatLog.childCount)
			swipe_load_more_messages.isRefreshing = false
		}

	}


	private fun performSendMessage() {

		val extra = intent.getParcelableExtra<Room>(MainGroupsActivity.ROOM_KEY)
		val key = extra.uid
		val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl.toString()
		val text = chatLog_writeText.text.toString()
		val fromId = FirebaseAuth.getInstance().uid.toString()
		val toId = extra.title
		val reference = FirebaseDatabase.getInstance().getReference("/messages/$key").push()

		if (chatLog_writeText.text.isNotEmpty()) {
			val chatMessage = ChatMessage("String", reference.key!!, text, fromId, toId, System.currentTimeMillis() / 1000, photoUrl)

			reference.setValue(chatMessage)
				.addOnSuccessListener {
					Log.d(TAG, "Saved message: ${reference.key}")
					chatLog_writeText.text.clear()
					recyclerView_chatLog.scrollToPosition(adapter.itemCount - 1)
				}

			val latestMessagesRef = FirebaseDatabase.getInstance().getReference("/rooms/$key/timestamp/")
			latestMessagesRef.setValue(chatMessage.time)
		}
	}
}


class TextFromItem(
	private val text: String,
	val username: String,
	val fromId: String,
	private val photoUrl: String,
	val time: Long,
	private val context: Context
) : Item<ViewHolder>() {
	override fun bind(viewHolder: ViewHolder, position: Int) {

		val day = TimeUnit.SECONDS.toDays(time)
		val hours = TimeUnit.SECONDS.toHours(time) - (day * 24)
		val minute = TimeUnit.SECONDS.toMinutes(time) - (TimeUnit.SECONDS.toHours(time) * 60)

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


class TextToItem(
	private val text: String,
	val firebaseUser: FirebaseUser,
	val time: Long,
	private val context: Context
) : Item<ViewHolder>() {
	override fun bind(viewHolder: ViewHolder, position: Int) {

		val day = TimeUnit.SECONDS.toDays(time)
		val hours = TimeUnit.SECONDS.toHours(time) - (day * 24)
		val minute = TimeUnit.SECONDS.toMinutes(time) - (TimeUnit.SECONDS.toHours(time) * 60)

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


class ImageFromItem(
	private val text: String,
	val username: String,
	val fromId: String,
	private val photoUrl: String,
	val time: Long,
	private val context: Context
) : Item<ViewHolder>() {
	override fun bind(viewHolder: ViewHolder, position: Int) {

		val day = TimeUnit.SECONDS.toDays(time)
		val hours = TimeUnit.SECONDS.toHours(time) - (day * 24)
		val minute = TimeUnit.SECONDS.toMinutes(time) - (TimeUnit.SECONDS.toHours(time) * 60)

		Glide.with(context).load(text).into(viewHolder.itemView.imageView_from_row)
		viewHolder.itemView.chat_message_username.text = username
		viewHolder.itemView.chat_message_timestamp.text = "$hours:$minute"

		val uri = Uri.parse(photoUrl)
		val targetImageLocation = viewHolder.itemView.imageView_toRow

		Glide.with(context).load(uri).into(targetImageLocation)
	}

	override fun getLayout(): Int {
		return R.layout.image_from_row
	}
}


class ImageToItem(private val text: String, val firebaseUser: FirebaseUser, val time: Long, private val context: Context): Item<ViewHolder>() {
	override fun bind(viewHolder: ViewHolder, position: Int) {

		val day = TimeUnit.SECONDS.toDays(time)
		val hours = TimeUnit.SECONDS.toHours(time) - (day * 24)
		val minute = TimeUnit.SECONDS.toMinutes(time) - (TimeUnit.SECONDS.toHours(time) * 60)

		Glide.with(context).load(text).into(viewHolder.itemView.imageView_to_row)
		viewHolder.itemView.chat_message_username_toRow.text = firebaseUser.displayName
		viewHolder.itemView.chat_message_timestamp_toRow.text = "$hours:$minute"

		val uri = firebaseUser.photoUrl
		val targetImageLocation = viewHolder.itemView.imageView_toRow

		Glide.with(context).load(uri).into(targetImageLocation)
	}


	override fun getLayout(): Int {
		return R.layout.image_to_row
	}
}
