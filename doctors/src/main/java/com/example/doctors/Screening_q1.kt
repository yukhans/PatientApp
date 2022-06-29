package com.example.doctors

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.hd_form1.*
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class Screening_q1 : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var docDatabase: DatabaseReference
    private lateinit var patientDatabase: DatabaseReference
    private lateinit var tempDatabase: DatabaseReference

    lateinit var dateToday: Calendar
    var nowmonth = 0
    var nowday = 0
    var nowyear = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hd_form1)

        supportActionBar?.title = "Doctor Screening Form"
        supportActionBar?.subtitle = "Question 1"

        //get date today
        dateToday = Calendar.getInstance()
        nowmonth = dateToday.get(Calendar.MONTH) + 1
        nowday = dateToday.get(Calendar.DAY_OF_MONTH) - 1
        nowyear = dateToday.get(Calendar.YEAR) - 2000

        var q1_yes_state = false
        var q1_no_state = false
        var respiratory_state = false
        var worse_state = false
        var stable_state = false
        var influenza_state = false
        var fever_state = false

        val intent = intent
        val uid = intent.getStringExtra("id")
        val firstName = intent.getStringExtra("firstName")
        val lastName = intent.getStringExtra("lastName")
        val spec = intent.getStringExtra("spec")
        val sex = intent.getStringExtra("sex")
        val consTime = intent.getStringExtra("consTime")

        q1_yes.setOnClickListener {
            q1_yes_state = q1_yes.isChecked
            if(q1_yes_state){
                q1_no_state = false
                q1_no.isChecked = false

                callDialog(uid.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime.toString())
            }
        }

        q1_no.setOnClickListener {
            q1_no_state = q1_no.isChecked
            if(q1_no_state){
                q1_yes_state = false
                q1_yes.isChecked = false
                nextBtn.visibility = VISIBLE
                Toast.makeText(applicationContext,"Click the next button!", Toast.LENGTH_SHORT).show()
            }
            else{
                nextBtn.visibility = GONE

            }
        }

        resSymptoms.setOnClickListener {
            respiratory_state = resSymptoms.isChecked
            if(respiratory_state){
                if(!q1_yes_state || (!q1_no_state || q1_no_state)){
                    q1_yes.isChecked = true
                    q1_yes_state = true
                    q1_no_state = false
                    q1_no.isChecked = false
                    callDialog(uid.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime.toString())
                }
            }
        }

        influenza.setOnClickListener {
            influenza_state = influenza.isChecked
            if(influenza_state){
                if(!q1_yes_state || (!q1_no_state || q1_no_state)){
                    q1_yes.isChecked = true
                    q1_yes_state = true
                    q1_no_state = false
                    q1_no.isChecked = false
                    callDialog(uid.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime.toString())
                }
            }
        }

        fever.setOnClickListener {
            fever_state = fever.isChecked
            if(fever_state){
                if(!q1_yes_state || (!q1_no_state || q1_no_state)){
                    q1_yes.isChecked = true
                    q1_yes_state = true
                    q1_no_state = false
                    q1_no.isChecked = false
                    callDialog(uid.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime.toString())
                }
            }
        }

        worsening.setOnClickListener {
            worse_state = worsening.isChecked
            if(worse_state){
                if(respiratory_state){
                    stable_state = false
                    stable.isChecked = false
                    if(!q1_yes_state || (!q1_no_state || q1_no_state)){
                        q1_yes.isChecked = true
                        q1_yes_state = true
                        q1_no_state = false
                        q1_no.isChecked = false
                    }
                }else{
                    worse_state = false
                    worsening.isChecked = false
                    Toast.makeText(applicationContext,"You did not choose a respiratory symptom yet!", Toast.LENGTH_SHORT).show()
                }

            }
        }

        stable.setOnClickListener {
            stable_state = stable.isChecked
            if(stable_state){
                if(respiratory_state){
                    worse_state = false
                    worsening.isChecked = false
                    if(!q1_yes_state || (!q1_no_state || q1_no_state)){
                        q1_yes.isChecked = true
                        q1_yes_state = true
                        q1_no_state = false
                        q1_no.isChecked = false
                    }
                }else{
                    stable_state = false
                    stable.isChecked = false
                    Toast.makeText(applicationContext,"You did not choose a respiratory symptom yet!", Toast.LENGTH_SHORT).show()
                }

            }
        }

        nextBtn.setOnClickListener{
            val intent = Intent(this, Screening_q2::class.java)
            intent.putExtra("id", uid)
            intent.putExtra("firstName", firstName)
            intent.putExtra("lastName", lastName)
            intent.putExtra("spec", spec)
            intent.putExtra("sex", sex)
            intent.putExtra("consTime", consTime)
            startActivity(intent)
        }
    }

    private fun callDialog(uid: String, firstName: String, lastName: String, spec: String, sex: String, consTime: String) {
        val builder: androidx.appcompat.app.AlertDialog.Builder? = androidx.appcompat.app.AlertDialog.Builder(this)
        builder?.setTitle("Health Evaluation Screening Failed.")
        builder?.setCancelable(false)
        builder?.setMessage("You cannot proceed to the succeeding questions. Your bookings for this day will be cancelled immediately.")
        builder?.setPositiveButton("Ok") { _, _ ->
            deleteBooking(uid)

            val intent = Intent(this, DoctorDashboard::class.java)
            intent.putExtra("id", uid)
            intent.putExtra("firstName", firstName)
            intent.putExtra("lastName", lastName)
            intent.putExtra("spec", spec)
            intent.putExtra("sex", sex)
            intent.putExtra("consTime", consTime)
            startActivity(intent)
        }

        val dialog : androidx.appcompat.app.AlertDialog? = builder?.create()
        dialog?.show()


        //Get alert dialog buttons
        val positiveButton : Button = dialog!!.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
        positiveButton.setTextColor(Color.parseColor("#F36767"))
    }

    private fun deleteBooking(uid: String) {
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR").child(uid)
        patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
        database = FirebaseDatabase.getInstance().getReference("messages")
        tempDatabase = FirebaseDatabase.getInstance().getReference("latestMessages")

        val datesList = mutableListOf<String>()
        val bookings = mutableListOf<String>()
        val slots = mutableMapOf<String, String>()

        val sdf = SimpleDateFormat("MMddyy")

        val c = Calendar.getInstance()
        c.time = Date()
        val today = sdf.format(c.time)
        datesList.add(today)

        for(i in 1..4)  {
            c.add(Calendar.DATE, 1)
            datesList.add(sdf.format(c.time))
        }

        // set hasAnsweredScreening date and result
        docDatabase.child("hasAnsweredScreening").child("date").setValue(today)
        docDatabase.child("hasAnsweredScreening").child("result").setValue("fail")

        // remove from doctor queue
        docDatabase.get().addOnSuccessListener { doctor ->
            val firstName = doctor.child("firstName").value.toString()
            val lastName = doctor.child("lastName").value.toString()
            val name = "$firstName $lastName"

            for(date in datesList)  {
                if(doctor.child("queue").child(date).exists())  {
                    docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                    docDatabase.child(uid).child("queue").child(date).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Bookings for today and for the next 4 days cancelled!", Toast.LENGTH_SHORT).show()
                        }

                    for(i in doctor.child("queue").child(date).children) {
                        val slot = i.key.toString()
                        val patID = i.value.toString()
                        bookings.add(patID)
                        slots[slot] = patID
                    }

                    slots.forEach   { (slot, patientID) ->
                        val check = "$date-$slot"

                        // remove from patient bookings
                        patientDatabase.child(patientID).get().addOnSuccessListener { patient ->
                            if(patient.child("bookings").exists())  {
                                for(i in patient.child("bookings").children)    {
                                    if(i.child("doctor").value.toString() == uid && i.child("date").value.toString() == date)    {
                                        patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
                                        patientDatabase.child(patientID).child("bookings")
                                            .child(i.key.toString()).removeValue()
                                            .addOnSuccessListener {
                                                patientDatabase = FirebaseDatabase.getInstance().getReference("Users")
                                                patientDatabase.child(patientID).child("states").child("doctorCancelled").child("booking").setValue(i.key.toString())
                                                patientDatabase.child(patientID).child("states").child("doctorCancelled").child("reason").setValue("Doctor failed health screening for $today.")
                                                patientDatabase.child(patientID).child("states").child("doctorCancelled").child("uid").setValue(uid)
                                                patientDatabase.child(patientID).child("states").child("doctorCancelled").child("name").setValue(name)
                                            }
                                    }
                                }
                            }
                        }

                        // delete message node
                        database.get().addOnSuccessListener { messages ->
                            for(i in messages.children) {
                                val split = i.key.toString().split("-")
                                if(split[0] == patientID && split[1] == uid)  {
                                    for(j in i.children)    {
                                        val split2 = j.key.toString().split("-")
                                        if(split2[0] == date)   {
                                            database.child(i.key.toString()).child(j.key.toString()).removeValue()
                                        }
                                    }
                                }
                            }
                        }

                        // delete in latest messages node
                        tempDatabase.child(uid).child("$patientID-$check").removeValue()
                        tempDatabase.child(patientID).child("$uid-$check").removeValue()
                    }
                }
            }
        }
    }
}