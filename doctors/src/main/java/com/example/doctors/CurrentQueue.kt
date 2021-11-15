package com.example.doctors

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.Image
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.marginRight
import androidx.core.view.setPadding
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_current_queue.*
import kotlinx.android.synthetic.main.activity_current_queue.cardCurrent
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class CurrentQueue : AppCompatActivity() {
    // layout properties
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // database
    private lateinit var docDatabase: DatabaseReference
    private lateinit var patientDatabase: DatabaseReference
    private lateinit var database: DatabaseReference
    private lateinit var tempDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer_current_queue)

        supportActionBar?.title = "Queue"
        supportActionBar?.subtitle = "Patients in Queue"

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

        val context = this

        // set views
        refresh(context, id.toString())

        // refresh queue
        refreshBtn.setOnClickListener {
            refresh(context, id.toString())
        }

        cardCurrent.setOnClickListener {
            val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this,cardCurrent,"cServing")
            intentPatient.putExtra("id", id)
            intentPatient.putExtra("firstName", firstName)
            intentPatient.putExtra("lastName", lastName)
            intentPatient.putExtra("spec", spec)
            intentPatient.putExtra("sex", sex)
            intentPatient.putExtra("schedule", schedule)
            intentPatient.putExtra("consTime", consTime)
            startActivity(intentPatient,options.toBundle())
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

    private fun refresh(context: Context, id: String)   {
        // refresh card view of queue
        queueView.removeAllViews()
        // get queue status from database
        getQueueStatus(context, id)

        // get current patient
        docDatabase.get().addOnSuccessListener { doc ->
            val curr = doc.child("currentPatient")

            if(curr.exists())   {
                patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(curr.value.toString())
                patientDatabase.get().addOnSuccessListener { p ->
                    for(i in p.child("bookings").children)   {
                        if(i.child("startTime").exists() && !(i.child("endTime").exists()))   {
                            updateCurrentPatient(p, i, id)
                            break
                        }
                    }
                }
            }   else    {
                slot1.text = "You are currently not serving any patient."
                nameText.visibility = GONE
                bdayText.visibility = GONE
                rsnText.visibility = GONE
                imageView.visibility = GONE
            }
        }
    }

    // end session
    private fun getTime(): List<String> {
        // get current date and time
        val sdf = SimpleDateFormat("MMddyy HH:mm")
        val currentDate = sdf.format(Date())
        return currentDate.toString().split(" ")
    }

    private fun getQueue(context: Context, id: String, queueMap: MutableMap<String, String>)    {
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
                        val reason = i.child("reason").value.toString()
                        val spec = i.child("spec").value.toString()
                        val state =
                            if(i.child("fillUpTime").exists())  {
                                "pass"
                            }   else  {
                                "fail"
                            }
                        addCard(queueView, context, "Patient #$counter", name, slot, date, "Reason: $reason", spec, state)
                    }
                }
            }
        }
    }

    private fun updateCurrentPatient(snapshot: DataSnapshot, i: DataSnapshot, id: String)  {
        if(i.child("doctor").value.toString() == id && !(i.child("endTime").exists()))    {
            val name = snapshot.child("name").value.toString()
            val sex = snapshot.child("sex").value.toString()
            val bday = snapshot.child("birthday").value.toString()
            val slot = i.child("timeslot").value.toString()
            val date = i.child("date").value.toString()
            val reason = i.child("reason").value.toString()
            nameText.visibility = VISIBLE
            bdayText.visibility = VISIBLE
            rsnText.visibility = VISIBLE
            imageView.visibility = VISIBLE
            slot1.text = "$date - $slot"
            nameText.text = name
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

    private fun getStartTime(snapshot: DataSnapshot, i: DataSnapshot, id: String, datetime: String)  {
        val dtSplit = datetime.split("-")
        if(i.child("doctor").value.toString() == id && !(i.child("endTime").exists()) && i.child("date").exists())    {
            if(i.child("date").value.toString() == dtSplit[0] && i.child("timeslot").value.toString() == dtSplit[1])    {
                // get current date and time
                val dateTime = getTime()
                val time = dateTime[1]

                val name = snapshot.child("name").value.toString()
                val bday = snapshot.child("birthday").value.toString()
                val slot = i.child("timeslot").value.toString()
                val date = i.child("date").value.toString()
                val reason = i.child("reason").value.toString()

                // add to history
                val updateHistory = mapOf(
                    "name" to name,
                    "bday" to bday,
                    "reason" to reason,
                    "startTime" to time
                )

                // add to patient booking
                val updateTime = mapOf(
                    "startTime" to time
                )

                patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(snapshot.key.toString())
                patientDatabase.child("bookings").child(i.key.toString()).updateChildren(updateTime)
                docDatabase.child("history").child(date).child(slot).updateChildren(updateHistory)
                docDatabase.child("queue").child(date).child(slot).removeValue()
            }
            refresh(this, id)
        }
    }

    private fun getEndTime(snapshot: DataSnapshot, i: DataSnapshot, id: String, datetime: String)    {
        val dtSplit = datetime.split("-")
        if(i.child("doctor").value.toString() == id && !(i.child("endTime").exists()) && i.child("date").exists())    {
            if(i.child("date").value.toString() == dtSplit[0] && i.child("timeslot").value.toString() == dtSplit[1])    {
                // get current date and time
                val dateTime = getTime()
                val time = dateTime[1]

                val slot = i.child("timeslot").value.toString()
                val date = i.child("date").value.toString()

                // add to history and patient booking
                val updateHistoryEnd = mapOf(
                    "endTime" to time
                )

                patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(snapshot.key.toString())
                patientDatabase.child("bookings").child(i.key.toString()).updateChildren(updateHistoryEnd)
                docDatabase.child("history").child(date).child(slot).updateChildren(updateHistoryEnd)

                val check = "$date-$slot"

                // delete message node
                database = FirebaseDatabase.getInstance().getReference("messages")
                database.get().addOnSuccessListener { messages ->
                    for(m in messages.children) {
                        val split = m.key.toString().split("-")
                        if(split[1] == id)  {
                            for(j in m.children)    {
                                if(j.key.toString() == check)   {
                                    database.child(m.key.toString()).child(check).removeValue()
                                    break
                                }
                            }
                        }
                    }
                }

                // delete in latest messages node
                tempDatabase = FirebaseDatabase.getInstance().getReference("latestMessages")
                tempDatabase.child(id).child("${snapshot.key.toString()}-$check").removeValue()
                tempDatabase.child(snapshot.key.toString()).child("$id-$check").removeValue()
            }
        }
    }

    private fun getQueueStatus(context: Context, id: String)    {
        // list of patients in queue
        val queueList = mutableListOf<String>()
        val queueMap = mutableMapOf<String, String>()

        val sdf = SimpleDateFormat("yyMMdd")
        val date = sdf.format(Date())

        // initialize databases
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)

        // updating current queue
        docDatabase.get().addOnSuccessListener { doc ->
            val dateAnswered = doc.child("hasAnsweredScreening").child("date").value.toString().toInt()
            val result = doc.child("hasAnsweredScreening").child("result").value.toString()

            if(doc.child("queue").exists())  {
                noqueueText.visibility = GONE
                for(i in doc.child("queue").children) {
                    val dateDB = i.key.toString()
                    for(j in doc.child("queue").child(dateDB).children) {
                        val slot = j.key.toString()
                        val patient = j.value.toString()
                        queueList.add(patient)
                        queueMap["$dateDB-$slot"] = patient
                    }
                }

                // create queue view
                getQueue(context, id, queueMap)

                startBtn.setOnClickListener {
                    if(doc.child("currentPatient").exists())    {
                        val startDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                            .setTitle("Start Session")
                            .setMessage("Please end session with current patient.")
                            .setNeutralButton("OK") { _, _ ->
                                refresh(context, id)
                            }
                        startDialog.show()
                    }   else if(date.toInt() >= (dateAnswered+5))   {
                        val startDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                            .setTitle("Health Declaration Form not yet filled up.")
                            .setMessage("Please scan the QR Code to complete the health declaration form first.")
                            .setNeutralButton("OK") { _, _ ->
                                refresh(context, id)
                            }
                        startDialog.show()
                    }   else {
                        val sdf2 = SimpleDateFormat("MMddyy")
                        val currentDate = sdf2.format(Date())
                        val sdfTime = SimpleDateFormat("HH:mm")
                        val currentTime = sdfTime.format(Date())
                        val first = queueList[0]

                        patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(first)
                        patientDatabase.get().addOnSuccessListener { p ->
                            for (i in p.child("bookings").children) {
                                val d = i.child("date").value.toString()
                                val s = i.child("timeslot").value.toString()
                                val check = "$d-$s"

                                if (doc.child("queue").child(d).child(s).value.toString() == p.key.toString()) {
                                    if (d == currentDate) {
                                        //counter++
                                        val startDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                                                .setTitle("Start Session")
                                                .setMessage("Serve next patient in queue scheduled for $s? It is currently $currentTime")
                                                .setPositiveButton("YES") { _, _ ->
                                                    val updateCurrentPatient = mapOf(
                                                        "currentPatient" to first
                                                    )
                                                    docDatabase.updateChildren(updateCurrentPatient)
                                                    queueList.removeAt(0)

                                                    updateCurrentPatient(p, i, id)
                                                    getStartTime(p, i, id, check)
                                                }
                                                .setNeutralButton("NO") { _, _ -> }
                                        startDialog.show()
                                    }   else {      // if next patient is not scheduled for current date
                                        val dialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                                            .setTitle("Patient not scheduled for today.")
                                            .setMessage("The next patient in queue is scheduled for a later date.")
                                            .setNeutralButton("OK") { _, _ -> }
                                        dialog.show()
                                    }
                                    break
                                }
                            }
                        }
                    }
                }

                endBtn.setOnClickListener {
                    if(doc.child("currentPatient").exists())    {
                        val endDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                            .setTitle("End Session")
                            .setMessage("End current session?")
                            .setPositiveButton("YES")   { _, _ ->
                                Toast.makeText(applicationContext, "End Session", Toast.LENGTH_SHORT).show()
                                docDatabase.get().addOnSuccessListener { doc ->
                                    val curr = doc.child("currentPatient")

                                    patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(curr.value.toString())
                                    patientDatabase.get().addOnSuccessListener { p ->
                                        for(i in p.child("bookings").children)   {
                                            val d = i.child("date").value.toString()
                                            val s = i.child("timeslot").value.toString()
                                            val check = "$d-$s"

                                            if(i.child("startTime").exists() && !(i.child("endTime").exists()))   {
                                                getEndTime(p, i, id, check)
                                                break
                                            }
                                        }
                                    }
                                }

                                docDatabase.child("currentPatient").removeValue()

                                slot1.text = "You are currently not serving any patient."
                                nameText.visibility = GONE
                                bdayText.visibility = GONE
                                rsnText.visibility = GONE
                                imageView.visibility = GONE
                                refresh(context, id)
                            }
                            .setNeutralButton("NO") { _, _ ->
                                refresh(context, id)
                            }
                        endDialog.show()
                    }   else    {
                        val endDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                            .setTitle("End Session")
                            .setMessage("You are currently not serving any patient.")
                            .setNeutralButton("OK") { _, _ ->
                                refresh(context, id)
                            }
                        endDialog.show()
                    }
                }
            }   else    {
                noqueueText.visibility = VISIBLE

                startBtn.setOnClickListener {
                    val startDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                        .setTitle("Start Session")
                        .setMessage("You have no patients in queue.")
                        .setNeutralButton("OK") { _, _ ->
                            refresh(context, id)
                        }
                    startDialog.show()
                }

                endBtn.setOnClickListener {
                    if(doc.child("currentPatient").exists())   {
                        val endDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                            .setTitle("End Session")
                            .setMessage("End current session?")
                            .setPositiveButton("YES")   { _, _ ->
                                Toast.makeText(applicationContext, "End Session", Toast.LENGTH_SHORT).show()
                                docDatabase.get().addOnSuccessListener { doc ->
                                    val curr = doc.child("currentPatient")

                                    patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(curr.value.toString())
                                    patientDatabase.get().addOnSuccessListener { p ->
                                        for(i in p.child("bookings").children)   {
                                            val d = i.child("date").value.toString()
                                            val s = i.child("timeslot").value.toString()
                                            val check = "$d-$s"

                                            if(i.child("startTime").exists() && !(i.child("endTime").exists()))   {
                                                getEndTime(p, i, id, check)
                                                break
                                            }
                                        }
                                    }
                                }

                                docDatabase.child("currentPatient").removeValue()
                                slot1.text = "You are currently not serving any patient."
                                nameText.visibility = GONE
                                bdayText.visibility = GONE
                                rsnText.visibility = GONE
                                imageView.visibility = GONE
                                refresh(context, id)
                            }
                            .setNeutralButton("NO") { _, _ ->
                                refresh(context, id)
                            }
                        endDialog.show()
                    }   else    {
                        val endDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                            .setTitle("End Session")
                            .setMessage("You are currently not serving any patient.")
                            .setNeutralButton("OK") { _, _ ->
                                refresh(context, id)
                            }
                        endDialog.show()
                    }

                }
            }
        }
    }

    // function to add patient as card
    private fun addCard(layout: LinearLayout, context: Context, titleStr: String, nameStr: String, slotStr: String, dateStr: String, reasonStr: String, specStr: String, stateStr: String) {
        // initialize card view
        val cardView = CardView(context)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(10, 40, 10, 30)
        val cardLinearLayout = LinearLayout(context)
        cardLinearLayout.orientation = LinearLayout.VERTICAL

        val linearLayoutSub = LinearLayout(context)
        linearLayoutSub.orientation = LinearLayout.HORIZONTAL

        cardView.setContentPadding(50, 50, 50, 50)
        cardView.layoutParams = layoutParams
        cardView.radius = 20F
        cardView.setCardBackgroundColor(Color.WHITE)
        cardView.cardElevation = 4F

        // initialize date format
        val sixth = dateStr.toInt().mod(10)
        val fifth = ((dateStr.toInt().mod(100)).minus(sixth)).div(10)
        val fourth = ((dateStr.toInt().mod(1000)).minus(dateStr.toInt().mod(100))).div(100)
        val third = ((dateStr.toInt().mod(10000)).minus(dateStr.toInt().mod(1000))).div(1000)
        val second = ((dateStr.toInt().mod(100000)).minus(dateStr.toInt().mod(10000))).div(10000)
        val first = ((dateStr.toInt().mod(1000000)).minus(dateStr.toInt().mod(100000))).div(100000)

        val year = "$fifth$sixth"
        val day = "$third$fourth"
        val month = "$first$second"

        val dateFormat: LocalDate = LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        val dateFormatted: String = dateFormat.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd"))

        // initialize views in card view
        val state = ImageView(context)
        if(stateStr == "pass")  {
            state.setImageResource(R.drawable.green_indicator_small)
        }   else if(stateStr == "fail") {
            state.setImageResource(R.drawable.red_indicator_small)
        }

        val title = TextView(context)
        title.text = titleStr
        title.textSize = 23F
        title.setTextColorRes(R.color.dark_salmon)
        title.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD)

        val name = TextView(context)
        name.text = nameStr
        name.textSize = 16F
        name.setTextColorRes(R.color.dark_gray)
        name.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD)

        val slot = TextView(context)
        slot.text = "$dateFormatted at $slotStr"
        slot.textSize = 18F
        slot.setTextColorRes(R.color.salmon)
        slot.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)

        val reason = TextView(context)
        reason.text = reasonStr
        reason.setTextColor(Color.GRAY)
        reason.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)

        val spec = TextView(context)
        spec.text = "For $specStr"
        spec.setTextColor(Color.GRAY)
        spec.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)

        // add text views to card view
        linearLayoutSub.addView(state)
        linearLayoutSub.addView(title)

        //cardLinearLayout.addView(state)
        //cardLinearLayout.addView(title)
        cardLinearLayout.addView(linearLayoutSub)
        cardLinearLayout.addView(slot)
        cardLinearLayout.addView(name)
        cardLinearLayout.addView(spec)
        cardLinearLayout.addView(reason)
        cardView.addView(cardLinearLayout)
        layout.addView(cardView)
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
}