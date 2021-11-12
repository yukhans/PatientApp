package com.example.patientapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.example.firebaseauth.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_patient_profile.*
import kotlinx.android.synthetic.main.activity_patientdashboard.*
import java.util.*


class PatientProfile : AppCompatActivity() {
    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth

    //google signin
    lateinit var googleSignInClient: GoogleSignInClient
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patient_profile)

        supportActionBar!!.title = "Home"
        supportActionBar!!.subtitle = "Patient Profile"

        drawerLayout = findViewById(R.id.drawer_layout1)
        navigationView = findViewById(R.id.nav_view1)


        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //connecting to firebase
        firebaseAuth = FirebaseAuth.getInstance()
        checkUser()

        //edit gender
        var sex : String
        updateGender.setOnClickListener{
            val genderDialogForm = androidx.appcompat.app.AlertDialog.Builder(this)
                .setMessage("Choose your sex")
                .setPositiveButton("Male") { _, _ ->
                    sex = "Male"
                    updateGender(sex, firebaseAuth.currentUser!!.uid)
                }
                .setNeutralButton("Female") { _, _ ->
                    sex = "Female"
                    updateGender(sex, firebaseAuth.currentUser!!.uid)
                }
            genderDialogForm.show()
        }

        //edit birthday
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        var bday : String
        var cYear = year
        var cMonth = month

        var cDay = day
        updateBday.setOnClickListener{
            val showCalendar = DatePickerDialog(this, { _, mYear, mMonth, dayOfMonth ->
                //set to calendar
                cYear = mYear
                cMonth = mMonth
                cDay = dayOfMonth
                var fMonth = mMonth + 1
                bday = "$fMonth/$dayOfMonth/$mYear"
                cal.set(cYear,cMonth,dayOfMonth)
                updateBirthday(bday, firebaseAuth.currentUser!!.uid)

            },year,month,day)
            showCalendar.show()
        }


        navigationView.setNavigationItemSelectedListener {
            when(it.itemId){
                //Profile
                R.id.item1 -> startActivity(Intent(this,this::class.java))
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

        val googleSignInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build()

        googleSignInClient = GoogleSignIn.getClient(this,googleSignInOptions)
    }

    private fun updateBirthday(birthday: String, userId: String) {
        database = FirebaseDatabase.getInstance().getReference("Users")
        val user = mapOf<String, String>(
            "birthday" to birthday
        )

        database.child(userId).updateChildren(user)
            .addOnSuccessListener {
            profile_bday.text = birthday
        }.addOnFailureListener {
            Toast.makeText(this,"Update failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateGender(sex: String, userId: String) {
        database = FirebaseDatabase.getInstance().getReference("Users")
        val user = mapOf<String, String>(
            "sex" to sex
        )

        database.child(userId).updateChildren(user).addOnSuccessListener {
            profile_sex.text = sex
            if(sex=="Male"){
                image.setImageResource(R.drawable.male)
            }
            else if(sex=="Female")
            {
                image.setImageResource(R.drawable.female)
            }
        }.addOnFailureListener {
            Toast.makeText(this,"Update failed", Toast.LENGTH_SHORT).show()
        }
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
                val uBday = it.child("birthday").value
                val uSex = it.child("sex").value
                val uEmail = it.child("email").value

                if(uSex.toString().isNotEmpty()){
                    profile_sex.text = uSex.toString()

                    if(uSex.toString()=="Male"){
                        image.setImageResource(R.drawable.male)
                    }
                    else if(uSex.toString()=="Female")
                    {
                        image.setImageResource(R.drawable.female)
                    }
                }
                else{
                    profile_sex.text = "Set your sex"
                }


                if(uBday.toString().isNotEmpty())
                {
                    profile_bday.text = uBday.toString()
                }
                else{
                    profile_bday.text = "Set your birthday"
                }

                //other info
                navUsername.text = uName.toString()
                navUserEmail.text = uEmail.toString()
                profile_fullname.text = uName.toString()
                profile_email.text = uEmail.toString()
                namenote.text = uName.toString()


            }
        }
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


}
