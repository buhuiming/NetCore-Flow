package com.bhm.sdk.demo.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bhm.netcore.R

/**
 * @author Buhuiming
 * @description:
 * @date :2023/5/6
 */
class StartActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(
            window,
            window.decorView
        )
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
        val button = findViewById<View>(R.id.btnOpen) as Button
        button.setOnClickListener {
            startActivity(Intent(this@StartActivity, MainActivity::class.java))
        }
    }
}