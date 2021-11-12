package com.example.doctors

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_current_patient.*
import kotlinx.android.synthetic.main.activity_current_patient.bdayText
import kotlinx.android.synthetic.main.activity_current_patient.cardCurrent
import kotlinx.android.synthetic.main.activity_current_patient.imageView
import kotlinx.android.synthetic.main.activity_current_patient.nameText
import kotlinx.android.synthetic.main.activity_current_patient.refreshBtn
import kotlinx.android.synthetic.main.activity_current_patient.rsnText
import kotlinx.android.synthetic.main.activity_current_patient.slot1

class CurrentPatient: AppCompatActivity() {
    // layout properties
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // database
    private lateinit var docDatabase: DatabaseReference
    private lateinit var patientDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer_current_patient)

        supportActionBar?.title = "Current Queue"
        supportActionBar?.subtitle = "Current Patient"

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val intentQueue = Intent(this, CurrentQueue::class.java)
        val intentDashboard = Intent(this, DoctorDashboard::class.java)
        val intentHistory = Intent(this, History::class.java)
        val intentProfile = Intent(this, DoctorProfile::class.java)

        // init doctor details
        val bundle: Bundle? = intent.extras
        val id = bundle!!.getString("id")
        val firstName = bundle.getString("firstName")
        val lastName = bundle.getString("lastName")
        val spec = bundle.getString("spec")
        val sex = bundle.getString("sex")
        val schedule = bundle.getString("schedule")
        val consTime = bundle.getInt("consTime")

        val headerView: View = navigationView.getHeaderView(0)
        val navName: TextView = headerView.findViewById(R.id.nav_user_name)
        val navId: TextView = headerView.findViewById(R.id.nav_user_id)

        navName.text = "$firstName $lastName"
        navId.text = "ID: $id"

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
                            val intentLogin = Intent(this, DoctorLogin::class.java)
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
                            docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                            docDatabase.get().addOnSuccessListener { doc ->
                                for (doctorUserSnapShot in doc.children) {
                                    if (doctorUserSnapShot.key.toString() == id) {
                                        //CHANGE VALUE ISLOGGEDIN == FALSE
                                        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                                        docDatabase.child(id).child("isLoggedIn").setValue(false)
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

        cardCurrent.setOnClickListener {
            val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this,cardCurrent,"cServing")
            intentQueue.putExtra("id", id)
            intentQueue.putExtra("firstName", firstName)
            intentQueue.putExtra("lastName", lastName)
            intentQueue.putExtra("spec", spec)
            intentQueue.putExtra("sex", sex)
            intentQueue.putExtra("consTime", consTime)
            intentQueue.putExtra("schedule", schedule)
            startActivity(intentQueue,options.toBundle())
        }

        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())

        // get current patient
        docDatabase.get().addOnSuccessListener { doc ->
            if(doc.child("currentPatient").exists())   {
                val curr = doc.child("currentPatient")
                patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(curr.value.toString())
                patientDatabase.get().addOnSuccessListener { p ->
                    for(i in p.child("bookings").children)   {
                        if(i.child("startTime").exists() && !(i.child("endTime").exists()))   {
                            val startTime = i.child("startTime").value.toString()
                            consultationStart.text = "Consultation started at $startTime"
                            updateCurrentPatient(p, i, id.toString())
                            break
                        }
                    }
                }
            }   else    {
                slot1.text = "You are currently not serving any patient."
                nameText.visibility = View.GONE
                bdayText.visibility = View.GONE
                rsnText.visibility = View.GONE
                specText.visibility = View.GONE
                imageView.visibility = View.GONE
                times.visibility = View.GONE
            }

            if(doc.child("avgConsTime").exists())   {
                val constime = doc.child("avgConsTime")
                // set text for average consultation time
                textConsultationTime.text = "Your Ave. Consultation Time: ${constime.value} minutes"
            }
        }

        refreshBtn.setOnClickListener{
            // get current patient
            docDatabase.get().addOnSuccessListener { doc ->
                val curr = doc.child("currentPatient")

                patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(curr.value.toString())
                patientDatabase.get().addOnSuccessListener { p ->
                    for(i in p.child("bookings").children)   {
                        if(i.child("startTime").exists() && !(i.child("endTime").exists()))   {
                            updateCurrentPatient(p, i, id.toString())
                            break
                        }
                    }
                }
            }
        }
    }

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

    private fun updateCurrentPatient(snapshot: DataSnapshot, i: DataSnapshot, id: String)  {
        if(i.child("doctor").value.toString() == id && !(i.child("endTime").exists()))    {
            val name = snapshot.child("name").value.toString()
            val sex = snapshot.child("sex").value.toString()
            val bday = snapshot.child("birthday").value.toString()
            val slot = i.child("timeslot").value.toString()
            val date = i.child("date").value.toString()
            val reason = i.child("reason").value.toString()
            val spec = i.child("spec").value.toString()
            slot1.text = "$date - $slot"
            nameText.text = name
            specText.text = "For $spec"
            bdayText.text = "Birthdate: $bday"
            rsnText.text = "Reason: $reason"
            if(sex == "Male")    {
                imageView.setImageResource(R.drawable.p_male)
                imageView.adjustViewBounds = true
                imageView.maxHeight = 180
                imageView.maxWidth = 180
            }   else if(sex == "Female") {
                imageView.setImageResource(R.drawable.p_female)
                imageView.adjustViewBounds = true
                imageView.maxHeight = 180
                imageView.maxWidth = 180
            }
        }
    }

    private fun passData(intent: Intent, id: String, firstName: String, lastName: String, spec: String, sex: String, schedule: String, consTime: Int)    {
        intent.putExtra("id", id)
        intent.putExtra("firstName", firstName)
        intent.putExtra("lastName", lastName)
        intent.putExtra("spec", spec)
        intent.putExtra("sex", sex)
        intent.putExtra("consTime", consTime)
        intent.putExtra("schedule", schedule)
        startActivity(intent)
    }
}