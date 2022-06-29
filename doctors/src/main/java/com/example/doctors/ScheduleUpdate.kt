// documentation complete: Sep 8 2021

package com.example.doctors

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.*
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlinx.android.synthetic.main.activity_update_schedule.*
import kotlinx.android.synthetic.main.activity_update_schedule.back
import kotlinx.android.synthetic.main.activity_update_schedule.confirmBtn
import java.time.*

class ScheduleUpdate : AppCompatActivity() {
    // layout properties
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // database
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer_update_schedule)

        // intents
        val intentDashboard = Intent(this, DoctorDashboard::class.java)
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

        // init navigation view
        supportActionBar?.title = "Profile"
        supportActionBar?.subtitle = "Update Schedule"

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

        // preview current schedule as chips
        extractSched(id.toString())

        // initialize spinner
        val morning = resources.getStringArray(R.array.Morning)
        val afternoon = resources.getStringArray(R.array.Afternoon)
        val AMStartSpinner = findViewById<Spinner>(R.id.spinnerAMStart)
        val AMEndSpinner = findViewById<Spinner>(R.id.spinnerAMEnd)
        val PMStartSpinner = findViewById<Spinner>(R.id.spinnerPMStart)
        val PMEndSpinner = findViewById<Spinner>(R.id.spinnerPMEnd)

        // initialize spinner adapters
        if(AMStartSpinner != null) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, morning)
            AMStartSpinner.adapter = adapter
        }
        if(AMEndSpinner != null) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, morning)
            AMEndSpinner.adapter = adapter
        }
        if(PMStartSpinner != null) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, afternoon)
            PMStartSpinner.adapter = adapter
        }
        if(PMEndSpinner != null) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, afternoon)
            PMEndSpinner.adapter = adapter
        }

        // add selected values to text display
        AMStartSpinner.onItemSelectedListener = object :
        AdapterView.OnItemSelectedListener  {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                AMStartText.text = AMStartSpinner.selectedItem.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        AMEndSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener  {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                AMEndText.text = AMEndSpinner.selectedItem.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        PMStartSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener  {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                PMStartText.text = PMStartSpinner.selectedItem.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        PMEndSpinner.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener  {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                PMEndText.text = PMEndSpinner.selectedItem.toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        // initialize spinner
        val times = resources.getStringArray(R.array.Minutes)
        val timeSpinner = findViewById<Spinner>(R.id.timeSpinner)
        if(timeSpinner != null) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, times)
            timeSpinner.adapter = adapter
        }

        // set label for day chip selected
        chipSched.setOnCheckedChangeListener { _, checkedId ->
            preview.visibility = VISIBLE

            if(checkedId==-1)   {
                preview.text = "No day selected."
            }   else    {
                val day = findViewById<Chip>(checkedId).text.toString()
                preview.text = "Schedule for $day"
            }
        }

        // confirm button to save daily schedule as chip
        confirmBtn.setOnClickListener   {
            // time strings
            val timeAMStart = AMStartSpinner.selectedItem.toString()
            val timeAMEnd = AMEndSpinner.selectedItem.toString()
            val timePMStart = PMStartSpinner.selectedItem.toString()
            val timePMEnd = PMEndSpinner.selectedItem.toString()

            val AMStartCheck = timeAMStart.filter { it.isDigit() }
            val AMEndCheck = timeAMEnd.filter { it.isDigit() }
            val PMStartCheck = timePMStart.filter { it.isDigit() }
            val PMEndCheck = timePMEnd.filter { it.isDigit() }

            val timeString = "$timeAMStart-$timeAMEnd, $timePMStart-$timePMEnd"

            // for error trapping
            var check = 4

            // error trapping to check fields
            if(chipSched.checkedChipId!=-1) {

                if((AMStartCheck.isBlank() && AMEndCheck.isBlank() && PMEndCheck.isBlank() && PMStartCheck.isBlank()) ||
                    (AMStartCheck.isBlank() && AMEndCheck.isNotBlank()) || (AMStartCheck.isNotBlank() && AMEndCheck.isBlank()) ||
                    (PMStartCheck.isBlank() && PMEndCheck.isNotBlank()) || (PMStartCheck.isNotBlank() && PMEndCheck.isBlank())) {
                    check--
                    Toast.makeText(this, "Incomplete Details.", Toast.LENGTH_SHORT).show()
                }

                if((AMStartCheck.isBlank() && AMEndCheck.isBlank() && PMStartCheck.isNotBlank() && PMEndCheck.isNotBlank()) ||
                    (AMStartCheck.isNotBlank() && AMEndCheck.isNotBlank() && PMStartCheck.isBlank() && PMEndCheck.isBlank()))    {

                    if(AMStartCheck.isBlank() && AMEndCheck.isBlank())  {
                        if(PMEndCheck.toInt() <= PMStartCheck.toInt()) {
                            check--
                            Toast.makeText(this, "Invalid Time Fields.", Toast.LENGTH_SHORT).show()
                        }
                    }   else if(PMStartCheck.isBlank() && PMEndCheck.isBlank()) {
                        if(AMEndCheck.toInt() <= AMStartCheck.toInt())  {
                            check--
                            Toast.makeText(this, "Invalid Time Fields.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }   else    {
                    if((AMEndCheck.toInt() <= AMStartCheck.toInt()) || (PMEndCheck.toInt() <= PMStartCheck.toInt()) ||
                        (AMEndCheck.toInt() >= PMStartCheck.toInt()) || (AMEndCheck.toInt() == PMStartCheck.toInt()))   {
                        check--
                        Toast.makeText(this, "Invalid Time Fields.", Toast.LENGTH_SHORT).show()
                    }
                }

                if(check==4)    {
                    // for loop to check each item of schedOverview
                    for(i in 0..CTOverview.childCount)   {
                        if(CTOverview.getChildAt(i) != null) {
                            val schedID = CTOverview.getChildAt(i).id
                            val schedString = findViewById<Chip>(schedID).text.toString()         // extract text of schedOverview child
                            val schedList: List<String> = schedString.split(": ")       // split text of schedOverview child

                            if(schedList[0] == findViewById<Chip>(chipSched.checkedChipId).text.toString()) {
                                Toast.makeText(this, "You already added a schedule for that day.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // add set schedule as chips
                    when (findViewById<Chip>(chipSched.checkedChipId).text.toString()) {
                        "Monday" -> {
                            CTOverview.addChip(this, "Monday: $timeString")
                        }
                        "Tuesday" -> {
                            CTOverview.addChip(this, "Tuesday: $timeString")
                        }
                        "Wednesday" -> {
                            CTOverview.addChip(this, "Wednesday: $timeString")
                        }
                        "Thursday" -> {
                            CTOverview.addChip(this, "Thursday: $timeString")
                        }
                        "Friday" -> {
                            CTOverview.addChip(this, "Friday: $timeString")
                        }
                        "Saturday" -> {
                            CTOverview.addChip(this, "Saturday: $timeString")
                        }
                        "Sunday" -> {
                            CTOverview.addChip(this, "Sunday: $timeString")
                        }
                    }
                }
                // if no day chip selected
            }   else if (chipSched.checkedChipId==-1)    {
                Toast.makeText(this, "No day selected.", Toast.LENGTH_SHORT).show()
            }
        }

        // final button to save schedule
        saveSched.setOnClickListener {
            // check average consultation time
            val avgTime = (timeSpinner.selectedItem.toString()).filter { it.isDigit() }

            // schedOverview = chip group of inputted schedule
            // if schedOverview is not empty, save schedule to database
            if(CTOverview.childCount > 0 && !(avgTime.isNullOrBlank())) {

                val updateConsTime = mapOf(
                    "avgConsTime" to avgTime.toInt()
                )

                database = FirebaseDatabase.getInstance().getReference("DOCTOR")
                database.child(id.toString()).updateChildren(updateConsTime)

                // schedArray = list of the days inputted - to be used in deleting extra schedule
                val schedArray = mutableListOf<String>()

                // for loop to check each item of schedOverview
                for(i in 0..CTOverview.childCount)   {
                    if(CTOverview.getChildAt(i) != null) {
                        val schedID = CTOverview.getChildAt(i).id
                        val schedString = findViewById<Chip>(schedID).text.toString()         // extract text of schedOverview child
                        val schedList: List<String> = schedString.split(": ")       // split text of schedOverview child

                        database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
                        database.child("timeSlot").get().addOnSuccessListener { dayDB ->
                            database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
                            // remove node in database if it exists
                            database.child("timeSlot").child(schedList[0]).removeValue()

                            // call function to make time slots in database
                            divideTimeSlot(id.toString(), schedList[0], schedList[1], avgTime.toInt())
                        }

                        // call function to update current schedule
                        updateSched(id.toString(), schedList[0], schedList[1])        // get(0) = day, get(1) = time string
                        schedArray.add(schedList[0])                                      // add day to schedArray list
                    }
                }

                // change updateSched state in database to true
                database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
                val schedUpdateState = mapOf(
                    "updateSched" to true
                )
                database.updateChildren(schedUpdateState)

                // call function to delete schedule
                deleteSched(id.toString(), schedArray)
                Toast.makeText(this, "Update Success.", Toast.LENGTH_SHORT).show()
                passData(intentProfile, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), schedule.toString(), consTime)
            }   else if(avgTime.isNullOrBlank())    {
                Toast.makeText(this, "Please select your average consultation time.", Toast.LENGTH_SHORT).show()
            }   else    {
                Toast.makeText(this, "Please enter your schedule.", Toast.LENGTH_SHORT).show()
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

    // function to update schedule
    private fun updateSched(id: String, day: String, time: String)   {
        // set database to point to the schedule node under DOCTOR
        database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id).child("schedule")
        val schedDay = mapOf(
            day to time
        )

        database.updateChildren(schedDay)
    }

    // function to make time slot for schedules
    private fun divideTimeSlot(id: String, day: String, time: String, consTime: Int)   {
        // split time to hours
        val split = time.split(":", "-", ", ")

        // AM time slots
        if(split[0].isNotBlank() && split[1].isNotBlank() && split[2].isNotBlank() && split[3].isNotBlank())    {
            var slotStart: LocalTime = LocalTime.of(split[0].toInt(), split[1].toInt())
            val slotEnd: LocalTime = LocalTime.of(split[2].toInt(), split[3].toInt())

            while((slotStart <= slotEnd) && (slotStart.plusMinutes(consTime.toLong()) <= slotEnd))	{
                // call function to update time slot
                updateTimeSlot(id, slotStart.toString(), day)
                slotStart = slotStart.plusMinutes(consTime.toLong())
            }
        }

        // PM time slots
        if(split[4].isNotBlank() && split[5].isNotBlank() && split[6].isNotBlank() && split[7].isNotBlank())    {
            var slotStart: LocalTime = LocalTime.of(split[4].toInt(), split[5].toInt())
            val slotEnd: LocalTime = LocalTime.of(split[6].toInt(), split[7].toInt())

            while((slotStart <= slotEnd) && (slotStart.plusMinutes(consTime.toLong()) <= slotEnd))	{
                // call function to update time slot
                updateTimeSlot(id, slotStart.toString(), day)
                slotStart = slotStart.plusMinutes(consTime.toLong())
            }
        }
    }

    // function to update time slots
    private fun updateTimeSlot(id: String, slot: String, day: String)   {
        database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id).child("timeSlot").child(day)

        val timeSlot = mapOf(
            slot to ""
        )

        database.updateChildren(timeSlot)
    }

    // function to delete schedule
    private fun deleteSched(id: String, array: List<String>)   {
        // set database to point to the schedule node under DOCTOR
        database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id).child("schedule")
        val sched = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val toDelete = sched.minus(array)       // get difference between complete days list and list of days to be updated
                                                // = list of days to be deleted

        for(i in toDelete) {
            database.child(i).removeValue()
        }
    }

    // function to extract current schedule from database
    private fun extractSched(id: String)  {
        // set database to point to the schedule node under DOCTOR
        database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id).child("schedule")
        val sched = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val context = this

        for(i in sched) {
            database.child(i).get().addOnSuccessListener { snapshot ->
                if(snapshot.exists())   {
                    // if day is found, add day and value to chip group
                    CTOverview.addChip(context, "$i: ${snapshot.getValue<String>().toString()}")
                }
            }
        }
    }

    // function to add chip to chip group
    fun ChipGroup.addChip(context: Context, title: String)  {
        Chip(context).apply {
            id = generateViewId()
            text = title
            isCloseIconVisible = true
            performCloseIconClick()
            addView(this)
            setOnCloseIconClickListener {
                removeView(this)
            }
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
}