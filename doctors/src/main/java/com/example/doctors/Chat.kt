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
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.recycler_chat.*
import kotlinx.android.synthetic.main.recycler_chat.view.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Chat : AppCompatActivity() {
    // layout properties
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // database
    private lateinit var docDatabase: DatabaseReference
    private lateinit var patientDatabase: DatabaseReference
    private lateinit var database: DatabaseReference

    val latestMessagesMap = HashMap<String, String>()
    val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer_chat)

        val bundle: Bundle? = intent.extras
        val id = bundle!!.getString("id")

        supportActionBar?.title = "Home"
        supportActionBar?.subtitle = "Chat Messages"

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this,drawerLayout,R.string.open,R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
                when(item.itemId){
                    R.id.profile -> {
                        passData(intentProfile, id.toString(), firstName, lastName, spec, sex, consTime)
                    }
                    R.id.dashboard -> {
                        passData(intentDashboard, id.toString(), firstName, lastName, spec, sex, consTime)
                    }
                    R.id.queue -> {
                        passData(intentQueue, id.toString(), firstName, lastName, spec, sex, consTime)
                    }
                    R.id.history -> {
                        passData(intentHistory, id.toString(), firstName, lastName, spec, sex, consTime)
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

            if(doc.child("queue").exists()) {
                // initialize recycler view
                chatRecycler.setHasFixedSize(true)

                description.visibility = VISIBLE
                noChatText.visibility = GONE
                chatRecycler.visibility = VISIBLE

                for(i in doc.child("queue").children)   {
                    val date = i.key.toString()
                    for(j in doc.child("queue").child(date).children)   {
                        val slot = j.key.toString()
                        val patient = j.value.toString()

                        patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
                        patientDatabase.child(patient).get().addOnSuccessListener { pat ->
                            val name = pat.child("name").value.toString()

                            for(b in pat.child("bookings").children)    {
                                if(b.child("doctor").value.toString() == id && b.child("date").value.toString() == date && b.child("timeslot").value.toString() == slot)    {
                                    val specDB = b.child("spec").value.toString()
                                    val reason = b.child("reason").value.toString()

                                    database = FirebaseDatabase.getInstance().getReference("latestMessages")
                                    database.child(id).child("$patient-$date-$slot").get().addOnSuccessListener { message ->
                                        val chat = message.child("message").value.toString()

                                        latestMessagesMap[patient] = "$date-$slot-$specDB-$reason-$id-$chat-$name"
                                        adapter.add(UserItem(name, patient,"$date-$slot-$specDB-$reason-$id-$chat"))
                                        chatRecycler.adapter = adapter
                                    }
                                }
                            }
                        }
                    }
                }

                adapter.setOnItemClickListener  {item, view ->
                    val userItem = item as UserItem

                    val intent = Intent(view.context, ChatMessages::class.java)
                    intent.putExtra("name", userItem.name)
                    intent.putExtra("dateTime", userItem.message)
                    intent.putExtra("patientID", userItem.patientID)
                    startActivity(intent)
                }

            }   else    {
                description.visibility = GONE
                noChatText.visibility = VISIBLE
                chatRecycler.visibility = GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem):Boolean{
        if(toggle.onOptionsItemSelected(item)){
            true
        }
        return super.onOptionsItemSelected(item)
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

class UserItem(val name: String, val patientID: String, val message: String): Item<GroupieViewHolder>()  {
    override fun getLayout(): Int {
        return R.layout.recycler_chat
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        // initialize date format
        // divide date to individual digits
        val split = message.split("-")
        val date = split[0]
        val slot = split[1]

        val sixth = date.toInt().mod(10)
        val fifth = ((date.toInt().mod(100)).minus(sixth)).div(10)
        val fourth = ((date.toInt().mod(1000)).minus(date.toInt().mod(100))).div(100)
        val third = ((date.toInt().mod(10000)).minus(date.toInt().mod(1000))).div(1000)
        val second = ((date.toInt().mod(100000)).minus(date.toInt().mod(10000))).div(10000)
        val first = ((date.toInt().mod(1000000)).minus(date.toInt().mod(100000))).div(100000)

        val year = "$fifth$sixth"
        val month = "$third$fourth"
        val day = "$first$second"

        val dateFormat: LocalDate = LocalDate.of(year.toInt(), day.toInt(), month.toInt())
        val dateFormatted: String = dateFormat.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd"))

        viewHolder.itemView.chatName.text = name
        viewHolder.itemView.chatMessage.text = "$dateFormatted at $slot"
        viewHolder.itemView.chatText.text =
            if(split[5] == "null")      ""
            else                        split[5]
    }
}