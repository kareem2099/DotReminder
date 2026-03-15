package com.FreeRave


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.FreeRave.remindme.MainActivity

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Immediately navigate to MainActivity
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
