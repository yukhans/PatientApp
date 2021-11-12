// documentation complete: Sep 8 2021

package com.example.doctors

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlinx.android.synthetic.main.activity_update_profile.*
import kotlinx.android.synthetic.main.activity_update_profile.back
import kotlinx.android.synthetic.main.activity_update_profile.confirmBtn

class ProfileUpdate : AppCompatActivity() {
    // layout properties
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // database
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer_profile_update)

        // intents
        val intentDashboard = Intent(this, DoctorDashboard::class.java)
        val intentQueue = Intent(this, CurrentQueue::class.java)
        val intentHistory = Intent(this, History::class.java)
        val intentProfile = Intent(this, DoctorProfile::class.java)
        val intentLogin = Intent(this, DoctorLogin::class.java)

        // init doctor details
        val bundle: Bundle? = intent.extras
        val id = bundle!!.getString("id")
        var firstName = bundle.getString("firstName")
        var lastName = bundle.getString("lastName")
        var spec = bundle.getString("spec")
        var sex = bundle.getString("sex")
        val schedule = bundle.getString("schedule")
        val consTime = bundle.getInt("consTime")

        // init navigation view
        supportActionBar?.title = "Profile"
        supportActionBar?.subtitle = "Update Details"

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val headerView: View = navigationView.getHeaderView(0)
        val navName: TextView = headerView.findViewById(R.id.nav_user_name)
        val navId: TextView = headerView.findViewById(R.id.nav_user_id)

        navName.text = "$firstName $lastName"
        navId.text = "ID: $id"

        // initialize button to go back to profile
        back.setOnClickListener {
            passData(intentProfile, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), schedule.toString(), consTime)
        }

        // read specializations in "Specialization" node in database
        val context = this
        database = FirebaseDatabase.getInstance().getReference("Specialization")
        val databaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // read values
                Log.d(ContentValues.TAG, "Children read.")
                val specNum = snapshot.childrenCount
                for(i in 1..specNum)    {
                    // add specialization to chip group
                    chipSpec.addChip(context, snapshot.child(i.toString()).getValue<String>().toString(), i.toInt())
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // failed to read value
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        }
        database.addValueEventListener(databaseListener)

        // final button to update profile details
        confirmBtn.setOnClickListener {
            spec = chipSpec.checkedChipIds.toString()

            // error trapping to check fields
            if(update_fname.text.isNotEmpty() && update_lname.text.isNotEmpty() && (radioMale.isChecked || radioFemale.isChecked) && chipSpec.checkedChipIds.size != 0 && chipSpec.checkedChipIds.size <= 3)    {
                // initialize inputs
                firstName = update_fname.text.toString()
                lastName = update_lname.text.toString()

                if(radioFemale.isChecked)   {
                    sex = "Female"
                }   else if(radioMale.isChecked)    {
                    sex = "Male"
                }

                // initialize update maps
                val fname = mapOf(
                    "firstName" to firstName.toString()
                )
                val lname = mapOf(
                    "lastName" to lastName.toString()
                )
                val sexUpdate = mapOf(
                    "sex" to sex.toString()
                )
                val specUpdate = mapOf(
                    "spec" to spec.toString()
                )

                // update values in database
                database = FirebaseDatabase.getInstance().getReference("DOCTOR")
                database.child(id.toString()).updateChildren(fname)
                database.child(id.toString()).updateChildren(lname)
                database.child(id.toString()).updateChildren(sexUpdate)
                database.child(id.toString()).updateChildren(specUpdate)

                passData(intentProfile, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), schedule.toString(), consTime)
                // if name fields are empty
            }   else if(update_lname.text.isNullOrEmpty() || update_fname.text.isNullOrEmpty())  {
                Toast.makeText(this,"Incomplete Credentials.", Toast.LENGTH_SHORT).show()
                update_fname.setText("")
                update_lname.setText("")
                radioSex.clearCheck()
                chipSpec.clearCheck()
                // if sex radio group is unchecked
            }   else if(!(radioMale.isChecked) && !(radioFemale.isChecked)) {
                Toast.makeText(this,"Please select your sex.", Toast.LENGTH_SHORT).show()
                update_fname.setText("")
                update_lname.setText("")
                chipSpec.clearCheck()
                // if specialization chip group is unchecked
            }   else if(chipSpec.checkedChipIds.size == 0)   {
                Toast.makeText(this,"Please select your specialization.", Toast.LENGTH_SHORT).show()
                update_fname.setText("")
                update_lname.setText("")
                radioSex.clearCheck()
                // if specialization exceeds max number (3)
            }   else if(chipSpec.checkedChipIds.size > 3)   {
                warningSpec.visibility = VISIBLE
                update_fname.setText("")
                update_lname.setText("")
                radioSex.clearCheck()
                chipSpec.clearCheck()
            }
        }

        // set navigation view
        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.profile -> {
                    passData(intentProfile, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), schedule.toString(), consTime)
                }
                R.id.dashboard -> {
                    passData(intentDashboard, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), schedule.toString(), consTime)
                }
                R.id.queue -> {
                    passData(intentQueue, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), schedule.toString(), consTime)
                }
                R.id.history -> {
                    passData(intentHistory, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), schedule.toString(), consTime)
                }
                R.id.logout -> {
                    val logoutDialogForm = androidx.appcompat.app.AlertDialog.Builder(this, R.style.AlertDialog)
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes") { _, _ ->
                            //saved data from local storage?
                            val sharePreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                            val editor = sharePreferences.edit()

                            //these are the parameters u want to save
                            editor.apply {
                                putString("id", id)
                                putString("firstName", firstName)
                                putString("lastName", lastName)
                                putString("spec", spec)
                                putString("sex", sex)
                                putInt("consTime", consTime)
                                putString("ISLOGGEDIN", "false")
                            }.apply()

                            // getDOCTOR
                            database = FirebaseDatabase.getInstance().getReference("DOCTOR")
                            database.get().addOnSuccessListener { doc ->
                                for (doctorUserSnapShot in doc.children) {
                                    if (doctorUserSnapShot.key.toString() == id) {
                                        //CHANGE VALUE ISLOGGEDIN == FALSE
                                        database = FirebaseDatabase.getInstance().getReference("DOCTOR")
                                        database.child(id).child("isLoggedIn").setValue(false)
                                    }
                                }
                                // TOAST TO NOTIFY
                                Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show()

                                // GO TO LOGIN PAGE
                                startActivity(intentLogin)
                            }.addOnFailureListener { exception ->
                                Toast.makeText(this, "$exception", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNeutralButton("No") { _, _ ->

                        }
                    logoutDialogForm.show()

                }
            }
            true
        }
    }

    // navigation functions
    override fun onOptionsItemSelected(item: MenuItem):Boolean{
        if(toggle.onOptionsItemSelected(item)){
            true
        }

        val bundle: Bundle? = intent.extras
        val id = bundle!!.getString("id")

        when(item.itemId)  {
            R.id.notif ->   {
                val intentChat = Intent(this, Chat::class.java)
                intentChat.putExtra("id", id)
                startActivity(intentChat)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    // function to add chip to chip group
    fun ChipGroup.addChip(context: Context, title: String, idNum: Int)  {
        Chip(context).apply {
            id = idNum
            text = title
            isClickable = true
            isCheckable = true
            isChipIconVisible = true
            isCheckedIconVisible = true
            isFocusable = true
            addView(this)
        }
    }

    // function to pass data to intent and start intent
    private fun passData(intent: Intent, id: String, firstName: String, lastName: String, spec: String, sex: String, schedule: String, consTime: Int)    {
        intent.putExtra("id", id)
        intent.putExtra("firstName", firstName)
        intent.putExtra("lastName", lastName)
        intent.putExtra("spec", spec)
        intent.putExtra("sex", sex)
        intent.putExtra("schedule", schedule)
        intent.putExtra("consTime", consTime)
        startActivity(intent)
    }
}