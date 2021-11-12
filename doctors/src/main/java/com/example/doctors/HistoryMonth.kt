package com.example.doctors

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.doctors.databinding.CalendarDayLayoutBinding
import com.example.doctors.databinding.FragmentHistoryMonthBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.kizitonwose.calendarview.CalendarView.Companion.SIZE_SQUARE
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import kotlinx.android.synthetic.main.fragment_history_month.*
import java.text.DateFormatSymbols
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.*


class HistoryMonth(id: String) : Fragment() {
    private lateinit var docDatabase: DatabaseReference
    private lateinit var binding: FragmentHistoryMonthBinding
    private val doc = id
    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_history_month, container, false)
        val title: TextView = root.findViewById(R.id.titleText)
        val calendarView: com.kizitonwose.calendarview.CalendarView = root.findViewById(R.id.calendarView)
        val views: LinearLayout = root.findViewById(R.id.preview)
        val noHistory: TextView = root.findViewById(R.id.noHistoryText)

        // initialize binding for fragment
        binding = FragmentHistoryMonthBinding.bind(root)

        // initialize calendar values
        val currentMonth = YearMonth.now()
        val firstMonth = currentMonth.minusMonths(10)
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        calendarView.setup(firstMonth, currentMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)
        calendarView.daySize = SIZE_SQUARE

        /******* MONTH CONTAINER/BINDER *******/
        class MonthViewContainer(view: View) : ViewContainer(view) {
            val textView = view.findViewById<TextView>(R.id.headerTextView)
        }
        calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                container.textView.text = "${month.yearMonth.month.name.toLowerCase().capitalize()} ${month.year}"
            }
        }
        /**************************************/

        /******** DAY CONTAINER/BINDER ********/
        class DayViewContainer(view: View) : ViewContainer(view) {
            val binding = CalendarDayLayoutBinding.bind(view)
            // Will be set when this container is bound
            lateinit var day: CalendarDay

            init {
                view.setOnClickListener {
                    if (day.owner == DayOwner.THIS_MONTH) {
                        selectDate(day.date, views, title, noHistory)
                    }
                }
            }
        }

        binding.calendarView.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                val text = container.binding.calendarDayText
                val dot = container.binding.dotView

                text.text = day.date.dayOfMonth.toString()

                if (day.owner == DayOwner.THIS_MONTH) {
                    checkHistory(day.date, dot)
                    text.makeVisible()
                    when (day.date) {
                        today ->    {
                            text.setTextColorRes(R.color.white)
                            text.setBackgroundResource(R.drawable.calendar_today_bg)
                            //displayHistory(context, views, day.date.month.value, day.date.year, day.date.dayOfMonth, title, noHistory)
                        }
                        selectedDate -> {
                            text.setTextColorRes(R.color.salmon)
                            text.setBackgroundResource(R.drawable.calendar_selected_bg)
                            text.background.alpha = 150
                        }
                        else -> {
                            if(day.date.dayOfWeek == DayOfWeek.SUNDAY)   {
                                text.setTextColorRes(R.color.gray)
                                text.background = null
                            }   else    {
                                text.setTextColorRes(R.color.dark_gray)
                                text.background = null
                            }
                        }
                    }
                } else {
                    text.makeInVisible()
                    dot.makeInVisible()
                }
            }
        }
        /**************************************/

        return root
    }

    // function for when date is selected
    private fun selectDate(date: LocalDate, views: LinearLayout, title: TextView, noHistory: TextView) {
        val context = this.requireContext()
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            oldDate?.let { calendarView.notifyDateChanged(it) }
            calendarView.notifyDateChanged(date)
            displayHistory(context, views, date.month.value, date.year, date.dayOfMonth, title, noHistory)
        }
    }

    // function to display history
    private fun displayHistory(context: Context, views: LinearLayout, month: Int, year: Int, dayOfMonth: Int, title: TextView, noHistory: TextView)    {
        // make sure card views are not doubled
        views.removeAllViews()

        // initialize string format of day, month and year
        var counter = 0
        val monthStr = DateFormatSymbols().months[month-1]
        val yearFormat = year - 2000
        val monthFormat =
            if(month in 1..9)    {
                "0$month"
            }   else    {
                month.toString()
            }
        val dayFormat =
            if(dayOfMonth in 1..9)  {
                "0$dayOfMonth"
            }   else    {
                dayOfMonth.toString()
            }

        // date format is now same as in database
        val dateSelected = "$monthFormat$dayFormat$yearFormat"
        title.text = "$monthStr $dayOfMonth, $year"

        // check in database and display as card view if history exists
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
        docDatabase.child(doc).get().addOnSuccessListener { doctor ->
            if (doctor.child("history").child(dateSelected).exists()) {
                views.visibility = VISIBLE
                noHistory.visibility = GONE
                for (entries in doctor.child("history").child(dateSelected).children) {
                    if(entries.child("endTime").exists())   {
                        counter++
                        val name = entries.child("name").value.toString()
                        val bday = entries.child("bday").value.toString()
                        val reason = entries.child("reason").value.toString()
                        val start = entries.child("startTime").value.toString()
                        val end = entries.child("endTime").value.toString()
                        val slot = entries.key.toString()

                        val startSplit = start.split(":")
                        val endSplit = end.split(":")
                        if (startSplit[0] == endSplit[0]) {
                            val consTime = endSplit[1].toInt() - startSplit[1].toInt()
                            addCard(views, context, "Patient #$counter", name, slot, bday, start, end, reason, "$consTime minutes")
                        } else if (startSplit[0] != endSplit[0]) {
                            val consTime = (60 - startSplit[1].toInt()) + endSplit[1].toInt()
                            addCard(views, context, "Patient #$counter", name, slot, bday, start, end, reason, "$consTime minutes")
                        }
                    }
                }
            }   else    {
                views.visibility = GONE
                noHistory.visibility = VISIBLE
            }
        }
    }

    // function to check if history is present in date
    private fun checkHistory(date: LocalDate, dot: View)  {
        // separate date to month, dayOfMonth and year
        val month = date.month.value
        val year = date.year
        val dayOfMonth = date.dayOfMonth

        // initialize string format of month, day and year
        val yearFormat = year - 2000
        val monthFormat =
            if(month in 1..9)    {
                "0$month"
            }   else    {
                month.toString()
            }
        val dayFormat =
            if(dayOfMonth in 1..9)  {
                "0$dayOfMonth"
            }   else    {
                dayOfMonth.toString()
            }

        // date format is now same as in database
        val dateSelected = "$monthFormat$dayFormat$yearFormat"

        // check if history exists in database
        docDatabase = FirebaseDatabase.getInstance().getReference("DOCTOR")
        docDatabase.child(doc).get().addOnSuccessListener { doctor ->
            if (doctor.child("history").child(dateSelected).exists()) {
                if(date == selectedDate)    {
                    dot.makeInVisible()
                }   else    {
                    dot.makeVisible()
                }
            }   else    {
                dot.makeInVisible()
            }
        }
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
        time.text = "Consultation Time: $stimeStr - $etimeStr ($consTime)"
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