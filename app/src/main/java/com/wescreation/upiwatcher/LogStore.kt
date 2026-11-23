package com.wescreation.upiwatcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Last 60 forwarding results, stored locally so you can see what happened. */
object LogStore {
    private const val KEY = "log"
    private const val MAX = 60

    @Synchronized
    fun add(c: Context, source: String, preview: String, outcome: String) {
        val sp = Prefs.raw(c)
        val arr = try { JSONArray(sp.getString(KEY, "[]")) } catch (e: Exception) { JSONArray() }
        val o = JSONObject()
            .put("t", System.currentTimeMillis())
            .put("s", source)
            .put("p", preview.replace('\n', ' ').take(90))
            .put("o", outcome)
        val out = JSONArray()
        out.put(o)
        for (i in 0 until minOf(arr.length(), MAX - 1)) out.put(arr.get(i))
        sp.edit().putString(KEY, out.toString()).apply()
    }

    fun clear(c: Context) { Prefs.raw(c).edit().remove(KEY).apply() }

    fun render(c: Context): String {
        val arr = try { JSONArray(Prefs.raw(c).getString(KEY, "[]")) } catch (e: Exception) { JSONArray() }
        if (arr.length() == 0) return "Nothing forwarded yet."
        val fmt = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            sb.append(fmt.format(Date(o.getLong("t")))).append("  [").append(o.getString("s")).append("]\n")
            sb.append(o.getString("o")).append('\n')
            sb.append("“").append(o.getString("p")).append("…”\n\n")
        }
        return sb.toString().trim()
    }
}
