package com.wescreation.upiwatcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Prefs.smsEnabled(context)) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Long SMS arrive in several parts – glue them back together per sender.
        val bySender = LinkedHashMap<String, StringBuilder>()
        for (p in parts) {
            bySender.getOrPut(p.originatingAddress ?: "?") { StringBuilder() }
                .append(p.messageBody ?: "")
        }
        val filter = Prefs.senders(context)
        for ((sender, sb) in bySender) {
            val text = sb.toString()
            if (!Filters.senderAllowed(sender, filter)) continue
            if (!Filters.looksLikeIncomingMoney(text)) continue
            Forwarder.enqueue(context, "sms", text)
        }
    }
}
