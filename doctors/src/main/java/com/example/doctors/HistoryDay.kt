package com.example.doctors

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoryDay(id: String) : Fragment() {
    private lateinit var docDatabase: DatabaseReference
    private val doc = id
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_history_day, container, false)
        val layout: LinearLayout = root.findViewById(R.id.patientsView)
        val noHistory: TextView = root.findViewById(R.id.noHistoryText)
        val dateText: TextView = root.findViewById(R.id.dateText)

        val sdf = SimpleDateFormat("MMddyy HH:mm")
        val currentDate = sdf.format(java.util.Date())
        val dateTime: List<String> = currentDate.toString().split(" ")
        val date = dateTime[0]
        val context = this.requireContext()

        val today = LocalDate.now()
        val formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd"))

        dateText.text = formattedDate

        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
        docDatabase.child(doc).get().addOnSuccessListener { doctor ->
            if(doctor.child("history").exists())    {
                noHistory.visibility = GONE

                if(doctor.child("history").child(date).exists())    {
                    var counter = 0
                    for(i in doctor.child("history").child(date).children)  {
                        counter++
                        val name = i.child("name").value.toString()
                        val bday = i.child("bday").value.toString()
                        val reason = i.child("reason").value.toString()
                        val start = i.child("startTime").value.toString()
                        val slot = i.key.toString()

                        if(i.child("endTime").exists()) {
                            val end = i.child("endTime").value.toString()

                            val startSplit = start.split(":")
                            val endSplit = end.split(":")
                            if(startSplit[0]==endSplit[0])  {
                                val consTime = endSplit[1].toInt() - startSplit[1].toInt()
                                addCard(layout, context, "Patient #$counter", name, slot, bday, start, end, reason, "$consTime minutes")
                            }   else if(startSplit[0]!=endSplit[0]) {
                                val consTime = (60 - startSplit[1].toInt()) + endSplit[1].toInt()
                                addCard(layout, context, "Patient #$counter", name, slot, bday, start, end, reason, "$consTime minutes")
                            }
                        }   else    {
                            addCard(layout, context, "Patient #$counter", name, slot, bday, start, "now", reason, "Currently serving this patient.")
                        }
                    }
                }   else    {
                    // no history
                    noHistory.visibility = VISIBLE
                }

            }   else    {
                // no history
                noHistory.visibility = VISIBLE
            }
        }

        return root
    }

    // function to add patient as card
    private fun addCard(layout: LinearLayout, context: Context, titleStr: String, nameStr: String, slotStr: String, bdayStr: String, stimeStr: String, etimeStr: String, rsn: String, consTime: String) {
        val cardView = CardView(context)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(50, 25, 50, 25)
        val cardLinearLayout = LinearLayout(context)
        cardLinearLayout.orientation = LinearLayout.VERTICAL

        cardView.setContentPadding(50, 50, 50, 50)
        cardView.layoutParams = layoutParams
        cardView.radius = 20F
        cardView.setCardBackgroundColor(Color.argb(255, 242, 242, 242))
        cardView.cardElevation = 4F

        val title = TextView(context)
        title.text = "$titleStr - $nameStr"
        title.textSize = 20F
        title.setTextColor(Color.argb(255, 64, 30, 30))
        title.setTypeface(SANS_SERIF, BOLD)

        val slot = TextView(context)
        slot.text = "Slot: $slotStr"
        slot.textSize = 16F
        slot.setTextColor(Color.argb(255, 243, 103, 103))
        slot.setTypeface(SANS_SERIF, BOLD)

        val reason = TextView(context)
        reason.text = "Reason: $rsn"
        reason.setTextColor(Color.argb(255, 51, 51, 51))
        reason.setTypeface(SANS_SERIF, NORMAL)

        val bday = TextView(context)
        bday.text = "Birthdate: $bdayStr"
        bday.setTextColor(Color.argb(255, 51, 51, 51))
        bday.setTypeface(SANS_SERIF, NORMAL)

        val time = TextView(context)
        time.text = "Consultation Time: $stimeStr - $etimeStr ($consTime)"
        time.setTextColor(Color.argb(255, 51, 51, 51))
        time.setTypeface(SANS_SERIF, NORMAL)

        cardLinearLayout.addView(title)
        cardLinearLayout.addView(slot)
        cardLinearLayout.addView(bday)
        cardLinearLayout.addView(reason)
        cardLinearLayout.addView(time)
        cardView.addView(cardLinearLayout)
        layout.addView(cardView)
    }
}
