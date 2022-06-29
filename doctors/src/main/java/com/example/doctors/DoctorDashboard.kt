package com.example.doctors

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.activity_doctor_dashboard.*
import kotlinx.android.synthetic.main.activity_doctor_dashboard.bdayText
import kotlinx.android.synthetic.main.activity_doctor_dashboard.cardCurrent
import kotlinx.android.synthetic.main.activity_doctor_dashboard.imageView
import kotlinx.android.synthetic.main.activity_doctor_dashboard.nameText
import kotlinx.android.synthetic.main.activity_doctor_dashboard.queueBtn
import kotlinx.android.synthetic.main.activity_doctor_dashboard.rsnText
import kotlinx.android.synthetic.main.activity_doctor_dashboard.slot1
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class DoctorDashboard : AppCompatActivity() {
    // layout properties
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // database
    private lateinit var docDatabase: DatabaseReference
    private lateinit var patientDatabase: DatabaseReference
    private lateinit var tempDocDatabase: DatabaseReference
    private lateinit var tempDatabase: DatabaseReference
    private lateinit var database: DatabaseReference

    // private storage to store booking count
    var count = 0

    // for alarm
    private lateinit var calendarFP : Calendar
    private lateinit var alarmManager : AlarmManager
    private lateinit var pendingIntent: PendingIntent
    var checker = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.title = "Home"
        supportActionBar?.subtitle = "Doctor Dashboard"

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this,drawerLayout,R.string.open,R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val bundle: Bundle? = intent.extras
        val intentPatient = Intent(this, CurrentPatient::class.java)
        val intentHistory = Intent(this, History::class.java)
        val intentProfileUpdate = Intent(this, ProfileUpdate::class.java)
        val intentLogin = Intent(this, DoctorLogin::class.java)
        val intentManage = Intent(this, ManageQueue::class.java)

        // init doctor details
        val id = bundle!!.getString("id")
        val firstName = bundle.getString("firstName")
        val lastName = bundle.getString("lastName")
        val spec = bundle.getString("spec")
        val sex = bundle.getString("sex")
        val consTime = bundle!!.getInt("consTime")

        val headerView: View = navigationView.getHeaderView(0)
        val navName: TextView = headerView.findViewById(R.id.nav_user_name)
        val navId: TextView = headerView.findViewById(R.id.nav_user_id)

        navName.text = "$firstName $lastName"
        navId.text = "ID: $id"

        greetingname.text = "Hi Dr. $lastName,"

        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())

        if(firstName.isNullOrEmpty() && lastName.isNullOrEmpty())    {
            val incDetailsDialogForm = androidx.appcompat.app.AlertDialog.Builder(this, R.style.AlertDialog)
                .setCancelable(false)
                .setTitle("You have incomplete account details.")
                .setMessage("Set-up your profile now!")
                .setPositiveButton("OK") { _, _ ->
                    intentProfileUpdate.putExtra("id", id)
                    startActivity(intentProfileUpdate)
                }
            incDetailsDialogForm.show()
        }

        val sdf = SimpleDateFormat("MMddyy")
        val date = sdf.format(Date())

        docDatabase.get().addOnSuccessListener { doctor ->
            val dateAnswered = doctor.child("hasAnsweredScreening").child("date").value.toString()
            val result = doctor.child("hasAnsweredScreening").child("result").value.toString()

            val c = Calendar.getInstance()
            try {
                c.time = sdf.parse(dateAnswered)
            }   catch (e: ParseException)  {
                e.printStackTrace()
            }
            c.add(Calendar.DATE, 5)
            val newDate = sdf.format(c.time)

            if(date.toInt() < newDate.toInt() && dateAnswered.toInt() != 0) {
                scanBtn.visibility = GONE
            }   else  {
                scanBtn.visibility = VISIBLE
                scanBtn.setOnClickListener {
                    //QR CODE
                    val scanner = IntentIntegrator(this)
                    scanner.addExtra("id", id)
                    scanner.addExtra("firstName", firstName)
                    scanner.addExtra("lastName", lastName)
                    scanner.addExtra("spec", spec)
                    scanner.addExtra("sex", sex)
                    scanner.addExtra("consTime", consTime.toString())
                    scanner.initiateScan()
                }
            }

            if(doctor.child("notifyFP").child("date").value.toString() != date && doctor.child("notifyFP").child("value").value == false) {
                if(doctor.child("queue").exists())  {
                    if(doctor.child("queue").child(date).exists())  {
                        if(doctor.child("queue").child(date).children.elementAt(0).exists())    {
                            val firstPatientTS = doctor.child("queue").child(date).children.elementAt(0).key.toString()
                            val firstPatientID = doctor.child("queue").child(date).children.elementAt(0).value.toString()
                            val sdfTime = SimpleDateFormat("HH:mm")
                            val sdfHour = SimpleDateFormat("HH")
                            val sdfMins = SimpleDateFormat("mm")

                            val c2 = Calendar.getInstance()
                            try {
                                c2.time = sdfTime.parse(firstPatientTS)
                            }   catch (e: ParseException)  {
                                e.printStackTrace()
                            }

                            c2.add(Calendar.HOUR, -1)
                            val alarmTimeHour = sdfHour.format(c2.time).toInt()
                            val alarmTimeMins = sdfMins.format(c2.time).toInt()

                            calendarFP = Calendar.getInstance()
                            calendarFP[Calendar.HOUR_OF_DAY] = alarmTimeHour
                            calendarFP[Calendar.MINUTE] = alarmTimeMins
                            calendarFP[Calendar.SECOND] = 0
                            calendarFP[Calendar.MILLISECOND] = 0

                            docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
                            docDatabase.child("notifyFP").child("date").setValue(date)

                            // set notification channel
                            createNotificationChannelFP()

                            // set alarm
                            firstPatientAlarm(id.toString(), firstPatientTS)

                            // check if doctor can make it for their first patient
                            firstPatientChecker(id.toString(), firstPatientID, firstPatientTS)
                        }
                    }
                }
            }
        }

        queueBtn.setOnClickListener {
            val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this,queueBtn,"transTitle")
            intentManage.putExtra("id", id)
            intentManage.putExtra("firstName", firstName)
            intentManage.putExtra("lastName", lastName)
            intentManage.putExtra("spec", spec)
            intentManage.putExtra("sex", sex)
            intentManage.putExtra("consTime", consTime)
            startActivity(intentManage,options.toBundle())
        }

        calendarBtn.setOnClickListener {
            val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this,calendarBtn,"transTitle")
            intentHistory.putExtra("id", id)
            intentHistory.putExtra("firstName", firstName)
            intentHistory.putExtra("lastName", lastName)
            intentHistory.putExtra("spec", spec)
            intentHistory.putExtra("sex", sex)
            intentHistory.putExtra("consTime", consTime)
            startActivity(intentHistory,options.toBundle())
        }

        cardCurrent.setOnClickListener {
            val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this,cardCurrent,"cServing")
            intentPatient.putExtra("id", id)
            intentPatient.putExtra("firstName", firstName)
            intentPatient.putExtra("lastName", lastName)
            intentPatient.putExtra("spec", spec)
            intentPatient.putExtra("sex", sex)
            intentPatient.putExtra("consTime", consTime)
            startActivity(intentPatient,options.toBundle())
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when(item.itemId){
                R.id.profile    -> {
                    val intentProfile = Intent(this, DoctorProfile::class.java)
                    passData(intentProfile, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime)
                }
                R.id.history    -> {
                    passData(intentHistory, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime)
                }
                R.id.logout ->  {
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

        docDatabase.get().addOnSuccessListener { docSnap ->
            // get current patient
            val curr = docSnap.child("currentPatient")

            if(curr.exists())   {
                patientDatabase = FirebaseDatabase.getInstance().getReference("Users").child(curr.value.toString())
                patientDatabase.get().addOnSuccessListener { p ->
                    for(i in p.child("bookings").children)   {
                        if(i.child("startTime").exists() && !(i.child("endTime").exists()))   {
                            updateCurrentPatient(p, i, id.toString())
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

            if(!(docSnap.child("schedule").exists()))   {
                currentlyServing.text = "Set your schedule to start serving patients!"
                slot1.visibility = GONE
                nameText.visibility = GONE
                bdayText.visibility = GONE
                rsnText.visibility = GONE
                imageView.visibility = GONE
            }
        }

        // check for new notifs
        checkQueueNotif(id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime)
        listenForLatestMessages(id.toString())
    }

    // function to check if there are new patients in queue
    private fun checkQueueNotif(id: String, firstName: String, lastName: String, spec: String, sex: String, consTime: Int) {
        // initialize database
        tempDocDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)

        val queueListener = object : ValueEventListener    {
            override fun onDataChange(snapshot: DataSnapshot) {
                checkQueue(id, firstName, lastName, spec, sex, consTime)
                count = 0
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }

        tempDocDatabase.child("queue").addValueEventListener(queueListener)
    }

    private fun checkQueue(id: String, firstName: String, lastName: String, spec: String, sex: String, consTime: Int)    {
        tempDocDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
        tempDocDatabase.get().addOnSuccessListener { doc ->
            for (i in doc.child("queue").children) {
                val date = i.key.toString()
                for (j in doc.child("queue").child(date).children) {
                    count++
                }
            }

            val dbCount = doc.child("queueCount").value.toString().toInt()

            val childEventListener = object: ChildEventListener  {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    if(dbCount != count)  {
                        if(count > dbCount) {
                            val added = count - dbCount
                            tempDocDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
                            tempDocDatabase.child("queueCount").setValue(count)
                            if(added > 1)   {
                                val sharePreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                                val idSP = sharePreferences.getString("id", null)
                                if(idSP == id)  {
                                    notificationQueue("You have $added new patients in queue.", id, firstName, lastName, spec, sex, consTime)
                                }
                            }   else if(added == 1) {
                                val sharePreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                                val idSP = sharePreferences.getString("id", null)
                                if(idSP == id)  {
                                    notificationQueue("You have $added new patient in queue.", id, firstName, lastName, spec, sex, consTime)
                                }
                            }

                        }   else if(count < dbCount)    {
                            val subbed = dbCount - count
                            tempDocDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id)
                            tempDocDatabase.child("queueCount").setValue(count)
                            if(subbed > 1)  {
                                notificationQueue("$subbed patients has been removed from your queue.", id, firstName, lastName, spec, sex, consTime)
                            }   else if(subbed == 1)    {
                                notificationQueue("$subbed patient has been removed from your queue.", id, firstName, lastName, spec, sex, consTime)
                            }
                        }
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    //notification("Queue changes.", intent)
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    //notification("Cancelled booking.", id, firstName, lastName, spec, sex, consTime)
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    TODO("Not yet implemented")
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            }

            tempDocDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
            tempDocDatabase.child(id).child("queue").addChildEventListener(childEventListener)
        }
    }

    private fun notificationQueue(s: String, id: String, firstName: String, lastName: String, spec: String, sex: String, consTime: Int)  {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel: NotificationChannel = NotificationChannel("n", "n", importance)
                .apply { setShowBadge(false) }
            channel.enableVibration(false)
            channel.enableLights(true)
            channel.lightColor = Color.RED

            val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, ManageQueue::class.java)
        notificationIntent.putExtra("id", id)
        notificationIntent.putExtra("firstName", firstName)
        notificationIntent.putExtra("lastName", lastName)
        notificationIntent.putExtra("spec", spec)
        notificationIntent.putExtra("sex", sex)
        notificationIntent.putExtra("consTime", consTime)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "n")
            .setContentTitle("Doctors App")
            .setContentText(s)
            .setSmallIcon(R.drawable.ic_notif)
            .setAutoCancel(true)
            //.setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
        val managerCompat: NotificationManagerCompat = NotificationManagerCompat.from(this)
        managerCompat.notify(1234, builder.build())
    }

    // function to check if there are new patients in queue
    private fun listenForLatestMessages(id: String) {
        database = FirebaseDatabase.getInstance().getReference("latestMessages")

        val childEventListener = object: ChildEventListener  {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key.toString()
                val split = key.split("-")
                val to = snapshot.child("to").value.toString()
                val from = snapshot.child("from").value.toString()
                val dateFormatted = formatDate(split[1])

                if(to == id && from != id)    {
                    patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
                    patientDatabase.child(from).get().addOnSuccessListener { patient ->
                        val name = patient.child("name").value.toString()
                        notificationChat("New message from $name.", "For their booking on $dateFormatted at ${split[2]}.", id)
                    }
                }
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                //notification("Cancelled booking.", id, firstName, lastName, spec, sex, consTime)
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }

        database.child(id).addChildEventListener(childEventListener)
    }

    private fun notificationChat(s: String, sub: String, id: String)  {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel: NotificationChannel = NotificationChannel("n", "n", importance)
                .apply { setShowBadge(false) }
            channel.enableVibration(false)
            channel.enableLights(true)
            channel.lightColor = Color.RED

            val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, Chat::class.java)
        notificationIntent.putExtra("id", id)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, "n")
            .setContentTitle(s)
            .setContentText(sub)
            .setSmallIcon(R.drawable.ic_notif)
            .setAutoCancel(true)
            //.setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
        val managerCompat: NotificationManagerCompat = NotificationManagerCompat.from(this)
        managerCompat.notify(1234, builder.build())
    }

    private fun createNotificationChannelFP() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)  {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel: NotificationChannel = NotificationChannel("notif3", "n", importance)
                .apply { setShowBadge(false) }
            channel.enableVibration(false)
            channel.enableLights(true)
            channel.lightColor = Color.RED

            val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun firstPatientAlarm(id: String, slot: String) {
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        val intent = Intent(this,AlarmReceiverFP::class.java)
        intent.putExtra("id", id)
        intent.putExtra("slot", slot)
        pendingIntent = PendingIntent.getBroadcast(this,0,intent,0)

        //.setRepeating - set repeat alarm , .set - setAlarm for once
        //.setExact - set exact point of time
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,calendarFP.timeInMillis,
            pendingIntent
        )

        Toast.makeText(this,"First patient reminder set." +
                "${calendarFP[Calendar.MONTH]} ${calendarFP[Calendar.DATE]} ${calendarFP[Calendar.YEAR]} ${calendarFP[Calendar.HOUR_OF_DAY]} ${calendarFP[Calendar.MINUTE]}",Toast.LENGTH_LONG).show()
    }

    private fun firstPatientChecker(id: String, patientID: String, slot: String)    {
        val valueEventListener = object : ValueEventListener    {
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.value == true)   {
                    val dialog = androidx.appcompat.app.AlertDialog.Builder(this@DoctorDashboard, R.style.AlertDialog)
                        .setTitle("Reminder for your first patient.")
                        .setMessage("Can you make it in time for your first appointment today at $slot?")
                        .setPositiveButton("YES") { _, _ ->
                            val sdfDate = SimpleDateFormat("MMddyy")
                            val today = sdfDate.format(Date())
                            patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
                            patientDatabase.child(patientID).get().addOnSuccessListener { patient ->
                                if(patient.child("bookings").exists())  {
                                    for(i in patient.child("bookings").children)   {
                                        if(i.child("doctor").value.toString() == id && i.child("date").value.toString() == today && i.child("timeslot").value.toString() == slot)   {
                                            val updateChildren = mapOf(
                                                "value" to true,
                                                "doctor" to id,
                                                "newTS" to slot,
                                                "date" to today,
                                                "timeslot" to slot,
                                                "booking" to i.key.toString(),
                                                "delayTime" to 0,
                                                "traveltime" to i.child("traveltime").value.toString()
                                            )

                                            patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
                                            patientDatabase.child(patientID).child("states").child("doctorIsOnTime")
                                                .updateChildren(updateChildren).addOnSuccessListener {
                                                Toast.makeText(this@DoctorDashboard, "Yay!", Toast.LENGTH_SHORT).show()
                                            }

                                            docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                                            docDatabase.child(id).child("notifyFP").child("value").setValue(false)
                                            docDatabase.child(id).child("doctorIsOnTime").removeValue()
                                            docDatabase.child(id).child("doctorIsOnTime").child("value").setValue(true)
                                            docDatabase.child(id).child("doctorIsOnTime").child("patientID").setValue(patientID)
                                            docDatabase.child(id).child("doctorIsOnTime").child("timeslot").setValue(slot)
                                        }
                                    }
                                }
                            }
                        }
                        .setNegativeButton("NO")    { _, _ ->
                            chooseDelayTime(id, patientID, slot)
                            docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                            docDatabase.child(id).child("notifyFP").child("value").setValue(false)
                        }
                    dialog.show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }

        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
        docDatabase.child(id).child("notifyFP").child("value").addValueEventListener(valueEventListener)
    }

    private fun chooseDelayTime(id: String, patientID: String, slot: String) {
        val items: Array<String> = resources.getStringArray(R.array.delay_time)
        val checkedItem = -1
        val builder1: AlertDialog.Builder? = AlertDialog.Builder(this)
            .setIcon(R.drawable.ic_sad)
            .setTitle("Delay Time")
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                //action code
                Handler().postDelayed({
                    val builder: AlertDialog.Builder? = AlertDialog.Builder(this)
                        .setTitle("Delay Time")
                        .setMessage("Delay first patient for ${items[which]}?")
                        .setPositiveButton("Yes") { _, _ ->
                            // set new timeslot for first patient
                            val sdf = SimpleDateFormat("HH:mm")
                            val delayTime = items[which].filter { it.isDigit() }
                            val c = Calendar.getInstance()
                            try {
                                c.time = sdf.parse(slot)
                            }   catch (e: ParseException)  {
                                e.printStackTrace()
                            }
                            c.add(Calendar.MINUTE, delayTime.toInt())
                            val newTS = sdf.format(c.time)

                            val sdfDate = SimpleDateFormat("MMddyy")
                            val today = sdfDate.format(Date())

                            patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
                            patientDatabase.child(patientID).get().addOnSuccessListener { patient ->
                                if(patient.child("bookings").exists())  {
                                    for(i in patient.child("bookings").children)   {
                                        if(i.child("doctor").value.toString() == id && i.child("date").value.toString() == today && i.child("timeslot").value.toString() == slot)   {
                                            val updateChildren = mapOf(
                                                "value" to false,
                                                "doctor" to id,
                                                "newTS" to newTS,
                                                "date" to today,
                                                "timeslot" to slot,
                                                "booking" to i.key.toString(),
                                                "delayTime" to delayTime,
                                                "traveltime" to i.child("traveltime").value.toString()
                                            )

                                            patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
                                            patientDatabase.child(patientID).child("states").child("doctorIsOnTime")
                                                .updateChildren(updateChildren).addOnSuccessListener {
                                                    Toast.makeText(this@DoctorDashboard, "Aww :( No worries, we will notify your patient immediately!", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                }
                            }

                            docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                            docDatabase.child(id).child("doctorIsOnTime").child("value").setValue(false)
                            docDatabase.child(id).child("doctorIsOnTime").child("patientID").setValue(patientID)
                            docDatabase.child(id).child("doctorIsOnTime").child("timeslot").setValue(slot)
                            docDatabase.child(id).child("doctorIsOnTime").child("delayTime").setValue(delayTime)
                            docDatabase.child(id).child("doctorIsOnTime").child("newTS").setValue(newTS)
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

    private fun formatDate(date: String): String {
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

        return dateFormat.format(DateTimeFormatter.ofPattern("EE, MMM dd"))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode,resultCode,data)
        //val bundle: Bundle? = data?.extras
        //val id = bundle!!.getString("id")

        if(result!=null){
            if(result.contents==null){
                Toast.makeText(this,"Cancelled", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this,"Scanned: " + result.contents, Toast.LENGTH_SHORT).show()

                if(result.contents == "https://qbmsthesis102821.page.link/doctorscreening")   {
                    // to receive deep links for qr code scan
                    // direct to screening form
                    FirebaseDynamicLinks.getInstance()
                        .getDynamicLink(intent)
                        .addOnSuccessListener {
                            Log.i("DoctorDashboard", "Dynamic Link Success")
                            // get doctor details
                            val bundle: Bundle? = intent?.extras
                            val id = bundle!!.getString("id")
                            val firstName = bundle.getString("firstName")
                            val lastName = bundle.getString("lastName")
                            val spec = bundle.getString("spec")
                            val sex = bundle.getString("sex")
                            val consTime = bundle.getInt("consTime")

                            // get deep link from result (may be null if no link is found)
                            var deepLink: Uri? = null
                            if(it != null)  {
                                deepLink = it.link
                            }

                            val intent = Intent(this, Screening_q1::class.java)
                            intent.putExtra("id", id)
                            intent.putExtra("firstName", firstName)
                            intent.putExtra("lastName", lastName)
                            intent.putExtra("spec", spec)
                            intent.putExtra("sex", sex)
                            intent.putExtra("consTime", consTime)
                            startActivity(intent)
                        }
                        .addOnFailureListener {
                            Log.w(ContentValues.TAG, "getDynamicLink:onFailure", it)
                        }
                }   else    {
                    val intentQR = Intent(this, QRScan::class.java)
                    intentQR.putExtra("link", result.contents)
                    startActivity(intentQR)
                }
            }
        }
        else{
            super.onActivityResult(requestCode, resultCode, data)
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

    private fun passData(intent: Intent, id: String, firstName: String, lastName: String, spec: String, sex: String, consTime: Int)    {
        intent.putExtra("id", id)
        intent.putExtra("firstName", firstName)
        intent.putExtra("lastName", lastName)
        intent.putExtra("spec", spec)
        intent.putExtra("sex", sex)
        intent.putExtra("consTime", consTime)
        startActivity(intent)
    }
}