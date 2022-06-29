package com.example.doctors

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiverFP : BroadcastReceiver() {
    // database
    private lateinit var docDatabase: DatabaseReference
    private lateinit var patientDatabase: DatabaseReference
    private lateinit var database: DatabaseReference
    private lateinit var tempDatabase: DatabaseReference

    //write here the code to execute when alarm is triggered
    override fun onReceive(context: Context?, intent: Intent?) {
        Toast.makeText(context,"First patient reminder set.", Toast.LENGTH_LONG).show()
        // get extras
        val bundle: Bundle? = intent?.extras
        val id = bundle!!.getString("id").toString()
        val slot = bundle.getString("slot").toString()

        val i = Intent(context,DoctorDashboard::class.java)
        intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context,0,i,0)

        val builder = NotificationCompat.Builder(context!!,"notif3")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Doctors App")
            .setContentText("Reminder for your first patient.")
            .setStyle(NotificationCompat.BigTextStyle().
                bigText("Can you make it in time for your first appointment today at $slot?"))
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(1234,builder.build())

        val sdfDate = SimpleDateFormat("MMddyy")
        val date = sdfDate.format(Date())

        val updateChildren = mapOf(
            "date" to date,
            "value" to true
        )

        database = FirebaseDatabase.getInstance().getReference("DOCTOR")
        database.child(id).child("notifyFP").updateChildren(updateChildren)
//        database.child(id).child("notifyFP").child("date").setValue(date)
//        database.child(id).child("notifyFP").child("value").setValue(true)
    }
}