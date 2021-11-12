package com.example.patientapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.firebaseauth.R
import com.example.firebaseauth.databinding.ActivityPatientLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_patient_login.*
import kotlinx.android.synthetic.main.activity_patientdashboard.*
import kotlinx.android.synthetic.main.activity_register.*



class PatientLogin : AppCompatActivity() {
    //view binding
    private lateinit var binding: ActivityPatientLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private companion object{
        private const val RC_SIGN_IN = 100
        private const val TAG = "GOOGLE_SIGN_IN_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //configure Google SignIn
        val googleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this,googleSignInOptions)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //GoogleSignIn Button, Click to begin Sign In
        binding.googleSignInButton.setOnClickListener{
            Log.d(TAG,"onCreate: begin Google SignIn")
            val intent = googleSignInClient.signInIntent
            startActivityForResult(intent, RC_SIGN_IN)


        }

        //if user clicked register button
        registerPatient.setOnClickListener{
            //init register intent
            val register_patient_intent = Intent(this, PatientRegistration::class.java)
            startActivity(register_patient_intent)
        }

        //Normal signin button, click to begin
        binding.loginBtn.setOnClickListener{
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken,0)

            if(emailtext.text.toString().isNullOrEmpty() || passtext.text.toString().isNullOrEmpty())
            {
                Toast.makeText(this,"Email Address or Password not provided", Toast.LENGTH_SHORT).show()
            }
            else
            {
                firebaseAuth.signInWithEmailAndPassword(emailtext.text.toString(), passtext.text.toString())
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Sign In Successful", Toast.LENGTH_SHORT).show()
                            val user = firebaseAuth.currentUser
                            updateUI(user)
                        } else {
                            Toast.makeText(this, "Invalid Email or Password", Toast.LENGTH_SHORT).show()
                        }
                    }

            }
        }
    }
    private fun updateUI(currentUser: FirebaseUser?) {
        if(currentUser!=null) {
            if(currentUser.isEmailVerified){
                //init profile intent
                val patientdashboard_intent = Intent(this, PatientDashboard::class.java)
                startActivity(patientdashboard_intent)
                finish()
            } else{
                Toast.makeText(this, "Email Address not verified", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == RC_SIGN_IN) {
            Log.d(TAG, "onActivityResult: Google SignIn Intent Result")
            val accountTask = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                //Google SignIn success, with auth firebase
                val account = accountTask.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogleAccount(account)
            }
            catch (e: Exception){
                //failed google sign in
                Log.w(TAG,"onActivityResult: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogleAccount(account: GoogleSignInAccount?) {
        Log.d(TAG,"firebaseAuthWithGoogleAccount: begin firebase auth with google account")

        val credential = GoogleAuthProvider.getCredential(account!!.idToken, null)
        firebaseAuth.signInWithCredential(credential)

            .addOnSuccessListener { authResult ->
                //login success
                Log.d(TAG,"firebaseAuthWithGoogleAccount: LoggedIn")

                //get loggedIn User
                val firebaseUser = firebaseAuth.currentUser
                //get user info
                val uid = firebaseAuth.uid
                val email = firebaseUser?.email


                Log.d(TAG,"firebaseAuthWithGoogleAccount: Uid: $uid")
                Log.d(TAG,"firebaseAuthWithGoogleAccount: Email: $email")

                //check if user is new or existing
                if(authResult.additionalUserInfo!!.isNewUser){
                    //user is new - account created
                    Log.d(TAG,"firebaseAuthWithGoogleAccount: Account created... \n$email")
                    Toast.makeText(this,"Account created ... \n$email", Toast.LENGTH_SHORT).show()
                }
                else{
                    //existing user = loggedIn
                    Log.d(TAG,"firebaseAuthWithGoogleAccount: Existing User... \n$email")
                    Toast.makeText(this,"LoggedIn ... \n$email", Toast.LENGTH_SHORT).show()
                }

                register()
            }
            .addOnFailureListener{ e ->
                //login failed
                Log.d(TAG,"firebaseAuthWithGoogleAccount: Loggin Failed due to ${e.message}")
                Toast.makeText(this,"Loggin Failed due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun register() {
        database = FirebaseDatabase.getInstance().getReference("Users")
        val firebaseUser = firebaseAuth.currentUser
        val User = UsersData(firebaseUser?.displayName, "", firebaseUser?.email, "")
        database.child(firebaseAuth.currentUser!!.uid)
            .setValue(User)
            .addOnSuccessListener {
                //start profile activity
                startActivity(Intent(this@PatientLogin, PatientDashboard::class.java))
                finish()
            }
    }

    override fun onBackPressed() {

    }

}
















