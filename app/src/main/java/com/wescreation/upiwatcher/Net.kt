package com.wescreation.upiwatcher

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Net {
    data class Resp(val code: Int, val body: String)

    /** Webhook URL must be https and carry the secret key. */
    fun urlProblem(url: String): String? {
        val u = Uri.parse(url.trim())
        if (u.scheme != "https") return "URL must start with https://"
        if (u.host.isNullOrEmpty()) return "URL has no website address"
        if (u.getQueryParameter("key").isNullOrEmpty()) return "URL is missing the secret key (…&key=…)"
        return null
    }

    /** Hide the secret key when showing the URL on screen / in the log. */
    fun maskUrl(url: String): String = url.replace(Regex("(key=)[^&]+"), "$1••••••")

    fun post(url: String, fields: Map<String, String>): Resp {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            conn.doOutput = true
            // A redirect would turn the POST into a GET and lose the SMS text.
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            conn.setRequestProperty("User-Agent", "WesUpiWatcher/1.0")
            val body = fields.entries.joinToString("&") {
                URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..399) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return Resp(code, text)
        } finally {
            conn.disconnect()
        }
    }
}
