package com.votoljk.unified

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class ReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        findViewById<Button>(R.id.reportClose).setOnClickListener { finish() }
        findViewById<Button>(R.id.reportExport).setOnClickListener {
            Toast.makeText(this, "Export Log tersedia dari dashboard utama.", Toast.LENGTH_SHORT).show()
        }
    }
}
