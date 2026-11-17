package com.clove

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val error = intent.getStringExtra("error") ?: "No error details available"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121216"))
            setPadding(48, 80, 48, 48)
        }

        val title = TextView(this).apply {
            text = "⚠️ Clove Browser Crash"
            setTextColor(Color.parseColor("#FF5555"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "An uncaught exception occurred. Details below:"
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 13f
            setPadding(0, 0, 0, 24)
        }
        root.addView(subtitle)

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 24)
        }

        val copyButton = Button(this).apply {
            text = "Copy Error"
            setBackgroundColor(Color.parseColor("#0A84FF"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Clove Crash", error))
                Toast.makeText(this@CrashActivity, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
            }
        }
        buttonRow.addView(copyButton)

        val restartButton = Button(this).apply {
            text = "Restart App"
            setBackgroundColor(Color.parseColor("#33333E"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val intent = Intent(this@CrashActivity, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
                finish()
            }
        }
        buttonRow.addView(restartButton)
        root.addView(buttonRow)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(Color.parseColor("#1C1C24"))
            setPadding(24, 24, 24, 24)
        }

        val logView = TextView(this).apply {
            text = error
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        scrollView.addView(logView)
        root.addView(scrollView)

        setContentView(root)
    }
}
