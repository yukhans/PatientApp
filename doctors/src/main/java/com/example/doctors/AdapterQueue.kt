package com.example.doctors

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AdapterQueue (private val queueList: ArrayList<String>, private val selection: Selection) : RecyclerView.Adapter<AdapterQueue.ViewHolder>() {

    var selectedItemPos = -1
    var lastItemSelectedPos = -1

    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val nameText : TextView = itemView.findViewById(R.id.name)
        val slotText : TextView = itemView.findViewById(R.id.slot)
        val titleTxt : TextView = itemView.findViewById(R.id.title)
        val specText : TextView = itemView.findViewById(R.id.spec)
        val checkbox : CheckBox = itemView.findViewById(R.id.patient_checker)

        init {
            itemView.setOnClickListener {
                selectedItemPos = adapterPosition
                if(lastItemSelectedPos == -1)
                    lastItemSelectedPos = selectedItemPos
                else {
                    notifyItemChanged(lastItemSelectedPos)
                    lastItemSelectedPos = selectedItemPos
                }
                notifyItemChanged(selectedItemPos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder   {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.recycler_queue,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return queueList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = queueList[position]
        val split = currentItem.split("-")
        val date = split[1]

        // initialize date format
        // divide date to individual digits
        val sixth = date.toInt().mod(10)
        val fifth = ((date.toInt().mod(100)).minus(sixth)).div(10)
        val fourth = ((date.toInt().mod(1000)).minus(date.toInt().mod(100))).div(100)
        val third = ((date.toInt().mod(10000)).minus(date.toInt().mod(1000))).div(1000)
        val second = ((date.toInt().mod(100000)).minus(date.toInt().mod(10000))).div(10000)
        val first = ((date.toInt().mod(1000000)).minus(date.toInt().mod(100000))).div(100000)

        val year = "$fifth$sixth"
        val month = "$third$fourth"
        val day = "$first$second"

        val dateFormat: LocalDate = LocalDate.of(year.toInt(), day.toInt(), month.toInt())
        val dateFormatted: String = dateFormat.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd"))

        // set text views
        holder.titleTxt.text = "Patient #${split[0]}"
        holder.slotText.text = "$dateFormatted at ${split[2]}"
        holder.nameText.text = split[3]
        holder.specText.text = "For ${split[4]}"
        val now: String = holder.nameText.text.toString()

        if(position == selectedItemPos) {
            holder.checkbox.isChecked = true
            selection.changeChoice(currentItem)
        }else if(selection.getChoice()==now && lastItemSelectedPos == -1 && position!=-1){
            lastItemSelectedPos = position
            holder.checkbox.isChecked = true
        }else{
            holder.checkbox.isChecked = false
        }
    }

}








