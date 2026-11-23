package com.wescreation.upiwatcher

import android.content.Context
import android.content.SharedPreferences

/** Small wrapper around private SharedPreferences (app sandbox only, no backup). */
object Prefs {
    const val DEFAULT_PACKAGES =
        "com.phonepe.app,com.google.android.apps.nbu.paisa.user,net.one97.paytm,in.org.npci.upiapp"

    private fun sp(c: Context): SharedPreferences =
        c.getSharedPreferences("wcupi_watcher", Context.MODE_PRIVATE)

    fun url(c: Context): String = sp(c).getString("url", "") ?: ""
    fun senders(c: Context): String = sp(c).getString("senders", "") ?: ""
    fun packages(c: Context): String = sp(c).getString("packages", DEFAULT_PACKAGES) ?: DEFAULT_PACKAGES
    fun smsEnabled(c: Context): Boolean = sp(c).getBoolean("sms_on", true)
    fun notifEnabled(c: Context): Boolean = sp(c).getBoolean("notif_on", false)

    fun save(c: Context, url: String, senders: String, packages: String, sms: Boolean, notif: Boolean) {
        sp(c).edit()
            .putString("url", url.trim())
            .putString("senders", senders.trim())
            .putString("packages", packages.trim())
            .putBoolean("sms_on", sms)
            .putBoolean("notif_on", notif)
            .apply()
    }

    fun raw(c: Context): SharedPreferences = sp(c)
}
