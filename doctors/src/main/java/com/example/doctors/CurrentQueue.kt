package com.example.doctors

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
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
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_current_queue.*
import kotlinx.android.synthetic.main.activity_current_queue.cardCurrent
import java.text.ParseException
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

    private var GRACE_PERIOD: Int = 5
    private var diff: Int = 0
    private var lapasCheck: Boolean = false
    private var alarmTime: String = ""
    private var pastETCheck: String = ""
    private var dateAndTS2: String = ""
    private var lateCheck = false
    private var delayTime = 0
    private var tsToUse = ""

    // variables for alarm
    private lateinit var calendarLP : Calendar
    private lateinit var alarmManager : AlarmManager
    private lateinit var pendingIntent: PendingIntent

    @SuppressLint("SetTextI18n")
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

        val sdf = SimpleDateFormat("MMddyy")
        val currentDate = sdf.format(Date())

        // set grace period
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
        docDatabase.child(id.toString()).get().addOnSuccessListener { doc ->
            GRACE_PERIOD =
                if (doc.child("gracePeriod").exists()) {
                    val gracePrdDB = doc.child("gracePeriod").value.toString().filter { it.isDigit() }
                    gracePrdDB.toInt()
                } else {
                    5
                }

            if(!(doc.child("currentPatient").exists())  && !(doc.child("history").child(currentDate).exists()))    {
                // to check if first patient in queue is late
                // set alarm if the doctor is currently not serving any patient
                firstPersonIsLateChecker(id.toString(), "$firstName $lastName")
            }
        }

        // set views
        refresh(context, id.toString())

        // refresh queue
        refreshBtn.setOnClickListener {
            // set views
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

    @SuppressLint("SimpleDateFormat")
    private fun  firstPersonIsLateChecker(id: String, name: String)  {
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
        docDatabase.child(id).get().addOnSuccessListener { doc ->
            // set alarm for when first patient in queue is late
            if(doc.child("queue").exists()) {
                if(doc.child("queue").children.elementAt(0).exists())   {
                    val firstDate = doc.child("queue").children.elementAt(0).key.toString()
                    val firstPatientTS = doc.child("queue").child(firstDate).children.elementAt(0).key.toString()
                    val firstPatientID = doc.child("queue").child(firstDate).children.elementAt(0).value.toString()
                    val newTS = doc.child("doctorIsOnTime").child("newTS").value.toString()

                    tsToUse =
                        if(doc.child("doctorIsOnTime").child("value").value.toString() == "false")  {
                            newTS
                        }   else    {
                            firstPatientTS
                        }

                    docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                    docDatabase.child(id).child("doctorIsOnTime").child("value").setValue(true)

                    val dateAndTS = "$firstDate $tsToUse"

                    val sdf = SimpleDateFormat("MMddyy HH:mm")
                    val sdfDate = SimpleDateFormat("MMddyy")
                    val sdfTime = SimpleDateFormat("HH:mm")
                    val sdfHour = SimpleDateFormat("HH")
                    val sdfMins = SimpleDateFormat("mm")
                    //val today = sdfDate.format(Date())

                    val c = Calendar.getInstance()
                    try {
                        c.time = sdf.parse(dateAndTS)
                    }   catch (e: ParseException)  {
                        e.printStackTrace()
                    }

                    c.add(Calendar.MINUTE, GRACE_PERIOD)
                    // set alarm time to delete next patient if they are late
                    alarmTime = sdfTime.format(c.time)
                    val alarmTimeHour = sdfHour.format(c.time).toInt()
                    val alarmTimeMins = sdfMins.format(c.time).toInt()
                    val alarmTimeDate = sdfDate.format(c.time)
                    val dateFormatted = formatDate(alarmTimeDate).split("-")

                    calendarLP = Calendar.getInstance()
                    calendarLP[Calendar.MONTH] = dateFormatted[0].toInt() - 1
                    calendarLP[Calendar.DAY_OF_MONTH] = dateFormatted[1].toInt()
                    calendarLP[Calendar.YEAR] = dateFormatted[2].toInt() + 2000
                    calendarLP[Calendar.HOUR_OF_DAY] = alarmTimeHour
                    calendarLP[Calendar.MINUTE] = alarmTimeMins
                    calendarLP[Calendar.SECOND] = 0
                    calendarLP[Calendar.MILLISECOND] = 0

                    // set notification channel
                    createNotificationChannelLP()

                    // set alarm
                    latePatientAlarm(id, firstPatientID, firstDate, firstPatientTS, name, "No Show/Late Patient")

                    // check late patient
                    latePatientChecker(id, firstPatientID, firstDate, firstPatientTS, name, "No Show/Late Patient")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refresh(context: Context, id: String)   {
        // refresh card view of queue
        queueView.removeAllViews()
        // get queue status from database
        getQueueStatus(context, id)

        // get current patient & current waiting room count
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
        docDatabase.get().addOnSuccessListener { doc ->
            val curr = doc.child("currentPatient")
            val clinicConfig = doc.child("clinicConfig")

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

            if(clinicConfig.exists())   {
                val currWaitingCount = clinicConfig.child("currWaitingCount").value
                val maxCap = clinicConfig.child("maxCap").value
                if(!(doc.child("queue").exists()))  {
                    docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
                    docDatabase.child("clinicConfig").child("currWaitingCount").setValue(0).addOnSuccessListener {
                        waitingRoomStat.text = "Waiting Room Status: 0 / $maxCap"
                    }
                }   else    {
                    waitingRoomStat.text = "Waiting Room Status: $currWaitingCount / $maxCap"
                }
            }

        }
    }

    // end session
    @SuppressLint("SimpleDateFormat")
    private fun getTime(): List<String> {
        // get current date and time
        val sdf = SimpleDateFormat("MMddyy HH:mm")
        val currentDate = sdf.format(Date())
        return currentDate.toString().split(" ")
    }

    private fun getQueue(context: Context, id: String, queueMap: MutableMap<String, String>)    {
        var counter = 0
        var WRCount = 0
        queueMap.forEach    { patient ->
            val dtSplit = patient.key.split("-")

            patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(patient.value)
            // call function to create cardview for queue
            patientDatabase.get().addOnSuccessListener { snapshot ->
                val isArrived =
                    if(snapshot.child("states").child("isArrived").exists())    {
                        if(snapshot.child("states").child("isArrived").value == "true") {
                            "true"
                        }   else    {
                            "false"
                        }
                    }   else    {
                        "false"
                    }

                for(i in snapshot.child("bookings").children)   {
                    if(i.child("doctor").value.toString() == id && !(i.child("endTime").exists()) && i.child("date").exists() && i.child("date").value.toString() == dtSplit[0] && i.child("timeslot").value.toString() == dtSplit[1])    {
                        counter++
                        val name = snapshot.child("name").value.toString()
                        val slot = i.child("timeslot").value.toString()
                        val date = i.child("date").value.toString()
                        val reason = i.child("reason").value.toString()
                        val spec = i.child("spec").value.toString()
                        val state =
                            if(i.child("fillUpTime").exists() && isArrived == "true")  {
                                "pass"
                            }   else  {
                                "fail"
                            }

                        addCard(queueView, context, "Patient #$counter", name, slot, date, "Reason: $reason", spec, state)

                        if(state == "fail") {
                            counter--

                            docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
                            docDatabase.get().addOnSuccessListener { doc ->
                                val clinicConfig = doc.child("clinicConfig")
                                val maxCap = clinicConfig.child("maxCap").value

                                if(WRCount < 0) {
                                    docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
                                    docDatabase.child("clinicConfig").child("currWaitingCount").setValue(0).addOnSuccessListener {
                                        waitingRoomStat.text = "Waiting Room Status: 0 / $maxCap"
                                    }
                                }   else    {
                                    docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
                                    docDatabase.child("clinicConfig").child("currWaitingCount").setValue(counter).addOnSuccessListener {
                                        waitingRoomStat.text = "Waiting Room Status: $counter / $maxCap"
                                    }
                                }
                            }
                        }   else    {
                            docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
                            docDatabase.get().addOnSuccessListener { doc ->
                                val clinicConfig = doc.child("clinicConfig")
                                val maxCap = clinicConfig.child("maxCap").value.toString().toInt()

                                docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
                                docDatabase.child("clinicConfig").child("currWaitingCount").setValue(counter).addOnSuccessListener {
                                    if(WRCount > maxCap)    {
                                        waitingRoomStat.text = "Waiting Room Status: $maxCap / $maxCap"
                                    }   else    {
                                        waitingRoomStat.text = "Waiting Room Status: $counter / $maxCap"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
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
                docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
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

    @SuppressLint("SimpleDateFormat")
    private fun getQueueStatus(context: Context, id: String)    {
        // list of patients in queue
        val queueList = mutableListOf<String>()
        val queueMap = mutableMapOf<String, String>()

        val sdf = SimpleDateFormat("MMddyy")
        val date = sdf.format(Date())

        // initialize databases
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)

        // updating current queue
        docDatabase.get().addOnSuccessListener { doc ->
            val firstName = doc.child("firstName").value.toString()
            val lastName = doc.child("lastName").value.toString()
            val dateAnswered = doc.child("hasAnsweredScreening").child("date").value.toString().toInt()

            val c = Calendar.getInstance()
            try {
                c.time = sdf.parse(dateAnswered.toString())
            }   catch (e: ParseException)  {
                e.printStackTrace()
            }
            c.add(Calendar.DATE, 5)
            val newDate = sdf.format(c.time)

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

                val first = queueList[0]
                val sdf2 = SimpleDateFormat("MMddyy")
                val currentDate = sdf2.format(Date())
                val sdfTime = SimpleDateFormat("HH:mm")
                val currentTime = sdfTime.format(Date())

                startBtn.setOnClickListener {
                    if(doc.child("currentPatient").exists())    {
                        val startDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                            .setTitle("Start Session")
                            .setMessage("Please end session with current patient.")
                            .setNeutralButton("OK") { _, _ ->
                                refresh(context, id)
                            }
                        startDialog.show()
                    }   //else if(date.toInt() >= newDate.toInt())   {
//                        val startDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
//                            .setTitle("Health Declaration Form not yet filled up.")
//                            .setMessage("Please scan the QR Code to complete the health declaration form first.")
//                            .setNeutralButton("OK") { _, _ ->
//                                refresh(context, id)
//                            }
//                        startDialog.show()
                       else {
                        patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(first)
                        patientDatabase.get().addOnSuccessListener { p ->
                            for (i in p.child("bookings").children) {
                                val d = i.child("date").value.toString()
                                val s = i.child("timeslot").value.toString()
                                val check = "$d-$s"
                                val state =
                                    if(i.child("fillUpTime").exists())  {
                                        "pass"
                                    }   else  {
                                        "fail"
                                    }
                                val startSplit = s.split(":")
                                val endSplit = currentTime.split(":")

                                // check if the patient before went past the timeslot of first patient in queue
                                val historyList = mutableListOf<String>()
                                if(doc.child("history").child(currentDate).exists())    {
                                    for(i in doc.child("history").child(currentDate).children)  {
                                        historyList.add(i.key.toString())
                                    }

                                    val lastPatientTS = historyList[historyList.size - 1]
                                    pastETCheck = doc.child("history").child(currentDate).child(lastPatientTS).child("endTime").value.toString()
                                    val pecSplit = pastETCheck.split(":")

                                    // if end time of past patient went past the timeslot of next patient, end time of past patient will be used as basis for grace period
                                    // if end time of past patient stayed within the average consultation time of the doctor, timeslot of next patient will be used as basis for grace period
                                    lapasCheck =
                                        if(startSplit[0] == pecSplit[0])    {
                                            pecSplit[1] > startSplit[1]
                                        }   else pecSplit[0] > startSplit[0]

                                    // diff represents the minutes past the start time/end time of past patient
                                    // its value depends on whether the past patient went past the timeslot of next patient (lapasCheck)
                                    diff =
                                        if(lapasCheck)  {
                                            when {
                                                pecSplit[0] == endSplit[0] -> {
                                                    endSplit[1].toInt() - pecSplit[1].toInt()
                                                }
                                                endSplit[0] < pecSplit[0] -> {
                                                    0
                                                }
                                                else -> {
                                                    (60 - pecSplit[1].toInt()) + endSplit[1].toInt()
                                                }
                                            }
                                        }   else    {
                                            when {
                                                startSplit[0] == endSplit[0] -> {
                                                    endSplit[1].toInt() - startSplit[1].toInt()
                                                }
                                                endSplit[0] < startSplit[0] -> {
                                                    0
                                                }
                                                else -> {
                                                    (60 - startSplit[1].toInt()) + endSplit[1].toInt()
                                                }
                                            }
                                        }
                                }

                                if (doc.child("queue").child(d).child(s).value.toString() == p.key.toString()) {
                                    if (d == currentDate && state == "pass" && diff <= GRACE_PERIOD) {
                                        val startDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                                                .setTitle("Start Session")
                                                .setMessage("Serve next patient in queue scheduled for $s? It is currently $currentTime")
                                                .setPositiveButton("YES") { _, _ ->
                                                    val updateCurrentPatient = mapOf(
                                                        "currentPatient" to first
                                                    )
                                                    docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
                                                    docDatabase.updateChildren(updateCurrentPatient)
                                                    queueList.removeAt(0)

                                                    updateCurrentPatient(p, i, id)
                                                    getStartTime(p, i, id, check)
//                                                    lapasCheck = false

                                                    // cancel alarm for late patient
                                                    cancelAlarm()
                                                }
                                                .setNeutralButton("NO") { _, _ -> }
                                        startDialog.show()
                                    }   else if(d == currentDate)    {
                                        if(state == "fail")    {    //if(diff <= GRACE_PERIOD && state == "fail")    {
                                            val startDialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                                                .setTitle("Start Session")
                                                .setMessage("The next patient is still not ready to be served. $diff minutes has passed since their booked timeslot.")
                                                .setNeutralButton("OK") { _, _ -> }
                                            startDialog.show()
                                        }   //else    {
//                                            val dialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
//                                                .setTitle("Late Patient")
//                                                .setMessage("The next patient in queue is late for their appointment. Their booking will be cancelled immediately.")
//                                                .setNeutralButton("OK") { _, _ ->
//                                                    cancelBooking(id, p.key.toString(), date, s, name, reason)
//                                                    docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
//                                                    docDatabase.child(id).child("patientIsLate").setValue(false)
//                                                }
//                                            dialog.show()
//                                        }
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
                                endSession(id, date, context, "$firstName $lastName")
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
                                endSession(id, date, context, "$firstName $lastName")
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

    // function to perform necessary events when ending session
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun endSession(id: String, date: String, context: Context, name: String)    {
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
        docDatabase.get().addOnSuccessListener { doc ->
            val curr = doc.child("currentPatient")
            val act = doc.child("avgConsTime").value.toString().toInt()

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

            // set alarm for when first patient in queue is late
            if(doc.child("queue").exists()) {
                if(doc.child("queue").children.elementAt(0).exists())   {
                    val firstDate = doc.child("queue").children.elementAt(0).key.toString()
                    val firstPatientTS = doc.child("queue").child(firstDate).children.elementAt(0).key.toString()
                    val firstPatientID = doc.child("queue").child(firstDate).children.elementAt(0).value.toString()
                    val dateAndTS = "$firstDate $firstPatientTS"

                    val sdf = SimpleDateFormat("MMddyy HH:mm")
                    val sdfDate = SimpleDateFormat("MMddyy")
                    val sdfTime = SimpleDateFormat("HH:mm")
                    val sdfHour = SimpleDateFormat("HH")
                    val sdfMins = SimpleDateFormat("mm")
                    val currentDate = sdfDate.format(Date())
                    val currentTime = sdfTime.format(Date())

                    val c = Calendar.getInstance()
                    val c2 = Calendar.getInstance()
                    try {
                        c.time = sdf.parse(dateAndTS)
                        c2.time = sdfTime.parse(firstPatientTS)
                    }   catch (e: ParseException)  {
                        e.printStackTrace()
                    }

                    c.add(Calendar.MINUTE, GRACE_PERIOD)        // if last patient did not go past expected end time
                    //c2.add(Calendar.MINUTE, act)                // to check if last patient went past expected end time

                    val endTime = sdfTime.format(c2.time)

                    val c3 = Calendar.getInstance()
                    try {
                        c3.time = sdfTime.parse(currentTime)
                    }   catch (e: ParseException)  {
                        e.printStackTrace()
                    }
                    c3.add(Calendar.MINUTE, GRACE_PERIOD)       // current time + grace period
                                                                // to be used if last patient went past expected end time

                    if(currentDate == firstDate)    {
                        if(currentTime > endTime) {
                            // set alarm time to delete next patient if they are late
                            Toast.makeText(applicationContext, "LAPAS: $currentTime $endTime", Toast.LENGTH_SHORT).show()
                            alarmTime = sdfTime.format(c3.time)
                            val alarmTimeHour = sdfHour.format(c3.time).toInt()
                            val alarmTimeMins = sdfMins.format(c3.time).toInt()
                            val dateFormatted = formatDate(currentDate).split("-")

                            calendarLP = Calendar.getInstance()
                            calendarLP[Calendar.MONTH] = dateFormatted[0].toInt() - 1
                            calendarLP[Calendar.DAY_OF_MONTH] = dateFormatted[1].toInt()
                            calendarLP[Calendar.YEAR] = dateFormatted[2].toInt() + 2000
                            calendarLP[Calendar.HOUR_OF_DAY] = alarmTimeHour
                            calendarLP[Calendar.MINUTE] = alarmTimeMins
                            calendarLP[Calendar.SECOND] = 0
                            calendarLP[Calendar.MILLISECOND] = 0

                            // set notification channel
                            createNotificationChannelLP()

                            // set alarm
                            latePatientAlarm(id, firstPatientID, firstDate, firstPatientTS, name, "No Show/Late Patient")

                            // check late patient
                            latePatientChecker(id, firstPatientID, firstDate, firstPatientTS, name, "No Show/Late Patient")
                        }   else if(currentTime <= endTime)    {
                            // set alarm time to delete next patient if they are late
                            Toast.makeText(applicationContext, "NOT LAPAS: $currentTime $endTime", Toast.LENGTH_SHORT).show()
                            alarmTime = sdfTime.format(c.time)
                            val alarmTimeHour = sdfHour.format(c.time).toInt()
                            val alarmTimeMins = sdfMins.format(c.time).toInt()
                            val alarmTimeDate = sdfDate.format(c.time)
                            val dateFormatted = formatDate(alarmTimeDate).split("-")

                            calendarLP = Calendar.getInstance()
                            calendarLP[Calendar.MONTH] = dateFormatted[0].toInt() - 1
                            calendarLP[Calendar.DAY_OF_MONTH] = dateFormatted[1].toInt()
                            calendarLP[Calendar.YEAR] = dateFormatted[2].toInt() + 2000
                            calendarLP[Calendar.HOUR_OF_DAY] = alarmTimeHour
                            calendarLP[Calendar.MINUTE] = alarmTimeMins
                            calendarLP[Calendar.SECOND] = 0
                            calendarLP[Calendar.MILLISECOND] = 0

                            // set notification channel
                            createNotificationChannelLP()

                            // set alarm
                            latePatientAlarm(id, firstPatientID, firstDate, firstPatientTS, name, "No Show/Late Patient")

                            // check late patient
                            latePatientChecker(id, firstPatientID, firstDate, firstPatientTS, name, "No Show/Late Patient")
                        }
                    }
                }
            }
        }

        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
        docDatabase.child("currentPatient").removeValue().addOnSuccessListener {
            slot1.text = "You are currently not serving any patient."
            nameText.visibility = GONE
            bdayText.visibility = GONE
            rsnText.visibility = GONE
            imageView.visibility = GONE
        }
        refresh(context, id)
    }

    private fun latePatientChecker(id: String, patientID: String, date: String, slot: String, name: String, reason: String)    {
        val valueEventListener = object : ValueEventListener    {
            override fun onDataChange(snapshot: DataSnapshot) {
                //Toast.makeText(this@CurrentQueue,"${snapshot.key.toString()} ${snapshot.value.toString()}",Toast.LENGTH_LONG).show()
                if(snapshot.value == true)   {
                    val dialog = androidx.appcompat.app.AlertDialog.Builder(this@CurrentQueue, R.style.AlertDialog)
                        .setTitle("Late Patient")
                        .setMessage("The next patient in queue is late for their appointment. Their booking will be cancelled immediately.")
                        .setNeutralButton("OK") { _, _ ->
                            cancelBooking(id, patientID, date, slot, name, reason)
                            docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                            docDatabase.child(id).child("patientIsLate").setValue(false)
                        }
                    dialog.show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }

        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
        docDatabase.child(id).child("patientIsLate").addValueEventListener(valueEventListener)
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
                        database = FirebaseDatabase.getInstance().getReference("DOCTOR")
                        database.child(id).child("patientIsLate").setValue(false)
                        val currentCount = doctor.child("queueCount").value.toString().toInt()
                        docDatabase.child(id).child("queueCount").setValue(currentCount-1)
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
                                refresh(this, id)
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

    // function to add patient as card
    @SuppressLint("SetTextI18n")
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

        cardLinearLayout.addView(linearLayoutSub)
        cardLinearLayout.addView(slot)
        cardLinearLayout.addView(name)
        cardLinearLayout.addView(spec)
        cardLinearLayout.addView(reason)
        cardView.addView(cardLinearLayout)
        layout.addView(cardView)
    }

    // functions for alarm
    private fun cancelAlarm() {
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this,AlarmReceiverLP::class.java)

        pendingIntent = PendingIntent.getBroadcast(this,0,intent,0)

        alarmManager.cancel(pendingIntent)

        Toast.makeText(this,"First alarm for late patient cancelled.",Toast.LENGTH_LONG).show()
    }

    private fun createNotificationChannelLP() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel: NotificationChannel = NotificationChannel("notif2", "n", importance)
                .apply { setShowBadge(false) }
            channel.enableVibration(false)
            channel.enableLights(true)
            channel.lightColor = Color.RED

            val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun latePatientAlarm(id: String, patientID: String, date: String, slot: String, name: String, reason: String) {
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        val intent = Intent(this,AlarmReceiverLP::class.java)
        intent.putExtra("id", id)
        intent.putExtra("patientID", patientID)
        intent.putExtra("date", date)
        intent.putExtra("slot", slot)
        intent.putExtra("name", name)
        intent.putExtra("reason", reason)
        pendingIntent = PendingIntent.getBroadcast(this,0,intent,0)

        //.setRepeating - set repeat alarm , .set - setAlarm for once
        //.setExact - set exact point of time
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,calendarLP.timeInMillis,
            pendingIntent
        )

        Toast.makeText(this,"You will be notified if the next patient in queue is late." +
                "${calendarLP[Calendar.MONTH]} ${calendarLP[Calendar.DATE]} ${calendarLP[Calendar.YEAR]} ${calendarLP[Calendar.HOUR_OF_DAY]} ${calendarLP[Calendar.MINUTE]}",Toast.LENGTH_LONG).show()
    }

    private fun formatDate(date: String): String {
        val strbld: StringBuilder
        strbld = StringBuilder()
        strbld.append(date)
        val year = "${strbld[4]}${strbld[5]}"
        val month = "${strbld[0]}${strbld[1]}"
        val day = "${strbld[2]}${strbld[3]}"

        return "$month-$day-$year"
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