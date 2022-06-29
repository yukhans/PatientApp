package com.example.doctors

import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.hd_form3.*
import java.util.*

class Screening_q3 : AppCompatActivity() {
    lateinit var dateToday: Calendar
    var nowmonth = 0
    var nowday = 0
    var nowyear = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hd_form3)

        supportActionBar?.title = "Doctor Screening Form"
        supportActionBar?.subtitle = "Question 3"

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

        var q3_yes_state = false
        var q3_no_state = false
        var completed_yes_state = false
        var completed_no_state = false
        var negative_yes_state = false
        var negative_no_state = false

        q3_yes.setOnClickListener {
            q3_yes_state = q3_yes.isChecked
            if(q3_yes_state){
                q3_no_state = false
                q3_no.isChecked = false
                ifyestext.visibility = VISIBLE
                ifyesgroup.visibility = VISIBLE
            }else{
                ifyestext.visibility = GONE
                ifyesgroup.visibility = GONE
                doyoutext.visibility = GONE
                doyougroup.visibility = GONE
            }
        }

        q3_no.setOnClickListener {
            q3_no_state = q3_no.isChecked
            if(q3_no_state){
                q3_yes_state = false
                q3_yes.isChecked = false
                nextBtn.visibility = VISIBLE
            }else{
                nextBtn.visibility = GONE
                ifyestext.visibility = GONE
                ifyesgroup.visibility = GONE
                doyoutext.visibility = GONE
                doyougroup.visibility = GONE
            }
        }

        completed_yes.setOnClickListener {
            completed_yes_state = completed_yes.isChecked
            if(completed_yes_state){
                completed_no_state = false
                complete_no.isChecked = false
                doyoutext.visibility = VISIBLE
                negative_yes.visibility = VISIBLE
                negative_no.visibility = VISIBLE
            }else{
                doyoutext.visibility = GONE
                negative_yes.visibility = GONE
                negative_no.visibility = GONE
            }
        }

        complete_no.setOnClickListener {
            completed_no_state = complete_no.isChecked
            if(completed_no_state){
                completed_yes_state = false
                completed_yes.isChecked = false
                doyoutext.visibility = VISIBLE
                negative_yes.visibility = VISIBLE
                negative_no.visibility = VISIBLE
            }else{
                doyoutext.visibility = GONE
                negative_yes.visibility = GONE
                negative_no.visibility = GONE
            }
        }

        negative_yes.setOnClickListener {
            negative_yes_state = negative_yes.isChecked
            if(negative_yes_state){
                negative_no_state = false
                negative_no.isChecked = false
                nextBtn.visibility = VISIBLE
            }else{
                nextBtn.visibility = GONE
            }
        }

        negative_no.setOnClickListener {
            negative_no_state = negative_no.isChecked
            if(negative_no_state){
                negative_yes_state = false
                negative_yes.isChecked = false
                nextBtn.visibility = VISIBLE
            }else{
                nextBtn.visibility = GONE
            }
        }

        nextBtn.setOnClickListener {
            val intent4 = Intent(this, Screening_q4::class.java)
            intent4.putExtra("id", uid)
            intent4.putExtra("firstName", firstName)
            intent4.putExtra("lastName", lastName)
            intent4.putExtra("spec", spec)
            intent4.putExtra("sex", sex)
            intent4.putExtra("consTime", consTime)
            startActivity(intent4)
        }
    }
}