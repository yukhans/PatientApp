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
import java.text.SimpleDateFormat
import java.util.*

class DoctorLogin : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var tempDatabase: DatabaseReference

    private var success = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_login)

        // first check if doctor is already signed in
        loadData()

        loginBtn.setOnClickListener {
            val idText = idtext.text.toString()
            val keyText = passtext.text.toString()

            if(idText.isNotEmpty() && keyText.isNotEmpty()) {
                readData(idText, keyText, this)
            }   else    {
                Toast.makeText(this, "ID or Key not provided", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readData(idText: String, keyText: String, context: Context) {
        //check if username and password exist
        database = FirebaseDatabase.getInstance().getReference("DOCTOR")
        database.get().addOnSuccessListener { doc ->
            for (doctorUserSnapShot in doc.children) {
                if (doctorUserSnapShot.key.toString() == idText && doctorUserSnapShot.child("pass").value.toString() == keyText) {
                    //if username and password match save data in Sharedpreferences
                    val firstName = doctorUserSnapShot.child("firstName").value.toString()
                    val lastName = doctorUserSnapShot.child("lastName").value.toString()
                    val spec = doctorUserSnapShot.child("spec").value.toString()
                    val sex = doctorUserSnapShot.child("sex").value.toString()
                    val consTime = doctorUserSnapShot.child("avgConsTime").value.toString()
                    saveData(idText, firstName, lastName, spec, sex, consTime.toInt())

                    // change islogged in to true
                    tempDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
                    tempDatabase.child(idText).child("isLoggedIn").setValue(true)

                    // set updateSched state according to if schedule node exists under doctor id
                    if (doctorUserSnapShot.child("schedule").exists()) {
                        val schedUpdateState = mapOf(
                            "updateSched" to true
                        )
                        tempDatabase.child(idText).updateChildren(schedUpdateState)
                    } else {
                        val schedUpdateState = mapOf(
                            "updateSched" to false
                        )
                        tempDatabase.child(idText).updateChildren(schedUpdateState)
                    }

                    if(!(doctorUserSnapShot.child("queueCount").exists()))  {
                        tempDatabase.child(idText).child("queueCount").setValue(0)
                    }

                    tempDatabase.child(idText).child("patientIsLate").setValue(false)

                    tempDatabase.child(idText).child("notifyFP").child("date").setValue("000000")
                    tempDatabase.child(idText).child("notifyFP").child("value").setValue(false)

                    if(!(doctorUserSnapShot.child("hasAnsweredScreening").exists()))  {
                        tempDatabase.child(idText).child("hasAnsweredScreening").child("date").setValue("000000")
                        tempDatabase.child(idText).child("hasAnsweredScreening").child("result").setValue("fail")
                    }

                    //change passwordmatch to true
//                    isPasswordMatch = true
//                    getID = true
                    success = true

                    //go to next activity
                    val intent = Intent(this, DoctorDashboard::class.java)
                    intent.putExtra("id", idText)
                    intent.putExtra("firstName", firstName)
                    intent.putExtra("lastName", lastName)
                    intent.putExtra("spec", spec)
                    intent.putExtra("sex", sex)
                    intent.putExtra("consTime", consTime)
                    startActivity(intent)
                }
            }
            if(!success) {
                val unregisteredDialogForm = AlertDialog.Builder(context, R.style.AlertDialog)
                    .setTitle("Invalid ID or key!")
                    .setMessage("Please enter your correct 8-digit ID and key or contact admin.")
                    .setNeutralButton("OK") { _, _ ->
                        idtext.setText("")
                        passtext.setText("")
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