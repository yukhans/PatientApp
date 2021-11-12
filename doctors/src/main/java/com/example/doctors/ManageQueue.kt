package com.example.doctors

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_manage_queue.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.collections.arrayListOf as arrayListOf

class ManageQueue : AppCompatActivity() {
    // layout properties
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // database
    private lateinit var docDatabase: DatabaseReference
    private lateinit var patientDatabase: DatabaseReference
    private lateinit var database: DatabaseReference
    private lateinit var tempDatabase: DatabaseReference

    // selection
    val selection = Selection()

    var queueCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer_manage_queue)

        supportActionBar?.title = "Queue"
        supportActionBar?.subtitle = "Manage Queue"

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val intentDashboard = Intent(this, DoctorDashboard::class.java)
        val intentPatient = Intent(this, CurrentPatient::class.java)
        val intentQueue = Intent(this, CurrentQueue::class.java)
        val intentHistory = Intent(this, History::class.java)
        val intentProfile = Intent(this, DoctorProfile::class.java)
        val intentLogin = Intent(this, DoctorLogin::class.java)

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

        // initialize recycler view
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        getQueueStatus(id.toString())

        refreshBtn.setOnClickListener {
            getQueueStatus(id.toString())
        }

        cqueueBtn.setOnClickListener {
            val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this,cqueueBtn,"transTitle")
            intentQueue.putExtra("id", id)
            intentQueue.putExtra("firstName", firstName)
            intentQueue.putExtra("lastName", lastName)
            intentQueue.putExtra("spec", spec)
            intentQueue.putExtra("sex", sex)
            intentQueue.putExtra("consTime", consTime)
            startActivity(intentQueue,options.toBundle())
        }

        cardCurrent.setOnClickListener {
            val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this,cardCurrent,"transTitle")
            intentPatient.putExtra("id", id)
            intentPatient.putExtra("firstName", firstName)
            intentPatient.putExtra("lastName", lastName)
            intentPatient.putExtra("spec", spec)
            intentPatient.putExtra("sex", sex)
            intentPatient.putExtra("consTime", consTime)
            startActivity(intentPatient,options.toBundle())
        }

        cancelBtn.setOnClickListener {
            if(selection.getChoice().isNotEmpty())  {
                val temp = selection.getChoice()
                val split = temp.split("-")
                val patientID = split[5]
                val date = split[1]
                val slot = split[2]

                // initialize date format
                // divide date to individual digits
                val sixth = date.toInt().mod(10)
                val fifth = ((date.toInt().mod(100)).minus(sixth)).div(10)
                val fourth = ((date.toInt().mod(1000)).minus(date.toInt().mod(100))).div(100)
                val third = ((date.toInt().mod(10000)).minus(date.toInt().mod(1000))).div(1000)
                val second = ((date.toInt().mod(100000)).minus(date.toInt().mod(10000))).div(10000)
                val first = ((date.toInt().mod(1000000)).minus(date.toInt().mod(100000))).div(100000)

                val year = "$fifth$sixth"
                val day = "$third$fourth"
                val month = "$first$second"

                val dateFormat: LocalDate = LocalDate.of(year.toInt(), month.toInt(), day.toInt())
                val dateFormatted: String = dateFormat.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd"))

                val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.AlertDialog)
                    .setTitle("Cancel Booking?")
                    .setMessage("Are you sure you want to cancel booking for $dateFormatted at $slot?")
                    .setPositiveButton("YES")   {_, _ ->
                        chooseReason(id.toString(), patientID, date, slot, "$firstName $lastName")
                    }
                    .setNegativeButton("NO")    {_, _ ->}
                dialog.show()
            }
        }
    }

    // function to get queue from database
    private fun getQueueStatus(id: String)    {
        val queueMap = mutableMapOf<String, String>()

        // initialize databases
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)

        // updating current queue
        docDatabase.get().addOnSuccessListener { doc ->
            if(doc.child("queue").exists())  {
                noqueueText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                for(i in doc.child("queue").children) {
                    val date = i.key.toString()
                    for(j in doc.child("queue").child(date).children) {
                        val slot = j.key.toString()
                        val patient = j.value.toString()
                        queueMap["$date-$slot"] = patient
                    }
                }

                queueCount = queueMap.count()

                // create queue view
                getQueue(id, queueMap)
            }   else    {
                recyclerView.visibility = View.GONE
                noqueueText.visibility = View.VISIBLE
            }
        }
    }

    // function to display queue
    private fun getQueue(id: String, queueMap: MutableMap<String, String>)    {
        val newQueueList: ArrayList<String> = arrayListOf()
        var counter = 0
        queueMap.forEach    { patient ->
            val dtSplit = patient.key.split("-")
            patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(patient.value)
            // call function to create cardview for queue
            patientDatabase.get().addOnSuccessListener { snapshot ->
                for(i in snapshot.child("bookings").children)   {
                    if(i.child("doctor").value.toString() == id && !(i.child("endTime").exists()) && i.child("date").exists() && i.child("date").value.toString() == dtSplit[0] && i.child("timeslot").value.toString() == dtSplit[1])    {
                        counter++
                        val name = snapshot.child("name").value.toString()
                        val slot = i.child("timeslot").value.toString()
                        val date = i.child("date").value.toString()
                        val spec = i.child("spec").value.toString()

                        val patientDetails = "$counter-$date-$slot-$name-$spec-${snapshot.key.toString()}"
                        newQueueList.add(patientDetails)
                    }
                }

                recyclerView.adapter = AdapterQueue(newQueueList, selection)
                recyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }

    // function to cancel selected booking from database
    private fun cancelBooking(id: String, patientID: String, date: String, slot: String, name: String, reason: String)   {
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
        patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(patientID)
        database = FirebaseDatabase.getInstance().getReference("messages")
        tempDatabase = FirebaseDatabase.getInstance().getReference("latestMessages")

        // remove from doctor queue
        docDatabase.get().addOnSuccessListener { doctor ->
            if(doctor.child("queue").exists())  {
                docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                docDatabase.child(id).child("queue")
                    .child(date).child(slot).removeValue()
                    .addOnSuccessListener {
                        getQueueStatus(id)
                    }

                val check = "$date-$slot"

                // delete message node
                database.get().addOnSuccessListener { messages ->
                    for(i in messages.children) {
                        val split = i.key.toString().split("-")
                        if(split[0] == patientID && split[1] == id)  {
                            for(j in i.children)    {
                                if(j.key.toString() == check)   {
                                    database.child(i.key.toString()).child(check).removeValue()
                                }
                            }
                        }
                    }
                }

                // delete in latest messages node
                tempDatabase.child(id).child("$patientID-$check").removeValue()
                tempDatabase.child(patientID).child("$id-$check").removeValue()
            }
        }

        // remove from patient bookings
        patientDatabase.get().addOnSuccessListener { patient ->
            if(patient.child("bookings").exists())  {
                for(i in patient.child("bookings").children)    {
                    if(i.child("doctor").value.toString() == id && i.child("date").value.toString() == date && i.child("timeslot").value.toString() == slot)    {
                        patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
                        patientDatabase.child(patientID).child("bookings")
                            .child(i.key.toString()).removeValue()
                            .addOnSuccessListener {
                                getQueueStatus(id)
                                patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
                                patientDatabase.child(patientID).child("states").child("doctorCancelled").child("booking").setValue(i.key.toString())
                                patientDatabase.child(patientID).child("states").child("doctorCancelled").child("reason").setValue(reason)
                                patientDatabase.child(patientID).child("states").child("doctorCancelled").child("uid").setValue(id)
                                patientDatabase.child(patientID).child("states").child("doctorCancelled").child("name").setValue(name)
                            }
                    }
                }
            }
        }
    }

    private fun chooseReason(id: String, patientID: String, date: String, slot: String, name: String) {
        val items: Array<String> = resources.getStringArray(R.array.reasons_list)
        val checkedItem = -1
        val builder1: AlertDialog.Builder? = AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_sad)
            .setTitle("Reasons for Cancellation")
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                //action code
                Handler().postDelayed({
                    val builder: AlertDialog.Builder? = AlertDialog.Builder(this)
                        .setTitle("CONFIRM CANCELLATION")
                        .setMessage("Are you sure you want to cancel your booking because of ${items[which]}?")
                        .setPositiveButton("Yes") { _, _ ->
                            cancelBooking(id, patientID, date, slot, name, items[which])
                            dialog.dismiss()
                        }
                        .setNeutralButton("No") {_, _ ->}
                    val dialog2 : AlertDialog? = builder?.create()
                    dialog2?.show()
                    //Get alert dialog buttons
                    val positiveButton : Button = dialog2!!.getButton(AlertDialog.BUTTON_POSITIVE)
                    val neutralButton : Button = dialog2.getButton(AlertDialog.BUTTON_NEUTRAL)
                    positiveButton.setTextColor(Color.parseColor("#F36767"))
                    neutralButton.setTextColor(Color.parseColor("#F36767"))

                }, 800)

            }
            .setNegativeButton("CANCEL") { dialog, which ->
                //Toast.makeText(context, "Clicked CANCEL", Toast.LENGTH_SHORT).show()

            }
        val dialog1 : AlertDialog? = builder1?.create()
        dialog1?.show()

        //Get alert dialog buttons
        val negativeButton : Button = dialog1!!.getButton(AlertDialog.BUTTON_NEGATIVE)
        negativeButton.setTextColor(Color.parseColor("#F36767"))
    }

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
}