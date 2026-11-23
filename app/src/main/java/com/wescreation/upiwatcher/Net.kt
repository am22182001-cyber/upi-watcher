package com.wescreation.upiwatcher

import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Net {
    data class Resp(val code: Int, val body: String, val viaGet: Boolean = false)

    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

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

    /**
     * Sends the fields as a POST. Some hosts' firewalls answer app POSTs with
     * 403/404 while a normal browser-style GET passes — in that case retry once as GET.
     */
    fun post(url: String, fields: Map<String, String>): Resp {
        val r = request("POST", url, fields)
        if (r.code in listOf(403, 404, 405, 406, 415, 501)) {
            val g = request("GET", withQuery(url, fields), emptyMap())
            if (g.code in 200..299) return g.copy(viaGet = true)
        }
        return r
    }

    private fun withQuery(url: String, fields: Map<String, String>): String {
        val b = Uri.parse(url).buildUpon()
        for ((k, v) in fields) b.appendQueryParameter(k, v)
        return b.build().toString()
    }

    private fun request(method: String, url: String, fields: Map<String, String>): Resp {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            // A redirect would turn the POST into a GET and lose the SMS text.
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", UA)
            conn.setRequestProperty("Accept", "application/json, text/plain, */*")
            if (method == "POST") {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                val body = fields.entries.joinToString("&") {
                    URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
                }
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..399) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return Resp(code, text)
        } finally {
            conn.disconnect()
        }
    }
}
