package com.wescreation.upiwatcher

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.IOException

class ForwardWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val text = inputData.getString(KEY_TEXT) ?: return Result.failure()
        val source = inputData.getString(KEY_SOURCE) ?: "sms"
        val url = Prefs.url(applicationContext)
        if (url.isBlank()) {
            LogStore.add(applicationContext, source, text, "❌ No webhook URL set")
            return Result.failure()
        }

        return try {
            val r = Net.post(url, mapOf("sms" to text, "source" to source))
            when {
                r.code in 200..299 -> {
                    LogStore.add(applicationContext, source, text, describe(r.body))
                    Result.success()
                }
                r.code in 300..399 -> {
                    LogStore.add(applicationContext, source, text,
                        "❌ Site redirected the request (HTTP ${r.code}). Open the Webhook URL from the plugin settings exactly as shown (check www / https).")
                    Result.failure()
                }
                r.code == 403 -> {
                    LogStore.add(applicationContext, source, text,
                        "❌ Rejected (403): wrong key, or SMS Auto-Detect is switched off in the plugin settings.")
                    Result.failure()
                }
                r.code == 429 || r.code >= 500 -> retryOrGiveUp(source, text, "Server busy (HTTP ${r.code})")
                else -> {
                    LogStore.add(applicationContext, source, text, "❌ HTTP ${r.code}")
                    Result.failure()
                }
            }
        } catch (e: IOException) {
            retryOrGiveUp(source, text, "No connection / timeout")
        } catch (e: Exception) {
            LogStore.add(applicationContext, source, text, "❌ ${e.javaClass.simpleName}: ${e.message}")
            Result.failure()
        }
    }

    private fun retryOrGiveUp(source: String, text: String, why: String): Result {
        return if (runAttemptCount >= 8) {
            LogStore.add(applicationContext, source, text, "❌ Gave up after 8 tries ($why)")
            Result.failure()
        } else {
            Result.retry()
        }
    }

    /** Turns the plugin's JSON reply into a readable line. */
    private fun describe(body: String): String {
        return try {
            val root = JSONObject(body)
            val d = root.optJSONObject("data") ?: root
            when {
                d.optBoolean("matched") -> "✅ Order #${d.optInt("order_id")} approved"
                d.optString("reason") == "ambiguous" ->
                    "⚠️ ${d.optInt("candidates")} orders share this amount – approve manually"
                d.optString("reason") == "near_match" ->
                    "⚠️ Close to order #${d.optInt("order_id")} but amount differs – check manually"
                else -> "ℹ️ No order matched (${d.optString("reason", "no match")})"
            }
        } catch (e: Exception) {
            "ℹ️ Delivered (unexpected reply)"
        }
    }

    companion object {
        const val KEY_TEXT = "text"
        const val KEY_SOURCE = "source"
    }
}
