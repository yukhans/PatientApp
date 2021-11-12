package com.example.patientapp


import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.example.firebaseauth.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_patientdashboard.*
import kotlinx.android.synthetic.main.nav_header.*
import kotlinx.android.synthetic.main.nav_header.view.*
import java.util.*


class PatientDashboard : AppCompatActivity() {

    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth
    //google signin
    private lateinit var googleSignInClient: GoogleSignInClient
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Home"
        supportActionBar?.subtitle = "Patient Dashboard"

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)


        toggle = ActionBarDrawerToggle(this,drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //connecting to firebase
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()


        navigationView.setNavigationItemSelectedListener {
            when(it.itemId){
                //Profile
                R.id.item1 -> startActivity(Intent(this, PatientProfile::class.java))
                //History
                R.id.item2 -> Toast.makeText(applicationContext,"History is clicked", Toast.LENGTH_SHORT).show()
                //Logout
                R.id.item3 ->  {
                    val logoutDialogForm = androidx.appcompat.app.AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes") { _, _ ->
                            signOut()
                            checkUser()
                        }
                        .setNeutralButton("No") { _, _ ->

                        }
                    logoutDialogForm.show()

                }
                //Dashboard
                R.id.item4 -> startActivity(Intent(this, PatientDashboard::class.java))
            }
            true
        }


        //Book Appointment Button
        button1.setOnClickListener{
            startActivity(Intent(this, BookAppointment::class.java))
        }
        //Queue Status Button
        button2.setOnClickListener{
            Toast.makeText(applicationContext,"Queue Status is clicked", Toast.LENGTH_SHORT).show()
        }
        //QR Code Scanner Button
        button3.setOnClickListener{
            //QR CODE
            val scanner = IntentIntegrator(this)
            scanner.initiateScan()


        }
        //Checklist
        button4.setOnClickListener{
            Toast.makeText(applicationContext,"Activity Checklist is clicked", Toast.LENGTH_SHORT).show()
        }


        //for the purpose of google signin patients
        val googleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this,googleSignInOptions)
    }

    override fun onOptionsItemSelected(item: MenuItem):Boolean{
        val id = item.getItemId()
        if(toggle.onOptionsItemSelected(item)){
            return true
        }
        if(id== R.id.notif){
            Toast.makeText(this, "Notification Clicked", Toast.LENGTH_LONG).show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    private fun signOut()
    {
        //firebase signout
        firebaseAuth.signOut()
        //google signout
        googleSignInClient.signOut()
    }

    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if(firebaseUser==null){
            //user not logged in
            startActivity(Intent(this, PatientLogin::class.java))
            finish()
        }
        else
        {
            //user already logged in, get user info from database
            val uid = firebaseUser.uid
            if(uid.isNotEmpty()){
                readData(uid)
            }
        }
    }

    private fun readData(userId: String) {
        database = FirebaseDatabase.getInstance().getReference("Users") //get the "Users" Node
        database.child(userId).get().addOnSuccessListener {
            if (it.exists()) {
                val headerView: View = navigationView.getHeaderView(0)
                val navUsername: TextView = headerView.findViewById(R.id.nav_user_name)
                val navUserEmail: TextView = headerView.findViewById(R.id.nav_user_email)

                val uName = it.child("name").value
                val uAge = it.child("age").value
                val uEmail = it.child("email").value

                greetingname.text = "Hi ${uName.toString()},"
                navUsername.text = uName.toString()
                navUserEmail.text = uEmail.toString()

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        val result = IntentIntegrator.parseActivityResult(requestCode,resultCode,data)
        if(result!=null){
            if(result.contents==null){
                Toast.makeText(this,"Cancelled", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this,"Scanned: " + result.contents, Toast.LENGTH_SHORT).show()
            }
        }
        else{
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onBackPressed() {
        // Do Here what ever you want do on back press;
    }
}