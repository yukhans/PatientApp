package com.example.doctors

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_chat_messages.*
import kotlinx.android.synthetic.main.chat_received.view.*
import kotlinx.android.synthetic.main.chat_sent.view.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ChatMessages : AppCompatActivity() {
    // database
    private lateinit var database: DatabaseReference
    private lateinit var lMessagesDatabase: DatabaseReference
    private lateinit var docDatabase: DatabaseReference

    // layout properties
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer_chat_messages)

        val bundle: Bundle? = intent.extras
        val name = bundle!!.getString("name")
        val dateTime = bundle.getString("dateTime")
        val patientID = bundle.getString("patientID")

        // initialize date format
        // divide date to individual digits
        val split = dateTime.toString().split("-")
        val date = split[0]
        val slot = split[1]
        val spec = split[2]
        val reason = split[3]
        val id = split[4]
        val dateSlot = "${split[0]}-${split[1]}"

        supportActionBar?.title = name

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this,drawerLayout,R.string.open,R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val dateFormatted = formatDate(date)

        dateTimeText.text = "$dateFormatted at $slot"
        specText.text = "Specialization: $spec"
        rsnText.text = "Reason: $reason"

        val adapter = GroupAdapter<GroupieViewHolder>()
        chatLogRecycler.setHasFixedSize(true)

        val intentQueue = Intent(this, CurrentQueue::class.java)
        val intentHistory = Intent(this, History::class.java)
        val intentLogin = Intent(this, DoctorLogin::class.java)
        val intentProfile = Intent(this, DoctorProfile::class.java)
        val intentDashboard = Intent(this, DoctorDashboard::class.java)

        // get doctor data from database
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
        docDatabase.get().addOnSuccessListener { doc ->
            val firstName = doc.child("firstName").value.toString()
            val lastName = doc.child("lastName").value.toString()
            val spec = doc.child("spec").value.toString()
            val sex = doc.child("sex").value.toString()
            val consTime = doc.child("avgConsTime").value.toString().toInt()

            val headerView: View = navigationView.getHeaderView(0)
            val navName: TextView = headerView.findViewById(R.id.nav_user_name)
            val navId: TextView = headerView.findViewById(R.id.nav_user_id)

            navName.text = "$firstName $lastName"
            navId.text = "ID: $id"

            navigationView.setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.profile -> {
                        passData(intentProfile, id, firstName, lastName, spec, sex, consTime)
                    }
                    R.id.dashboard -> {
                        passData(intentDashboard, id, firstName, lastName, spec, sex, consTime)
                    }
                    R.id.queue -> {
                        passData(intentQueue, id, firstName, lastName, spec, sex, consTime)
                    }
                    R.id.history -> {
                        passData(intentHistory, id, firstName, lastName, spec, sex, consTime)
                    }
                    R.id.logout -> {
                        val logoutDialogForm =
                            androidx.appcompat.app.AlertDialog.Builder(this, R.style.AlertDialog)
                                .setMessage("Are you sure you want to log out?")
                                .setPositiveButton("Yes") { _, _ ->
                                    //saved data from local storage?
                                    val sharePreferences =
                                        getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
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
                                    docDatabase =
                                        FirebaseDatabase.getInstance().getReference("DOCTOR")
                                    docDatabase.get().addOnSuccessListener { doc ->
                                        for (doctorUserSnapShot in doc.children) {
                                            if (doctorUserSnapShot.key.toString() == id) {
                                                //CHANGE VALUE ISLOGGEDIN == FALSE
                                                docDatabase = FirebaseDatabase.getInstance()
                                                    .getReference("DOCTOR")
                                                docDatabase.child(id).child("isLoggedIn")
                                                    .setValue(false)
                                            }
                                        }
                                        // TOAST TO NOTIFY
                                        Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show()

                                        // GO TO LOGIN PAGE
                                        startActivity(intentLogin)
                                    }.addOnFailureListener { exception ->
                                        Toast.makeText(this, "$exception", Toast.LENGTH_SHORT)
                                            .show()
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

        database = FirebaseDatabase.getInstance().getReference("messages")
        database.get().addOnSuccessListener { messages ->
            if (messages.exists()) {
                for (i in messages.children) {
                    val split1 = i.key.toString().split("-")
                    val patID = split1[0]
                    val docID = split1[1]

                    if (docID == id && patID == patientID) {
                        for (j in i.children) {
                            if (j.key.toString() == dateSlot) {
                                noChatText.visibility = GONE
                                chatLogRecycler.visibility = VISIBLE

                                for (d in j.children) {
                                    val to = d.child("to").value.toString()
                                    val from = d.child("from").value.toString()
                                    val message = d.child("message").value.toString()
                                    val dateTimeDB = d.key.toString()

                                    val split2 = dateTimeDB.split(" ")
                                    val timestampFull = split2[1]
                                    val timestampSplit = timestampFull.split(":")
                                    val timestamp = "${timestampSplit[0]}:${timestampSplit[1]}"
                                    val dateFormatted2 = formatDate(split2[0])

                                    if (to == docID) {
                                        adapter.add(ReceivedChatItem(message, timestamp, dateFormatted2))
                                    }   else if (from == docID) {
                                        adapter.add(SentChatItem(message, timestamp, dateFormatted2))
                                    }
                                }
                            }   //else {
                                //noChatText.visibility = VISIBLE
                                //chatLogRecycler.visibility = GONE
                            //}
                        }
                    }   //else    {
                        //noChatText.visibility = VISIBLE
                        //chatLogRecycler.visibility = GONE
                    //}
                }
            }   else {
                noChatText.visibility = VISIBLE
                chatLogRecycler.visibility = GONE
            }
        }

        sendButton.setOnClickListener {
            noChatText.visibility = GONE
            chatLogRecycler.visibility = VISIBLE

            if(enterMessage.text.isNotBlank() || enterMessage.text.isNotEmpty())    {
                val sdf = SimpleDateFormat("EE, MMM dd")
                val sdf2 = SimpleDateFormat("MMddyy HH:mm:ss")

                val dateF = sdf.format(java.util.Date())
                val dateTimeF = sdf2.format(java.util.Date())

                val splitF = dateTimeF.split(" ")
                val timestampFull = splitF[1]
                val timestampSplit = timestampFull.split(":")
                val timestamp = "${timestampSplit[0]}:${timestampSplit[1]}"
                val message = enterMessage.text.toString()

                val ids = "$patientID-$id"

                // add to main messages node in database
                database = FirebaseDatabase.getInstance().getReference("messages")
                database.child(ids).child(dateSlot).child(dateTimeF).child("from").setValue(id)
                database.child(ids).child(dateSlot).child(dateTimeF).child("to").setValue(patientID)
                database.child(ids).child(dateSlot).child(dateTimeF).child("message").setValue(message).addOnSuccessListener {
                    adapter.add(SentChatItem(message, timestamp, dateF))
                }

                // add to latest messages node
                // this will be useful for notifications
                lMessagesDatabase = FirebaseDatabase.getInstance().getReference("latestMessages")

                // for the doctors node
                lMessagesDatabase.child(id).child("${patientID.toString()}-$dateSlot").child("from").setValue(id)
                lMessagesDatabase.child(id).child("${patientID.toString()}-$dateSlot").child("to").setValue(patientID)
                lMessagesDatabase.child(id).child("${patientID.toString()}-$dateSlot").child("message").setValue(message)
                lMessagesDatabase.child(id).child("${patientID.toString()}-$dateSlot").child("timestamp").setValue(dateTimeF).addOnSuccessListener {
                    enterMessage.setText("")
                }

                // for the patients node
                lMessagesDatabase.child(patientID.toString()).child("$id-$dateSlot").child("from").setValue(id)
                lMessagesDatabase.child(patientID.toString()).child("$id-$dateSlot").child("to").setValue(patientID)
                lMessagesDatabase.child(patientID.toString()).child("$id-$dateSlot").child("message").setValue(message)
                lMessagesDatabase.child(patientID.toString()).child("$id-$dateSlot").child("timestamp").setValue(dateTimeF)

                // doctors and patients node are separate for easy initialization of
                // child listeners for notifications
            }
        }

        refreshBtn.setOnClickListener {
            listenForLatestMessages(id, patientID.toString(), dateSlot)
        }

        chatLogRecycler.adapter = adapter
    }

    private fun listenForLatestMessages(id: String, patientID: String, dateSlot: String) {
        val adapter = GroupAdapter<GroupieViewHolder>()
        chatLogRecycler.setHasFixedSize(true)

        database = FirebaseDatabase.getInstance().getReference("messages")
        database.get().addOnSuccessListener { messages ->
            if (messages.exists()) {
                for (i in messages.children) {
                    val split1 = i.key.toString().split("-")
                    val patID = split1[0]
                    val docID = split1[1]

                    if (docID == id && patID == patientID) {
                        for (j in i.children) {
                            if (j.key.toString() == dateSlot) {
                                noChatText.visibility = GONE
                                chatLogRecycler.visibility = VISIBLE

                                for (d in j.children) {
                                    val to = d.child("to").value.toString()
                                    val from = d.child("from").value.toString()
                                    val message = d.child("message").value.toString()
                                    val dateTime = d.key.toString()

                                    val split = dateTime.split(" ")
                                    val timestampFull = split[1]
                                    val timestampSplit = timestampFull.split(":")
                                    val timestamp = "${timestampSplit[0]}:${timestampSplit[1]}"
                                    val dateFormatted = formatDate(split[0])

                                    if (to == docID) {
                                        adapter.add(ReceivedChatItem(message, timestamp, dateFormatted))
                                    }   else if (from == docID) {
                                        adapter.add(SentChatItem(message, timestamp, dateFormatted))
                                    }
                                }
                            }   //else {
                                //noChatText.visibility = VISIBLE
                                //chatLogRecycler.visibility = GONE
                            //}
                        }
                    }   //else    {
                        //noChatText.visibility = VISIBLE
                        //chatLogRecycler.visibility = GONE
                    //}
                }
            }   else {
                noChatText.visibility = VISIBLE
                chatLogRecycler.visibility = GONE
            }
        }

        chatLogRecycler.adapter = adapter
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

    private fun passData(intent: Intent, id: String, firstName: String, lastName: String, spec: String, sex: String, consTime: Int)    {
        intent.putExtra("id", id)
        intent.putExtra("firstName", firstName)
        intent.putExtra("lastName", lastName)
        intent.putExtra("spec", spec)
        intent.putExtra("sex", sex)
        intent.putExtra("consTime", consTime)
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem):Boolean{
        if(toggle.onOptionsItemSelected(item)){
            true
        }
        return super.onOptionsItemSelected(item)
    }
}

class ReceivedChatItem(val message: String, val timestamp: String, val date: String): Item<GroupieViewHolder>()   {
    override fun bind(view: GroupieViewHolder, p1: Int) {
        view.itemView.receivedMessage.text = message
        view.itemView.timestampReceived.text = timestamp
        view.itemView.dateReceived.text = date
    }

    override fun getLayout(): Int {
        return R.layout.chat_received
    }

}

class SentChatItem(val message: String, val timestamp: String, val date: String): Item<GroupieViewHolder>()   {
    override fun bind(view: GroupieViewHolder, p1: Int) {
        view.itemView.sentMessage.text = message
        view.itemView.timestampSent.text = timestamp
        view.itemView.dateSent.text = date
    }

    override fun getLayout(): Int {
        return R.layout.chat_sent
    }

}