package com.example.doctors

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.hd_form4.*
import java.text.SimpleDateFormat
import java.util.*

class Screening_q4 : AppCompatActivity() {
    private lateinit var database: DatabaseReference

    lateinit var dateToday: Calendar
    var nowmonth = 0
    var nowday = 0
    var nowyear = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hd_form4)

        supportActionBar?.title = "Doctor Screening Form"
        supportActionBar?.subtitle = "Question 4"

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

        var q4_yes_state = false
        var q4_no_state = false
        var q4_1_yes_state = false
        var q4_1_no_state = false
        var q4_2_yes_state = false
        var q4_2_no_state = false
        var yes_quarantine_pos = false
        var no_quarantine_pos = false
        var swab_pos = false
        var swab_neg = false
        var swabTestDate = ""

        q4_yes.setOnClickListener{
            q4_yes_state = q4_yes.isChecked
            if(q4_yes_state){
                q4_no.isChecked = false
                q4_no_state = false
                q4_1_text.visibility = VISIBLE
                q4_1_group.visibility = VISIBLE
                swabtestview.visibility = GONE
                swabresult_group.visibility = GONE
                completed14days.visibility = GONE
                quarantine_group.visibility = GONE
                yes_positive.visibility = GONE
                yes_negative.visibility = GONE
                confirmBtn.visibility = GONE
                rt_pcr_checkgroup.visibility = GONE
                positive_group.visibility = GONE
            }
            else{
                q4_1_text.visibility = GONE
                q4_1_group.visibility = GONE
            }
        }

        q4_no.setOnClickListener{
            q4_no_state = q4_no.isChecked
            if(q4_no_state){
                q4_yes.isChecked = false
                q4_yes_state = false
                q4_1_text.visibility = VISIBLE
                q4_1_group.visibility = VISIBLE
                swabtestview.visibility = GONE
                swabresult_group.visibility = GONE
                completed14days.visibility = GONE
                quarantine_group.visibility = GONE
                yes_positive.visibility = GONE
                yes_negative.visibility = GONE
                confirmBtn.visibility = GONE
                rt_pcr_checkgroup.visibility = GONE
                positive_group.visibility = GONE
            }else{
                q4_1_text.visibility = GONE
                q4_1_group.visibility = GONE
            }
        }


        q4_1_yes.setOnClickListener{
            q4_1_yes_state =  q4_1_yes.isChecked

            if(q4_1_yes_state){
                q4_1_no_state = false
                q4_1_no.isChecked = false
                swabtestview.visibility = VISIBLE
                swabtestdate.visibility = VISIBLE
                completed14days.visibility = GONE
                quarantine_group.visibility = GONE
                yes_positive.visibility = GONE
                yes_negative.visibility = GONE
                confirmBtn.visibility = GONE
                rt_pcr_checkgroup.visibility = GONE
                positive_group.visibility = GONE
                subseq_question.visibility = GONE
                subseq_group.visibility = GONE

                if(swabtestdate.text.isNotEmpty()){
                    swabtestdate.setText("")
                }

                q4_2_textview.visibility = GONE
                q4_2_yes.visibility = GONE
                q4_2_no.visibility = GONE
                q4_2_checkgroup.visibility = GONE
            }
            if(q4_2_yes.isChecked && q4_2_no.isChecked || yes_positive.isChecked || yes_negative.isChecked){
                q4_2_yes.isChecked = false
                yes_positive.isChecked = false
                yes_negative.isChecked = false
            }
            if(!q4_1_yes_state){
                swabtestview.visibility = GONE
                swabtestdate.visibility = GONE
            }

        }

        q4_1_no.setOnClickListener{
            q4_1_no_state = q4_1_no.isChecked

            if(q4_1_no_state){
                swabtestview.visibility = GONE
                swabtestdate.visibility = GONE
                q4_1_yes_state = false
                q4_1_yes.isChecked = false
                q4_2_textview.visibility = VISIBLE
                q4_2_yes.visibility = VISIBLE
                q4_2_no.visibility = VISIBLE
                q4_2_checkgroup.visibility = VISIBLE
                ifpositive_pt_pcr.visibility = GONE
                positive_complete.visibility = GONE
                positive_group.visibility = GONE
                swabtestresult.visibility = GONE
                swabresult_group.visibility = GONE
                completed14days.visibility = GONE
                quarantine_group.visibility = GONE
                yes_positive.visibility = GONE
                yes_negative.visibility = GONE
                confirmBtn.visibility = GONE
                rt_pcr_checkgroup.visibility = GONE
                positive_group.visibility = GONE
            }
        }

        swabresult_pos.setOnClickListener{
            swab_pos =  swabresult_pos.isChecked
            if(swab_pos && swabTestDate.isNotEmpty()){
                swabresult_neg.isChecked = false
                swab_neg = false
                q4_2_textview.visibility = GONE
                q4_2_yes.visibility = GONE
                q4_2_no.visibility = GONE
                q4_2_yes.isChecked = false
                q4_2_no.isChecked = false
                q4_2_checkgroup.visibility = GONE
                ifpositive_pt_pcr.visibility = VISIBLE
                completed14days.visibility = VISIBLE
                quarantine_group.visibility = VISIBLE
                quarantine_yes.isChecked = false
                quarantine_no.isChecked = false
                q4_2_yes.isChecked = false
                q4_2_no.isChecked = false
            }
            else{
                ifpositive_pt_pcr.visibility = GONE
                completed14days.visibility = GONE
                quarantine_group.visibility = GONE
            }

            if(swabresult_pos.isChecked && swabresult_neg.isChecked){
                swabresult_pos.isChecked = false
            }

        }

        swabresult_neg.setOnClickListener{
            swab_neg =  swabresult_neg.isChecked
            if(swab_neg && swabTestDate.isNotEmpty()){
                swabresult_pos.isChecked = false
                swab_pos = false
                q4_2_yes.isChecked = false
                q4_2_no.isChecked = false
                q4_2_textview.visibility = VISIBLE
                q4_2_yes.visibility = VISIBLE
                q4_2_no.visibility = VISIBLE
                q4_2_checkgroup.visibility = VISIBLE
                ifpositive_pt_pcr.visibility = GONE
                completed14days.visibility = GONE
                quarantine_group.visibility = GONE
            }else{
                q4_2_textview.visibility = GONE
                q4_2_yes.visibility = GONE
                q4_2_no.visibility = GONE
                q4_2_checkgroup.visibility = GONE
            }
        }

        swabtestdate.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if(swabtestdate.text.isNotEmpty()){
                    swabTestDate = swabtestdate.text.toString()
                    swabtestresult.visibility = VISIBLE
                    swabresult_group.visibility = VISIBLE
                }else{
                    swabtestresult.visibility = GONE
                    swabresult_group.visibility = GONE
                    ifpositive_pt_pcr.visibility = GONE
                    q4_2_textview.visibility = GONE
                    q4_2_checkgroup.visibility = GONE
                    completed14days.visibility = GONE
                    quarantine_group.visibility = GONE

                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })


        q4_2_yes.setOnClickListener{
            q4_2_yes_state =  q4_2_yes.isChecked

            if(q4_2_yes_state){
                q4_2_no_state = false
                q4_2_no.isChecked = false
                yes_positive.visibility = VISIBLE
                yes_negative.visibility = VISIBLE
                confirmBtn.visibility = GONE
                positive_group.visibility = GONE
                rt_pcr_checkgroup.visibility = GONE
                lastgroup.visibility = GONE

            }
            else{
                yes_positive.visibility = GONE
                yes_negative.visibility = GONE
            }

            if(q4_2_yes.isChecked && q4_2_no.isChecked){
                q4_2_yes.isChecked = false
            }
        }

        var pos_ig = false
        var neg_ig = false

        yes_positive.setOnClickListener {
            pos_ig = yes_positive.isChecked
            if(pos_ig){
                neg_ig = false
                yes_negative.isChecked = false
                positive_group.visibility = VISIBLE
                rt_pcr_checkgroup.visibility = VISIBLE

                if(confirmBtn.visibility==VISIBLE){
                    confirmBtn.visibility=GONE
                }
            }
            else{
                positive_group.visibility = GONE
                rt_pcr_checkgroup.visibility = GONE
            }
            if(yes_positive.isChecked && yes_negative.isChecked){
                yes_positive.isChecked = false
            }
        }

        yes_negative.setOnClickListener {
            neg_ig = yes_negative.isChecked
            if(neg_ig){
                pos_ig = false
                yes_positive.isChecked = false
                confirmBtn.visibility=VISIBLE
                positive_group.visibility=GONE
                rt_pcr_checkgroup.visibility=GONE
            }
            else{
                confirmBtn.visibility=GONE
            }
            if(yes_negative.isChecked && yes_positive.isChecked){
                yes_negative.isChecked = false
            }
        }


        q4_2_no.setOnClickListener{
            q4_2_no_state =  q4_2_no.isChecked

            if(q4_2_no_state){
                q4_2_yes.isChecked = false
                q4_2_yes_state = false
                confirmBtn.text = "CONFIRM"
                confirmBtn.visibility = VISIBLE
                positive_group.visibility = GONE
                rt_pcr_checkgroup.visibility = GONE
                lastgroup.visibility = GONE
                yes_positive.visibility = GONE
                yes_negative.visibility = GONE

                if(positive_group.visibility==VISIBLE && rt_pcr_checkgroup.visibility == VISIBLE){
                    positive_group.visibility=GONE
                    rt_pcr_checkgroup.visibility =GONE
                }

                if(yes_positive.isChecked || yes_negative.isChecked){
                    yes_positive.isChecked = false
                    yes_negative.isChecked = false
                    yes_negative.visibility = GONE
                    yes_positive.visibility = GONE
                }

            }
            if(q4_2_no.isChecked && q4_2_yes.isChecked){
                q4_2_no.isChecked = false
            }
        }

        quarantine_yes.setOnClickListener {
            yes_quarantine_pos = quarantine_yes.isChecked
            if(yes_quarantine_pos){
                quarantine_no.isChecked = false
                no_quarantine_pos = false
                subseq_question.visibility = VISIBLE
                subseq_group.visibility = VISIBLE

                if(confirmBtn.visibility == VISIBLE){
                    confirmBtn.visibility = GONE
                }
            }

            if(quarantine_yes.isChecked && quarantine_no.isChecked){
                quarantine_yes.isChecked = false
            }
        }

        quarantine_no.setOnClickListener {
            no_quarantine_pos = quarantine_no.isChecked
            if(no_quarantine_pos){
                quarantine_yes.isChecked = false
                yes_quarantine_pos = false
                subseq_question.visibility = VISIBLE
                subseq_group.visibility = VISIBLE
            }
            if(quarantine_no.isChecked && quarantine_yes.isChecked){
                quarantine_no.isChecked = false
            }
        }

        var subseq_yes_state = false
        var subseq_no_state = false

        subseq_yes.setOnClickListener {
            subseq_yes_state = subseq_yes.isChecked
            if(subseq_yes_state){
                subseq_no_state = false
                subseq_no.isChecked = false
                q4_2_textview.visibility = VISIBLE
                q4_2_yes.visibility = VISIBLE
                q4_2_no.visibility = VISIBLE
                q4_2_checkgroup.visibility = VISIBLE
            }
            if(subseq_yes.isChecked && subseq_no.isChecked){
                subseq_yes.isChecked = false
            }
        }

        subseq_no.setOnClickListener {
            subseq_no_state = subseq_no.isChecked
            if(subseq_no_state){
                subseq_yes_state = false
                subseq_yes.isChecked = false
                q4_2_textview.visibility = VISIBLE
                q4_2_yes.visibility = VISIBLE
                q4_2_no.visibility = VISIBLE
                q4_2_checkgroup.visibility = VISIBLE
            }
            if(subseq_no.isChecked && subseq_yes.isChecked){
                subseq_no.isChecked = false
            }
        }

        var yes_ig = false
        var no_ig = false

        igYes.setOnClickListener {
            yes_ig = igYes.isChecked
            if(yes_ig){
                no_ig = false
                igNo.isChecked = false
                ifrtpcrnotdonegroup.visibility = VISIBLE
                lastgroup.visibility = VISIBLE
            }else{
                ifrtpcrnotdonegroup.visibility = GONE
                lastgroup.visibility = GONE
            }
            if(igYes.isChecked && igNo.isChecked){
                igYes.isChecked = false
            }
        }

        igNo.setOnClickListener {
            no_ig = igNo.isChecked
            if(no_ig){
                yes_ig = false
                igYes.isChecked = false
                confirmBtn.text = "CONFIRM"
                confirmBtn.visibility = VISIBLE

                if(ifrtpcrnotdonegroup.visibility==VISIBLE){
                    ifrtpcrnotdonegroup.visibility = GONE
                    lastgroup.visibility = GONE
                }
            }else{
                confirmBtn.visibility = GONE
            }

            if(igNo.isChecked && igYes.isChecked){
                igNo.isChecked = false
            }
        }

        var yeslast = false
        var nolast = false

        lastyes.setOnClickListener {
            yeslast = lastyes.isChecked
            if(yeslast){
                nolast = false
                lastno.isChecked = false
                confirmBtn.text = "CONFIRM"
                confirmBtn.visibility = VISIBLE
            }
            else{
                confirmBtn.visibility = GONE
            }
            if(lastyes.isChecked && lastno.isChecked){
                lastyes.isChecked = false
            }
        }

        lastno.setOnClickListener {
            nolast = lastno.isChecked
            if(nolast){
                yeslast = false
                lastyes.isChecked = false
                confirmBtn.text = "CONFIRM"
                confirmBtn.visibility = VISIBLE
            }
            else{
                confirmBtn.visibility = GONE
            }
            if(lastno.isChecked && lastyes.isChecked){
                lastno.isChecked = false
            }
        }


        confirmBtn.setOnClickListener {
            val sdf = SimpleDateFormat("MMddyy")
            val sdfTime = SimpleDateFormat("HH:mm")
            val date = sdf.format(Date())
            val time = sdfTime.format(Date())

            database = FirebaseDatabase.getInstance().getReference("DOCTOR")
            database.child(uid.toString()).child("hasAnsweredScreening").child("date").setValue(date).addOnSuccessListener {
                Toast.makeText(applicationContext,"You have passed the Health Declaration Form!", Toast.LENGTH_SHORT).show()

                database = FirebaseDatabase.getInstance().getReference("DOCTOR")
                database.child(uid.toString()).child("hasAnsweredScreening").child("result").setValue("pass")
                database.child(uid.toString()).child("hasAnsweredScreening").child("fillUpTime").setValue(time)

                val intent2 = Intent(this, DoctorDashboard::class.java)
                intent2.putExtra("id", uid.toString())
                intent2.putExtra("firstName", firstName)
                intent2.putExtra("lastName", lastName)
                intent2.putExtra("spec", spec)
                intent2.putExtra("sex", sex)
                intent2.putExtra("consTime", consTime)
                startActivity(intent2)
            }
        }
    }
}