@file:Suppress("DEPRECATION")

package com.example.chatapp.activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.ACTION_IMAGE_CAPTURE
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
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
import java.util.*
import java.util.concurrent.TimeUnit


@Suppress("DEPRECATED_IDENTITY_EQUALS", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
class ChatLogActivity : AppCompatActivity() {


	private lateinit var filePath: Uri
	private lateinit var pathToFile: String
	private lateinit var storage: FirebaseStorage
	private lateinit var storageReference: StorageReference
	private var PICK_IMAGE_REQUEST: Int = 71
	val adapter = GroupAdapter<ViewHolder>()
	var counter = 1


	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_chat_log)

		updateUI()
		listenForMessages()

	}


	companion object {
		const val TAG = "ChatLogActivity"
	}


	private fun updateUI(){
		val extra = intent.getParcelableExtra<Room>(MainGroupsActivity.ROOM_KEY)
		recyclerView_chatLog.adapter = adapter
		supportActionBar?.title = extra.title


		/*  Binding the editText(where user inserts text) with the onClickListener  */
		chatLog_sendText.setOnClickListener {
			Log.d(TAG, "Attempt to send message...")
			performSendMessage()
		}


		/*  Listener to send messages when ENTER it's pressed.  */
		chatLog_writeText.setOnKeyListener(object : View.OnKeyListener {
			override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
				if (event.action === KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
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


	/*  Opening the camera + taking picture works - sadly I didn't manage to implement the rest of it in time, to
	  * show it into chat */
	private fun openCamera() {
		val intent = Intent(ACTION_IMAGE_CAPTURE)
//		if(intent.resolveActivity(packageManager) != null){
//			var photoFile: File = createPhotoFile()
//			pathToFile = photoFile.path.toString()
//			val photoUri = FileProvider.getUriForFile(this, "com.example.chatapp.fileprovider", photoFile)
//			intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
			startActivityForResult(intent, 1)
		}



	/*  This is the class that helps with choosing the pics from the gallery  */
	private fun chooseImage() {
		val intent: Intent = Intent()
		intent.type = "image/*"
		intent.action = Intent.ACTION_GET_CONTENT

		startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
	}


	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)


		/*  If onResolt and requestCode + data & data.data are not null, we save data.data in a filePath and start uploading it to FireStore  */
		if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
			filePath = data.data
			uploadImage()
		}
	}


	/*  Uploading the image from the gallery to FireStore  */
	private fun uploadImage() {

		/* Showing a dialog */
		val progressDialog = ProgressDialog(this)
		progressDialog.setTitle("Uploading...")
		progressDialog.show()
		storage = FirebaseStorage.getInstance()
		storageReference = storage.reference

		/* Saving it into Storage  and tracking the uploading progress to take the download link after it's finished. */
		val ref: StorageReference = storageReference.child("images/" + UUID.randomUUID().toString())
		ref.putFile(filePath)
		var uploadTask = ref.putFile(filePath)
		var downloadUri = ""
		uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
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

		/* Saving the image uploaded to FireStore also as an item inside Firebase Realtime Database */
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


	/*  The chat listener works like this:
		1. Listens to child's added to the ref inside Database
		2. When child added - it checks if it's a string(message text) or if it's an image(image text)
		3. After deciding what type of message it is, it checks to see if it's a toMessage or a fromMessage (using fromID / toID)
		4. After deciding for who it's the message, using the TextToItem/TextFromItem or ImageToItem/ImageFromItem (all 4 are classes
		that implement custom layouts to look nice inside the chat.
		e.g if it's an ImageFromItem - it will be displayed on the right of chat using the layout made for images.
		5. Lastly, we have the SwipeToRefresh that helps with loading more messages when needed, swipe up - pull to refresh and 50 more messages (or x available
		messages) will appear. It uses the count variable that increments with +1 everytime we pullToRefresh and in the reference (ref = FirebaseDatabase...limitToLast(counter*50))
	 */
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


	/* For sending the message to the database we take the text from the editText, the items that we need to have a consistent message from the user
	like uid, photoUrl, toId, fromId, so on.
	Than we store it in Firebase Realtime Database under /messages/$key node.
	In the end of the function, we also save the date for the last message for each Room(used to sort the rooms on the Main Page)
	 */
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

/* Down bellow we have the 4 classes that the each message type implements together with the custom layout
*
*   Each of them have a lot of information about the user, time, room, etc.
*
*   Decided to use Glide for loading images and gifs as it's very easy and efficient.
* */

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

/* //TODO: WIP Converting the bitmap into Uri to use UploadImage() -> For saving the image captured with camera on Firestore
---

	@SuppressLint("SimpleDateFormat")
	private fun createPhotoFile(): File{
		val name: String = SimpleDateFormat("yyyyMMdd_hhmmss").format(Date())
		val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
		var image: File = File.createTempFile(name, ".jpg", storageDir)
			return image
	}


-OnActivityResult-
		if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
			//val externalFile = File(Environment.getExternalStorageDirectory(), pathToFile)
			val bitmap = BitmapFactory.decodeFile(pathToFile)
			image_view_camera.setImageBitmap(bitmap)
			val external = getImageUri(applicationContext, bitmap)
			filePath = external
			uploadImage() }
--
private fun  getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes: ByteArrayOutputStream = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
val path: String = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", "")
val values = ContentValues()
values.put(MediaStore.Images.Media.TITLE,"Title");
values.put(MediaStore.Images.Media.DESCRIPTION,"From Camera");
val path = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
return path
}
*/

