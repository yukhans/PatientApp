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
import kotlinx.android.synthetic.main.hd_form2.*
import java.text.SimpleDateFormat
import java.util.*

class Screening_q2: AppCompatActivity() {
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
        setContentView(R.layout.hd_form2)

        supportActionBar?.title = "Doctor Screening Form"
        supportActionBar?.subtitle = "Question 2"

        //get date today
        dateToday = Calendar.getInstance()
        nowmonth = dateToday.get(Calendar.MONTH) + 1
        nowday = dateToday.get(Calendar.DAY_OF_MONTH) - 1
        nowyear = dateToday.get(Calendar.YEAR) - 2000

        val intent = intent
        val uid = intent.getStringExtra("id")
        val firstName = intent.getStringExtra("firstName")
        val lastName = intent.getStringExtra("lastName")
        val spec = intent.getStringExtra("spec")
        val sex = intent.getStringExtra("sex")
        val consTime = intent.getStringExtra("consTime")

        var q2_yes_state = false
        var q2_no_state = false
        var tested_yes_state = false
        var tested_no_state = false
        var complete_yes_state = false
        var complete_no_state = false

        q2_yes.setOnClickListener {
            q2_yes_state = q2_yes.isChecked
            if(q2_yes_state){
                q2_no.isChecked = false
                q2_no_state = false
                wereyou.visibility = VISIBLE
                tested_yes.visibility = VISIBLE
                tested_no.visibility = VISIBLE
                nextBtn.visibility = GONE
            }else{
                wereyou.visibility = GONE
                tested_yes.visibility = GONE
                tested_no.visibility = GONE
                completequarantinegroup.visibility = GONE

                nextBtn.visibility = GONE
            }
        }

        q2_no.setOnClickListener {
            q2_no_state = q2_no.isChecked
            if(q2_no_state){
                q2_yes.isChecked = false
                q2_yes_state = false
                wereyou.visibility = GONE
                tested_yes.visibility = GONE
                tested_no.visibility = GONE
                completequarantinegroup.visibility = GONE

                nextBtn.visibility = VISIBLE
            }else{
                nextBtn.visibility = GONE
            }
        }

        tested_yes.setOnClickListener {
            tested_yes_state = tested_yes.isChecked
            if(tested_yes_state){
                tested_no_state = false
                tested_no.isChecked = false
                nextBtn.visibility = VISIBLE
                completequarantinegroup.visibility = GONE
            }
            else{
                nextBtn.visibility = GONE
            }

        }

        tested_no.setOnClickListener {
            tested_no_state = tested_no.isChecked
            if(tested_no_state){
                nextBtn.visibility = GONE
                tested_yes_state = false
                tested_yes.isChecked = false
                completequarantinegroup.visibility = VISIBLE
            }
            else{
                completequarantinegroup.visibility = GONE
            }

        }

        complete_yes.setOnClickListener {
            complete_yes_state = complete_yes.isChecked
            if(complete_yes_state){
                complete_no.isChecked = false
                complete_no_state = false
                nextBtn.visibility = VISIBLE
            }else{
                nextBtn.visibility = GONE
            }
        }

        complete_no.setOnClickListener {
            complete_no_state = complete_no.isChecked
            if(complete_no_state){
                complete_yes.isChecked = false
                complete_yes_state = false
                nextBtn.visibility = VISIBLE
                callDialog(uid.toString(), firstName.toString(), lastName.toString(), spec.toString(), sex.toString(), consTime.toString())
            }
        }

        nextBtn.setOnClickListener {
            if(tested_yes_state){
                val intent4 = Intent(this, Screening_q4::class.java)
                intent4.putExtra("id", uid)
                intent4.putExtra("firstName", firstName)
                intent4.putExtra("lastName", lastName)
                intent4.putExtra("spec", spec)
                intent4.putExtra("sex", sex)
                intent4.putExtra("consTime", consTime)
                startActivity(intent4)

            }else{
                val intent3 = Intent(this, Screening_q3::class.java)
                intent3.putExtra("id", uid)
                intent3.putExtra("firstName", firstName)
                intent3.putExtra("lastName", lastName)
                intent3.putExtra("spec", spec)
                intent3.putExtra("sex", sex)
                intent3.putExtra("consTime", consTime)
                startActivity(intent3)
            }
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
                                                patientDatabase.child(patientID).child("states").child("doctorCancelled").child("reason").setValue("Doctor failed health screening for $date.")
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