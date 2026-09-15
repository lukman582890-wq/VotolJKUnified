from pathlib import Path
p=Path('app/src/main/java/com/votoljk/unified/MainActivity.kt')
s=p.read_text()
for line in [
    'import android.graphics.Color\n',
    'import android.graphics.drawable.GradientDrawable\n',
    'import android.view.Gravity\n',
    'import android.view.View\n',
    'import android.view.ViewGroup\n',
    'import android.widget.FrameLayout\n',
]:
    if line not in s:
        s=s.replace('import android.app.Activity\n', 'import android.app.Activity\n'+line, 1)
needle='        setContentView(R.layout.activity_main)\n'
start=s.find(needle)
if start >= 0:
    block_start=start+len(needle)
    old_marker='        val contentRoot = findViewById<ViewGroup>(android.R.id.content)\n'
    os=s.find(old_marker, block_start)
    if os >= 0:
        oe=s.find('\n        if (dashboardView != null && dashboardView !is FrameLayout) {', os)
        if oe >= 0:
            end=s.find('\n        }\n', oe)
            if end >= 0:
                end += len('\n        }\n')
                s=s[:os]+'''        val contentRoot = findViewById<ViewGroup>(android.R.id.content)\n        if (contentRoot.findViewWithTag<View>("REPORT_FLOATING") == null) {\n            val reportButton = Button(this).apply {\n                tag = "REPORT_FLOATING"\n                text = "REPORT"\n                textSize = 11f\n                setTextColor(Color.WHITE)\n                background = GradientDrawable().apply {\n                    setColor(Color.rgb(18, 30, 42))\n                    setStroke(2, Color.rgb(0, 232, 255))\n                    cornerRadius = 100f\n                }\n                elevation = 14f\n                setOnClickListener { startActivity(Intent(this@MainActivity, ReportActivity::class.java)) }\n            }\n            val lp = ViewGroup.MarginLayoutParams(dpReport(92), dpReport(52))\n            lp.leftMargin = dpReport(18); lp.rightMargin = dpReport(18); lp.bottomMargin = dpReport(18)\n            contentRoot.addView(reportButton, lp)\n            reportButton.translationZ = 100f\n            reportButton.post {\n                reportButton.x = (contentRoot.width - reportButton.width - dpReport(18)).toFloat()\n                reportButton.y = (contentRoot.height - reportButton.height - dpReport(18)).toFloat()\n            }\n        }\n'''+s[end:]
if 'private fun dpReport(' not in s:
    marker='    private fun requestBtPermissions() {'
    s=s.replace(marker, '    private fun dpReport(v: Int): Int = (v * resources.displayMetrics.density).toInt()\n\n'+marker, 1)
p.write_text(s)
