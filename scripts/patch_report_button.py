from pathlib import Path
p=Path('app/src/main/java/com/votoljk/unified/MainActivity.kt')
s=p.read_text()
if 'import android.graphics.Color' not in s:
    s=s.replace('import android.app.Activity\n', 'import android.app.Activity\nimport android.graphics.Color\nimport android.graphics.drawable.GradientDrawable\nimport android.view.Gravity\nimport android.view.View\nimport android.view.ViewGroup\n', 1)
needle='        setContentView(R.layout.activity_main)\n'
insert='''        val contentRoot = findViewById<ViewGroup>(android.R.id.content)\n        val dashboardView = contentRoot.getChildAt(0)\n        if (dashboardView != null && dashboardView !is FrameLayout) {\n            contentRoot.removeView(dashboardView)\n            val frame = FrameLayout(this)\n            frame.addView(dashboardView, ViewGroup.LayoutParams(-1, -1))\n            val reportButton = Button(this).apply {\n                text = "REPORT"\n                textSize = 11f\n                setTextColor(Color.WHITE)\n                background = GradientDrawable().apply {\n                    setColor(Color.rgb(18, 30, 42))\n                    setStroke(2, Color.rgb(0, 232, 255))\n                    cornerRadius = 100f\n                }\n                elevation = 10f\n                setOnClickListener { startActivity(Intent(this@MainActivity, ReportActivity::class.java)) }\n            }\n            val lp = FrameLayout.LayoutParams(dpReport(92), dpReport(52), Gravity.BOTTOM or Gravity.END)\n            lp.setMargins(0, 0, dpReport(18), dpReport(18))\n            frame.addView(reportButton, lp)\n            contentRoot.addView(frame, ViewGroup.LayoutParams(-1, -1))\n        }\n'''
if 'text = "REPORT"' not in s:
    s=s.replace(needle, needle+insert, 1)
if 'private fun dpReport(' not in s:
    marker='    private fun requestBtPermissions() {'
    s=s.replace(marker, '    private fun dpReport(v: Int): Int = (v * resources.displayMetrics.density).toInt()\n\n'+marker, 1)
p.write_text(s)
