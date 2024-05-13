package com.pkm.pinme.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.pkm.pinme.R
import com.pkm.pinme.databinding.ActivityScanBinding

class ScanActivity : AppCompatActivity() {

    private lateinit var activityScanBinding: ActivityScanBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityScanBinding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(activityScanBinding.root)

        activityScanBinding.btnDisplay.setOnClickListener {
            if (activityScanBinding.edUrl.text.isEmpty()) {
                activityScanBinding.edUrl.error = "Please fill this field"
            } else {
                startActivity(
                    Intent(this, MainActivity::class.java).putExtra(
                        "url",
                        activityScanBinding.edUrl.getText().toString()
                    )
                )
            }
        }
    }
}