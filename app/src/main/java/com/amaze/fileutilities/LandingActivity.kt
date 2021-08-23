package com.amaze.fileutilities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.amaze.fileutilities.databinding.LandingActivityBinding

class LandingActivity : AppCompatActivity() {

    private lateinit var landingActivityBinding: LandingActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        landingActivityBinding = LandingActivityBinding.inflate(layoutInflater)
        setContentView(landingActivityBinding.root)
    }
}