package com.example.doctors

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_doctor_login.*

class DoctorLogin : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var tempDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_login)

        // init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        // first check if doctor is already signed in
        loadData()

        loginBtn.setOnClickListener {
            val idtext = idtext.text.toString()
            val keytext = passtext.text.toString()

            if(idtext.isNotEmpty() && keytext.isNotEmpty()) {
                readData(idtext, keytext, this)
            }   else    {
                Toast.makeText(this, "ID or Key not provided", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readData(idtext: String, keytext: String, context: Context) {
        var isPasswordMatch = false
        var getID = false

        //check if username and password exist
        database = FirebaseDatabase.getInstance().getReference("DOCTOR")
        database.get().addOnSuccessListener { doc ->
            for (doctorUserSnapShot in doc.children) {
                if (doctorUserSnapShot.key.toString() == idtext && doctorUserSnapShot.child("pass").value.toString() == keytext) {
                    //if username and password match save data in Sharedpreferences
                    val firstName = doctorUserSnapShot.child("firstName").value.toString()
                    val lastName = doctorUserSnapShot.child("lastName").value.toString()
                    val spec = doctorUserSnapShot.child("spec").value.toString()
                    val sex = doctorUserSnapShot.child("sex").value.toString()
                    val consTime = doctorUserSnapShot.child("avgConsTime").value.toString()
                    saveData(idtext, firstName, lastName, spec, sex, consTime.toInt())

                    // change islogged in to true
                    tempDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                    tempDatabase.child(idtext).child("isLoggedIn").setValue(true)

                    // set updateSched state according to if schedule node exists under doctor id
                    if (doctorUserSnapShot.child("schedule").exists()) {
                        val schedUpdateState = mapOf(
                            "updateSched" to true
                        )
                        tempDatabase.child(idtext).updateChildren(schedUpdateState)
                    } else {
                        val schedUpdateState = mapOf(
                            "updateSched" to false
                        )
                        tempDatabase.child(idtext).updateChildren(schedUpdateState)
                    }

                    if(!(doctorUserSnapShot.child("queueCount").exists()))  {
                        tempDatabase.child(idtext).child("queueCount").setValue(0)
                    }

                    if(!(doctorUserSnapShot.child("hasAnsweredScreening").exists()))  {
                        tempDatabase.child(idtext).child("hasAnsweredScreening").child("date").setValue(0)
                        tempDatabase.child(idtext).child("hasAnsweredScreening").child("result").setValue("fail")
                    }

                    //change passwordmatch to true
                    isPasswordMatch = true
                    getID = true

                    //go to next activity
                    val intent = Intent(this, DoctorDashboard::class.java)
                    intent.putExtra("id", idtext)
                    intent.putExtra("firstName", firstName)
                    intent.putExtra("lastName", lastName)
                    intent.putExtra("spec", spec)
                    intent.putExtra("sex", sex)
                    intent.putExtra("consTime", consTime)
                    startActivity(intent)
                }
            }
            if(!isPasswordMatch) {
                val unregisteredDialogForm = AlertDialog.Builder(context, R.style.AlertDialog)
                    .setTitle("Invalid key!")
                    .setMessage("Please enter your correct key or contact admin.")
                    .setNeutralButton("OK") { _, _ ->

                    }
                unregisteredDialogForm.show()
            }
            if(!getID)  {
                val unregisteredDialogForm = AlertDialog.Builder(context, R.style.AlertDialog)
                    .setTitle("Invalid ID!")
                    .setMessage("Please enter your correct 8-digit ID or contact admin.")
                    .setNeutralButton("OK") { _, _ ->

                    }
                unregisteredDialogForm.show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "$exception", Toast.LENGTH_SHORT).show()
        }
    }

    //load data from local storage?
    private fun loadData() {
        val sharePreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        // get data
        val idtext = sharePreferences.getString("id",null)
        val firstName = sharePreferences.getString("firstName", null)
        val lastName = sharePreferences.getString("lastName", null)
        val spec = sharePreferences.getString("spec", null)
        val sex = sharePreferences.getString("sex", null)
        val consTime = sharePreferences.getInt("consTime", 0)
        val isLoggedIn = sharePreferences.getString("ISLOGGEDIN",null)

        if(isLoggedIn == "true"){
            val intent = Intent(this, DoctorDashboard::class.java)
            intent.putExtra("id", idtext.toString())
            intent.putExtra("firstName", firstName)
            intent.putExtra("lastName", lastName)
            intent.putExtra("spec", spec)
            intent.putExtra("sex", sex)
            intent.putExtra("consTime", consTime)
            intent.putExtra("ISLOGGEDIN", "true")
            startActivity(intent)
        }
    }

    //saved data from local storage?
    private fun saveData(idtext:String, firstName: String, lastName: String, spec: String, sex: String, consTime: Int) {
        val sharePreferences = getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
        val editor = sharePreferences.edit()

        //these are the parameters u want to save
        editor.apply {
            putString("id", idtext)
            putString("firstName", firstName)
            putString("lastName", lastName)
            putString("spec", spec)
            putString("sex", sex)
            putInt("consTime", consTime)
            putString("ISLOGGEDIN", "true")
        }.apply()

        Toast.makeText(this, "Data Saved", Toast.LENGTH_SHORT).show()
    }
}