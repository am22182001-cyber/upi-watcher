package com.wescreation.upiwatcher

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object Forwarder {
    /** Queue a message for delivery. Survives reboots; retries automatically when offline. */
    fun enqueue(c: Context, source: String, text: String) {
        if (Prefs.url(c).isBlank()) return
        val clean = text.take(1500)
        val hash = sha(source + clean).take(16)

        // Same message twice within 2 minutes (e.g. a notification updating itself) -> once.
        val sp = Prefs.raw(c)
        val now = System.currentTimeMillis()
        if (now - sp.getLong("seen_$hash", 0L) < 120_000L) return
        sp.edit().putLong("seen_$hash", now).apply()

        val req = OneTimeWorkRequestBuilder<ForwardWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                Data.Builder()
                    .putString(ForwardWorker.KEY_TEXT, clean)
                    .putString(ForwardWorker.KEY_SOURCE, source)
                    .build()
            )
            .build()
        WorkManager.getInstance(c).enqueueUniqueWork("fwd_$hash", ExistingWorkPolicy.KEEP, req)
    }

    private fun sha(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
