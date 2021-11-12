package com.example.patientapp

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaseauth.R
import com.example.firebaseauth.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_patient_login.*
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*


class PatientRegistration : AppCompatActivity() {
    //view binding
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()

        //calendar
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        var cYear = year
        var cMonth = month
        var cDay = day

        var sex = ""
        var successAgree = false
        var successGender = false
        var success = true

        // check gender radio buttons
        if(male_btn.isChecked || female_btn.isChecked)  {
            successGender = true
        }

        // check t&c checkbox
        if(checkBox.isChecked)  {
            successAgree = true
        }

        binding.bday.setOnClickListener{
            val showCalendar = DatePickerDialog(this, { _, mYear, mMonth, dayOfMonth ->
                //set to calendar
                cYear = mYear
                cMonth = mMonth
                cDay = dayOfMonth
                var fMonth = mMonth + 1
                binding.bday.text = "$fMonth/$dayOfMonth/$mYear"
                cal.set(cYear,cMonth,dayOfMonth)
            },year,month,day)

            showCalendar.show()
        }

        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when(checkedId) {
                R.id.male_btn -> sex = "Male"
                R.id.female_btn -> sex = "Female"
            }
        }

        registerButton.setOnClickListener {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            if (fname.text.toString().isNullOrEmpty() || lname.text.toString().isNullOrEmpty() || binding.bday.text.toString().isNullOrEmpty() || registeredEmail.text.toString().isNullOrEmpty() || registeredPass.text.toString().isNullOrEmpty() || successGender==false) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            }
            else if(successAgree==false)    {       // pop-up toast to agree to terms & service
                Toast.makeText(applicationContext, "Agreeing to Terms of Service and Privacy Policy is required.", Toast.LENGTH_SHORT).show()
            }
            else
            {
                register(registeredEmail.text.toString(), registeredPass.text.toString(), fname.text.toString(), lname.text.toString(), binding.bday.text.toString(), sex)
            }

        }

        goToLogin.setOnClickListener {
            //init login intent
            val login_patient_intent = Intent(this, PatientLogin::class.java)
            startActivity(login_patient_intent)

        }

    }

    private fun register(email: String, password: String, firstname: String, lastname: String, bday: String, sex: String) {
        firebaseAuth.createUserWithEmailAndPassword(
            email, password)

            .addOnCompleteListener(this) { task1 ->
                if (task1.isSuccessful) {
                    firebaseAuth.currentUser?.sendEmailVerification()
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                database = FirebaseDatabase.getInstance().getReference("Users")
                                val User = UsersData("$firstname $lastname", sex, email, bday)
                                database.child(firebaseAuth.currentUser!!.uid)
                                    .setValue(User)
                                    .addOnSuccessListener {
                                        fname.text.clear()
                                        lname.text.clear()
                                        registeredEmail.text.clear()
                                        registeredPass.text.clear()
                                        male_btn.isChecked = false
                                        female_btn.isChecked = false
                                        checkBox.isChecked = false
                                        fname.requestFocus()
                                    }
                                Toast.makeText(this, "Sign Up Successful. Check your email for verification", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Sign Up Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }


    override fun onBackPressed() {
        // Do Here what ever you want do on back press;
    }
}