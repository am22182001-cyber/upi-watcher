package com.wescreation.upiwatcher

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var etUrl: TextInputEditText
    private lateinit var etSenders: TextInputEditText
    private lateinit var etPackages: TextInputEditText
    private lateinit var swSms: SwitchMaterial
    private lateinit var swNotif: SwitchMaterial
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val refresher = object : Runnable {
        override fun run() {
            refreshStatus()
            handler.postDelayed(this, 2500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUrl = findViewById(R.id.etUrl)
        etSenders = findViewById(R.id.etSenders)
        etPackages = findViewById(R.id.etPackages)
        swSms = findViewById(R.id.swSms)
        swNotif = findViewById(R.id.swNotif)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)

        etUrl.setText(Prefs.url(this))
        etSenders.setText(Prefs.senders(this))
        etPackages.setText(Prefs.packages(this))
        swSms.isChecked = Prefs.smsEnabled(this)
        swNotif.isChecked = Prefs.notifEnabled(this)

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { save(true) }

        findViewById<MaterialButton>(R.id.btnPaste).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
            if (text.isNullOrEmpty()) toast("Clipboard is empty") else etUrl.setText(text)
        }

        findViewById<MaterialButton>(R.id.btnTest).setOnClickListener { sendTest() }

        findViewById<MaterialButton>(R.id.btnSmsPerm).setOnClickListener {
            if (hasSmsPermission()) toast("SMS access already allowed")
            else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 11)
        }

        findViewById<MaterialButton>(R.id.btnNotifAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<MaterialButton>(R.id.btnBattery).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            }
        }

        findViewById<MaterialButton>(R.id.btnClearLog).setOnClickListener {
            LogStore.clear(this)
            refreshStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresher)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refresher)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        refreshStatus()
    }

    private fun hasSmsPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    private fun notifAccessOn() =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    private fun batteryOk(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    /** Returns false (and shows why) if the URL is not usable. */
    private fun save(showToast: Boolean): Boolean {
        val url = etUrl.text?.toString()?.trim() ?: ""
        val problem = Net.urlProblem(url)
        if (problem != null) {
            toast(problem)
            return false
        }
        Prefs.save(
            this, url,
            etSenders.text?.toString() ?: "",
            etPackages.text?.toString() ?: Prefs.DEFAULT_PACKAGES,
            swSms.isChecked, swNotif.isChecked
        )
        if (showToast) toast("Saved")
        refreshStatus()
        return true
    }

    private fun sendTest() {
        if (!save(false)) return
        val url = Prefs.url(this)
        toast("Testing…")
        Thread {
            val msg = try {
                val r = Net.post(url, mapOf("ping" to "1", "source" to "app"))
                when {
                    r.code in 200..299 -> {
                        val site = try {
                            val root = JSONObject(r.body)
                            (root.optJSONObject("data") ?: root).optString("site")
                        } catch (e: Exception) { "" }
                        "✅ Connected" + if (site.isNotEmpty()) " to “$site”" else ""
                    }
                    r.code == 403 -> "❌ Rejected: wrong key, or turn ON “Enable SMS Auto-Detect” in the plugin settings"
                    r.code in 300..399 -> "❌ Site redirected the request – use the URL exactly as shown in the plugin"
                    else -> "❌ Server answered HTTP ${r.code}"
                }
            } catch (e: Exception) {
                "❌ Could not reach the site: ${e.message}"
            }
            runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
        }.start()
    }

    private fun refreshStatus() {
        val url = Prefs.url(this)
        val sb = StringBuilder()
        sb.append(if (url.isBlank()) "⚠️ Webhook: not set\n" else "✅ Webhook: ${Net.maskUrl(url)}\n")
        sb.append(if (hasSmsPermission()) "✅ SMS access: allowed\n" else "⚠️ SMS access: not allowed yet\n")
        sb.append(
            if (!swNotif.isChecked) "• App notifications: off\n"
            else if (notifAccessOn()) "✅ Notification access: on\n"
            else "⚠️ Notification access: not granted\n"
        )
        sb.append(if (batteryOk()) "✅ Battery optimisation: off (good)" else "⚠️ Battery optimisation: on – may delay alerts")
        tvStatus.text = sb.toString()
        tvLog.text = LogStore.render(this)
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
