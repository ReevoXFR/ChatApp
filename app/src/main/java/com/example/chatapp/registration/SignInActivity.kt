package com.example.chatapp.registration

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.chatapp.activities.MainGroupsActivity
import com.example.chatapp.R
import com.example.chatapp.models.User
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_sign_in.*
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.FacebookCallback
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.database.FirebaseDatabase




class SignInActivity : AppCompatActivity() {

    val RC_SIGN_IN: Int = 1
    lateinit var callbackManager: CallbackManager
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var mGoogleSignInOptions: GoogleSignInOptions
    private lateinit var firebaseAuth: FirebaseAuth
    val TAG = "SignInActivity"
    var auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        showSplashScreen()

        Glide.with(applicationContext).load(R.drawable.loading).into(loader_image_view)
        Glide.with(applicationContext).load(R.drawable.live_bg).into(sing_in_activity_background)

        //Google
        firebaseAuth = FirebaseAuth.getInstance()
        configureGoogleSignIn()
        setupUI()

        //Facebook
        // Initialize Facebook Login button
        callbackManager = CallbackManager.Factory.create()

        facebook_button.setReadPermissions("email", "public_profile")
        facebook_button.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                Log.d(TAG, "facebook:onSuccess:$loginResult")
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Log.d(TAG, "facebook:onCancel")
	            buildDialog("Facebook")
            }

            override fun onError(error: FacebookException) {
                Log.d(TAG, "facebook:onError", error)
                // ...
            }
        })

    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    Toast.makeText(this, user?.uid, Toast.LENGTH_LONG)
                    val intent = Intent(this, MainGroupsActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }

                // ...
            }
    }

    private fun buildDialog(provider: String){

        val dialog = AlertDialog.Builder(this)
        dialog.setCancelable(false)
        dialog.setTitle("$provider login error")
        dialog.setMessage("Please try again!")
        // Display a neutral button on alert dialog
        dialog.setNeutralButton("Ok - I will retry"){_,_ ->
            Toast.makeText(this,"Ok.",Toast.LENGTH_SHORT).show()
        }

        dialog.create()
        dialog.show()

    }

    private fun showSplashScreen(){
        splash_screen.visibility = View.VISIBLE
        val isLoggedIn = verifyUserIsLoggedIn()
            Handler().postDelayed(
                {
                    if(isLoggedIn) {
                        val intent = Intent(this, MainGroupsActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    } else {
                        val aniFade = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_out)
                        splash_screen.startAnimation(aniFade)
                    }
                }, 4500 // value in milliseconds 
            )
     }

    private fun configureGoogleSignIn() {
        mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)
    }

    private fun setupUI() {
        google_button.setOnClickListener {
            signInGoogle()
        }

    }

    private fun signInGoogle() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //Google
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed:(", Toast.LENGTH_LONG).show()
            }
        }
        //Facebook
        callbackManager.onActivityResult(requestCode, resultCode, data)

    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                startActivity(MainGroupsActivity.getLaunchIntent(this))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            } else {
                Toast.makeText(this, "Google sign in failed:(", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        fun getLaunchIntent(from: Context) = Intent(from, SignInActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    }

    private fun verifyUserIsLoggedIn(): Boolean {

        val user = FirebaseAuth.getInstance().currentUser
        return user?.uid != null
    }

    fun saveUserToFirebaseDatabase() {
        val name = FirebaseAuth.getInstance().currentUser?.displayName.toString()
        val uid = FirebaseAuth.getInstance().uid.toString()
        val profile = FirebaseAuth.getInstance().currentUser?.photoUrl.toString()
        val provider = FirebaseAuth.getInstance().currentUser?.providerId.toString()
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        val user = User(uid, name, profile, provider)

        ref.setValue(user)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "Finally we saved the user to Firebase Database")
//                    val intent = Intent(this, MainGroupsActivity::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    startActivity(intent)
//                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                }
                .addOnFailureListener {
                    Log.d("RegisterActivity", "Failed to set value to database: ${it.message}")
                }
        }
}




















