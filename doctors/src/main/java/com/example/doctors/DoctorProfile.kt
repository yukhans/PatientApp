package com.example.doctors

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.*
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import kotlinx.android.synthetic.main.activity_doctor_profile.*
import kotlinx.android.synthetic.main.activity_doctor_profile.preview
import kotlinx.android.synthetic.main.activity_doctor_profile.updateKey
import kotlinx.android.synthetic.main.activity_doctor_profile.updateSchedule

class DoctorProfile : AppCompatActivity() {
    // layout properties
    lateinit var toggle: ActionBarDrawerToggle
    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    // database
    private lateinit var database: DatabaseReference

    var counter: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.drawer_profile)

        // intents
        val intentDashboard = Intent(this, DoctorDashboard::class.java)
        val intentQueue = Intent(this, CurrentQueue::class.java)
        val intentHistory = Intent(this, History::class.java)
        val intentProfile = Intent(this, DoctorProfile::class.java)
        val intentLogin = Intent(this, DoctorLogin::class.java)
        val intentKeyUpdate = Intent(this, KeyUpdate::class.java)
        val updateIntent = Intent(this, ProfileUpdate::class.java)
        val intentSchedUpdate = Intent(this, ScheduleUpdate::class.java)

        // init doctor details
        val bundle: Bundle? = intent.extras
        val id = bundle!!.getString("id")
        val firstName = bundle.getString("firstName")
        val lastName = bundle.getString("lastName")
        val spec = bundle.getString("spec")
        val sex = bundle.getString("sex")
        val consTime = bundle.getInt("consTime")

        supportActionBar?.title = "Home"
        supportActionBar?.subtitle = "Profile"

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.profile -> {
                    passData(intentProfile, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime)
                }
                R.id.dashboard -> {
                    passData(intentDashboard, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime)
                }
                R.id.queue -> {
                    passData(intentQueue, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime)
                }
                R.id.history -> {
                    passData(intentHistory, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime)
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

        updateDetails.setOnClickListener {
            Toast.makeText(applicationContext, "Update Profile", Toast.LENGTH_SHORT).show()
            passData(updateIntent, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime)
        }

        // read in database if updateKey and updateSched exists
        database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
        database.get().addOnSuccessListener {
            if(it.child("avgConsTime").exists()) {
                profile_consTime.text = "${it.child("avgConsTime").value.toString()} minutes"
            }   else    {
                profile_consTime.text = "-----"
            }

            if(it.child("gracePeriod").exists())    {
                profile_gracePrd.text = "${it.child("gracePeriod").value.toString()}"
            }   else    {
                profile_gracePrd.text = "5 minutes (set by default)"
            }

            if(it.child("clinicConfig").exists())    {
                profile_cconfig.text = "${it.child("clinicConfig").child("cconfigStr").value.toString()}"
            }   else    {
                profile_cconfig.text = "-----"
            }
        }

        updateGracePrd.setOnClickListener {
            chipsGP.removeAllViews()
            if(updateGPCard.visibility == VISIBLE)   {
                updateGPCard.visibility = GONE
            }   else if(updateGPCard.visibility == GONE) {
                updateGPCard.visibility = VISIBLE
            }

            // initialize chip group
            database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
            database.get().addOnSuccessListener { doc ->
                val gracePeriodSelection =
                    if(doc.child("avgConsTime").value.toString().toInt() == 0)
                        5
                    else
                        ((doc.child("avgConsTime").value.toString()).filter { it.isDigit() }).toInt()

                for(i in 1..gracePeriodSelection)    {
                    chipsGP.addChip(this, "$i mins")
                }

                updateGP.setOnClickListener {
                    val gracePeriod = findViewById<Chip>(chipsGP.checkedChipId).text.toString()

                    database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
                    database.child("gracePeriod").setValue(gracePeriod).addOnSuccessListener {
                        profile_gracePrd.text = gracePeriod
                        updateGPCard.visibility = GONE
                    }
                }
            }
        }

        updateConsType.setOnClickListener {
            if(updateConsTypeCard.visibility == VISIBLE) {
                updateConsTypeCard.visibility = GONE
            }   else if(updateConsTypeCard.visibility == GONE)   {
                updateConsTypeCard.visibility = VISIBLE
            }

            okCT.setOnClickListener {
                updateCT2.visibility = VISIBLE
                val constypeAmtStr = constypeAmt.text.toString().toInt()

                // initialize chip group
                for(i in 1..constypeAmtStr)    {
                    chipsCT.addChip(this, "Type #$i")
                }

                saveCT.setOnClickListener {
                    val constypeStr = constype.text.toString()
                    val constypeTimeInt = constypeTime.text.toString().toInt()

                    if(CTOverview.childCount < constypeAmtStr)  {
                        CTOverview.addChip2(this, "$constypeStr: $constypeTimeInt")
                    }   else    {
                        Toast.makeText(this, "Reached Maximum Consultation Types of $constypeAmtStr", Toast.LENGTH_LONG).show()
                    }

                    constype.text.clear()
                    constypeTime.text.clear()
                }
            }

            updateCT.setOnClickListener {
                for(i in 0..CTOverview.childCount) {
                    if(chipsCT.getChildAt(i) != null)   {
                        val chipID = CTOverview.getChildAt(i).id
                        val chipStr = findViewById<Chip>(chipID).text.toString()
                        val chipList: List<String> = chipStr.split(": ")

                        database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
                        database.child("consultationType").child(((i.toString().toInt())+1).toString()).child("consStr").setValue(chipList[0]).addOnSuccessListener {
                            constypeAmt.text.clear()
                        }
                        database.child("consultationType").child(((i.toString().toInt())+1).toString()).child("consTime").setValue(chipList[1]).addOnSuccessListener {
                            updateConsTypeCard.visibility = GONE
                        }
                    }
                }

                database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
                database.child("consultationType").get().addOnSuccessListener {
                    for(i in it.children)   {
                        val str = i.child("consStr").value.toString()
                        val amt = i.child("consTime").value.toString()
                        profileCT.addText(this, "$str: $amt mins")
                    }
                }
            }
        }

        updateCconfig.setOnClickListener {
            if(updateCconfigCard.visibility == VISIBLE) {
                updateCconfigCard.visibility = GONE
            }   else if(updateCconfigCard.visibility == GONE)   {
                updateCconfigCard.visibility = VISIBLE
            }

            // initialize spinner
            val times = resources.getStringArray(R.array.delay_time)
            val timeSpinner = findViewById<Spinner>(R.id.CConfigSpinner_time)
            if(timeSpinner != null) {
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, times)
                timeSpinner.adapter = adapter
            }

            // initialize chip select
            chipCConfig.setOnCheckedChangeListener { _, checkedId ->
                preview.visibility = VISIBLE

                if(checkedId==-1)   {
                    preview.text = "No clinic configuration selected."
                }   else    {
                    val cconfig = findViewById<Chip>(checkedId).text.toString()
                    preview.text = "$cconfig Clinic Configuration"

                    if(cconfig == "Building")   {
                        forBuilding.visibility = VISIBLE
                        cconfig_walkingtime.visibility = VISIBLE

                        updateCC.setOnClickListener {
                            val maxCap = CConfig_cap.text.toString()
                            val walkingTime = (timeSpinner.selectedItem.toString()).filter { it.isDigit() }
                            val roomNumber = CConfig_room.text.toString()
                            val floorNumber = CConfig_floor.text.toString()
                            val cconfigStr = "Clinic is in a $cconfig, Waiting Room Max Cap: $maxCap, Floor #$floorNumber, Room #$roomNumber, Walking Time from Entrance: $walkingTime mins"

                            val updateClinicConfig = mapOf(
                                "cconfigStr" to cconfigStr,
                                "maxCap" to maxCap,
                                "walkingTime" to walkingTime,
                                "roomNumber" to roomNumber,
                                "floorNumber" to floorNumber,
                                "currWaitingCount" to 0
                            )

                            database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
                            database.child("clinicConfig").updateChildren(updateClinicConfig).addOnSuccessListener {
                                profile_cconfig.text = cconfigStr
                                updateCconfigCard.visibility = GONE
                            }
                        }
                    }   else if(cconfig == "Standalone")    {
                        forBuilding.visibility = GONE
                        cconfig_walkingtime.visibility = GONE

                        updateCC.setOnClickListener {
                            val maxCap = CConfig_cap.text.toString()
                            val cconfigStr = "Clinic is a $cconfig, Waiting Room Max Cap: $maxCap"

                            val updateClinicConfig = mapOf(
                                "cconfigStr" to cconfigStr,
                                "maxCap" to maxCap,
                                "walkingTime" to 0,
                                "currWaitingCount" to 0
                            )

                            database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id.toString())
                            database.child("clinicConfig").updateChildren(updateClinicConfig).addOnSuccessListener {
                                profile_cconfig.text = cconfigStr
                                updateCconfigCard.visibility = GONE
                            }
                        }
                    }
                }
            }
        }

        database.get().addOnSuccessListener {
            getStatus(it, intentSchedUpdate,  intentKeyUpdate, id.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString())
        }

        // call module to extract specializations and schedule from database
        if(spec.toString().isEmpty())     profile_spec.text = "-------"
        else                              extractSpecs(spec.toString())
        extractSched(id.toString())

        // update profile details
        val headerView: View = navigationView.getHeaderView(0)
        val navName: TextView = headerView.findViewById(R.id.nav_user_name)
        val navId: TextView = headerView.findViewById(R.id.nav_user_id)

        if(sex == "Male")    {
            image.setImageResource(R.drawable.male)
        }   else if(sex == "Female") {
            image.setImageResource(R.drawable.female)
        }

        navName.text = "$firstName $lastName"
        navId.text = "ID: $id"
        profile_fullname.text = "$firstName $lastName"
        profile_id.text = id
        profile_sex.text = sex
        profile_consTime.text = "$consTime minutes"
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

    private fun getStatus(snapshot: DataSnapshot, intentSchedUpdate: Intent, intentKeyUpdate: Intent, id: String, firstName: String, lastName: String, spec: String, sex: String) {
        updateKey.setOnClickListener {
            Toast.makeText(applicationContext, "Update Key", Toast.LENGTH_SHORT).show()
            database = FirebaseDatabase.getInstance().getReference("DOCTOR")
            database.child(id).child("avgConsTime").get().addOnSuccessListener {
                onUpdateButton(intentKeyUpdate, updateKey, id, firstName, lastName, spec, sex, it.value.toString().toInt())
            }
        }

        // check updateSched state
        if(snapshot.child("updateSched").exists())   {
            // if sched already updated before, set button visibility to gone
            if(snapshot.child("updateSched").getValue<Boolean>() == true)    {
                updateSchedule.visibility = GONE
            }
            // if sched not updated, pass intent
            if(snapshot.child("updateSched").getValue<Boolean>() == false)   {
                updateSchedule.setOnClickListener {
                    Toast.makeText(applicationContext, "Update Schedule", Toast.LENGTH_SHORT).show()
                    database = FirebaseDatabase.getInstance().getReference("DOCTOR")
                    database.child(id).child("avgConsTime").get().addOnSuccessListener {
                        onUpdateButton(intentSchedUpdate, updateKey, id, firstName, lastName, spec, sex, it.value.toString().toInt())
                    }
                }
            }
        }
    }

    private fun extractSpecs(spec: String)  {
        val specDigit = spec.filter { it.isDigit() }
        val first = specDigit.toInt().mod(10)
        val second = ((specDigit.toInt().mod(100)).minus(first)).div(10)
        val third = ((specDigit.toInt().mod(1000)).minus(specDigit.toInt().mod(100))).div(100)
        val specs = arrayOf(first, second, third)
        val specString = arrayOf("", "", "")

        for(i in specs) {
            database = FirebaseDatabase.getInstance().getReference("Specialization")
            database.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    specString[counter] = snapshot.child(i.toString()).getValue<String>().toString()
                    if (second == 0 && third == 0) {
                        profile_spec.text = specString[0]
                    } else if (third == 0) {
                        profile_spec.text = "${specString[0]}, ${specString[1]}"
                    } else {
                        profile_spec.text = "${specString[0]}, ${specString[1]}, ${specString[2]}"
                    }
                    counter++
                } else {
                    profile_spec.text = "Specialization not specified!"
                }
            }
        }
        counter = 0
    }

    private fun extractSched(id: String)  {
        val sched = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val context = this
        database = FirebaseDatabase.getInstance().getReference("DOCTOR").child(id).child("schedule")

        for(i in sched) {
            database.child(i).get().addOnSuccessListener { snapshot ->
                if(snapshot.exists())   {
                    schedView.addText(context, "$i: ${snapshot.getValue<String>().toString()}")
                }
            }
        }
    }

    // function to add textview to linear layout
    fun LinearLayout.addText(context: Context, title: String)  {
        TextView(context).apply {
            id = generateViewId()
            text = title
            addView(this)
        }
    }

    // function to add chip to chip group
    fun ChipGroup.addChip(context: Context, title: String)  {
        Chip(context).apply {
            id = generateViewId()
            text = title
            isCheckable = true
            isClickable = true
            isCheckedIconVisible = true
            isChipIconVisible = true
            addView(this)
        }
    }

    // function to add chip to chip group 2
    fun ChipGroup.addChip2(context: Context, title: String)  {
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

    private fun passData(intent: Intent, id: String, firstName: String, lastName: String, spec: String, sex: String, consTime: Int)    {
        intent.putExtra("id", id)
        intent.putExtra("firstName", firstName)
        intent.putExtra("lastName", lastName)
        intent.putExtra("spec", spec)
        intent.putExtra("sex", sex)
        intent.putExtra("consTime", consTime)
        startActivity(intent)
    }

    private fun onUpdateButton(intent: Intent, button: Button, id: String, firstName: String, lastName: String, spec: String, sex: String, consTime: Int)    {
        val options: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this,button,"transTitle")
        intent.putExtra("id", id)
        intent.putExtra("firstName", firstName)
        intent.putExtra("lastName", lastName)
        intent.putExtra("spec", spec)
        intent.putExtra("sex", sex)
        intent.putExtra("consTime", consTime)
        startActivity(intent,options.toBundle())
    }
}