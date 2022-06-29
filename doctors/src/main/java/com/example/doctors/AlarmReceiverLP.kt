package com.example.doctors

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AlarmReceiverLP : BroadcastReceiver() {
    // database
    private lateinit var docDatabase: DatabaseReference
    private lateinit var patientDatabase: DatabaseReference
    private lateinit var database: DatabaseReference
    private lateinit var tempDatabase: DatabaseReference

    //write here the code to execute when alarm is triggered
    override fun onReceive(context: Context?, intent: Intent?) {
        // get extras
        val bundle: Bundle? = intent?.extras
        val id = bundle!!.getString("id").toString()
        val patientID = bundle.getString("patientID").toString()
        val date = bundle.getString("date").toString()
        val slot = bundle.getString("slot").toString()
        val name = bundle.getString("name").toString()
        val reason = bundle.getString("reason").toString()

        val i = Intent(context,CurrentQueue::class.java)
        intent!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context,0,i,0)

        val builder = NotificationCompat.Builder(context!!,"notif2")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Doctors App")
            .setContentText("The next patient in queue is late.")
            .setStyle(NotificationCompat.BigTextStyle().
                bigText("The next patient in your queue is late for their appointment. " +
                        "Their booking will be cancelled immediately."))
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(1234,builder.build())

        database = FirebaseDatabase.getInstance().getReference("DOCTOR")
        database.child(id).child("patientIsLate").setValue(true)
    }
}