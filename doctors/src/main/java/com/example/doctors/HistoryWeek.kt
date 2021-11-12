package com.example.doctors

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoryWeek(id: String) : Fragment() {
    private lateinit var docDatabase: DatabaseReference
    private val doc = id
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_history_week, container, false)
        val noHistoryText: TextView = root.findViewById(R.id.noHistoryText)

        val textviews = listOf<TextView>(
            root.findViewById(R.id.date7Text),
            root.findViewById(R.id.date6Text),
            root.findViewById(R.id.date5Text),
            root.findViewById(R.id.date4Text),
            root.findViewById(R.id.date3Text),
            root.findViewById(R.id.date2Text),
            root.findViewById(R.id.date1Text)
        )

        val textviews2 = listOf<TextView>(
            root.findViewById(R.id.date7Text2),
            root.findViewById(R.id.date6Text2),
            root.findViewById(R.id.date5Text2),
            root.findViewById(R.id.date4Text2),
            root.findViewById(R.id.date3Text2),
            root.findViewById(R.id.date2Text2),
            root.findViewById(R.id.date1Text2)
        )

        val cardviews = listOf<CardView>(
            root.findViewById(R.id.date7),
            root.findViewById(R.id.date6),
            root.findViewById(R.id.date5),
            root.findViewById(R.id.date4),
            root.findViewById(R.id.date3),
            root.findViewById(R.id.date2),
            root.findViewById(R.id.date1)
        )

        val views = listOf<LinearLayout>(
            root.findViewById(R.id.view7),
            root.findViewById(R.id.view6),
            root.findViewById(R.id.view5),
            root.findViewById(R.id.view4),
            root.findViewById(R.id.view3),
            root.findViewById(R.id.view2),
            root.findViewById(R.id.view1)
        )

        val context = this.requireContext()

        val cDate: LocalDate = LocalDate.now()
        var counter = 0
        var dayCounter = 0

        for(i in 1..7)  {
            val lDate: LocalDate = cDate.minusDays(i.toLong())
            val lDateFormatted: String = lDate.format(DateTimeFormatter.ofPattern("MMddyy"))

            textviews[i-1].text = lDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd"))
            textviews2[i-1].text = lDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd"))

            docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
            docDatabase.child(doc).get().addOnSuccessListener { doctor ->
                if(doctor.child("history").child(lDateFormatted).exists())  {
                    dayCounter++
                    noHistoryText.visibility = GONE
                    for(entries in doctor.child("history").child(lDateFormatted).children)  {
                        counter++
                        val name = entries.child("name").value.toString()
                        val bday = entries.child("bday").value.toString()
                        val reason = entries.child("reason").value.toString()
                        val start = entries.child("startTime").value.toString()
                        val end = entries.child("endTime").value.toString()
                        val slot = entries.key.toString()

                        val startSplit = start.split(":")
                        val endSplit = end.split(":")
                        if(startSplit[0]==endSplit[0])  {
                            val consTime = endSplit[1].toInt() - startSplit[1].toInt()
                            addCard(views[i-1], context, "Patient #$counter", name, slot, bday, start, end, reason, consTime.toString())
                        }   else if(startSplit[0]!=endSplit[0]) {
                            val consTime = (60 - startSplit[1].toInt()) + endSplit[1].toInt()
                            addCard(views[i-1], context, "Patient #$counter", name, slot, bday, start, end, reason, consTime.toString())
                        }
                    }

                    cardviews[i-1].setOnClickListener {
                        if(views[i-1].visibility == VISIBLE) {
                            TransitionManager.beginDelayedTransition(cardviews[i-1], AutoTransition())
                            views[i-1].visibility = GONE
                        } else    {
                            TransitionManager.beginDelayedTransition(cardviews[i-1], AutoTransition())
                            views[i-1].visibility = VISIBLE
                        }
                    }
                }   else    {
                    cardviews[i-1].visibility = GONE
                }
            }
        }

        if(dayCounter == 0) {
            noHistoryText.visibility = VISIBLE
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
        cardView.setCardBackgroundColor(Color.WHITE)
        cardView.cardElevation = 4F

        val title = TextView(context)
        title.text = "$titleStr - $nameStr"
        title.textSize = 20F
        title.setTextColor(Color.argb(255, 64, 30, 30))
        title.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD)

        val slot = TextView(context)
        slot.text = "Slot: $slotStr"
        slot.textSize = 16F
        slot.setTextColor(Color.argb(255, 243, 103, 103))
        slot.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)

        val reason = TextView(context)
        reason.text = "Reason: $rsn"
        reason.setTextColor(Color.argb(255, 51, 51, 51))
        reason.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)

        val bday = TextView(context)
        bday.text = "Birthdate: $bdayStr"
        bday.setTextColor(Color.argb(255, 51, 51, 51))
        bday.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)

        val time = TextView(context)
        time.text = "Consultation Time: $stimeStr - $etimeStr ($consTime minutes)"
        time.setTextColor(Color.argb(255, 51, 51, 51))
        time.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)

        cardLinearLayout.addView(title)
        cardLinearLayout.addView(slot)
        cardLinearLayout.addView(bday)
        cardLinearLayout.addView(reason)
        cardLinearLayout.addView(time)
        cardView.addView(cardLinearLayout)
        layout.addView(cardView)
    }
}