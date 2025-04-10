package com.example.safefitness.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.safefitness.R
import com.example.safefitness.ui.main.MainActivity

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val button = findViewById<Button>(R.id.onboardingButton)
        button.setOnClickListener {
            getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("isFirstLaunch", false)
                .apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
