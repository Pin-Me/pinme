package com.pkm.pinme.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.pkm.pinme.R
import com.pkm.pinme.ui.scan.ScanQRActivity

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, ScanQRActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish()
        }, 3000)
    }
}