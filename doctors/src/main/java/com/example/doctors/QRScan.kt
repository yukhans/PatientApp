package com.example.doctors

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.qr_scan.*

class QRScan : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qr_scan)

        supportActionBar?.title = "Patient App"
        supportActionBar?.subtitle = "QR Scanner"

        val bundle: Bundle? = intent.extras
        val link = bundle!!.getString("link")

        textView.text = link
    }
}