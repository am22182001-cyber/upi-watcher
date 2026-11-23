package com.wescreation.upiwatcher

/**
 * Privacy first: only messages that look like "money received" AND contain a
 * rupee amount are ever sent to your site. OTPs, debits, promos and personal
 * chats never leave the phone. (Mirrors the checks the plugin does server-side.)
 */
object Filters {
    private val amountRe = Regex("(?:rs\\.?|inr|₹)\\s*[0-9]", RegexOption.IGNORE_CASE)
    private val debitWords = listOf("debited", "debit of", "withdrawn", "spent", "sent to", "paid to", "purchase of")
    private val creditWords = listOf("credited", "credit of", "received", "deposited", "has been added", "paid you", "sent you")

    fun looksLikeIncomingMoney(text: String): Boolean {
        val t = text.lowercase()
        if (!amountRe.containsMatchIn(t)) return false
        if (debitWords.any { t.contains(it) }) return false
        return creditWords.any { t.contains(it) }
    }

    /** Empty filter = allow every sender. Otherwise sender must contain one of the comma-separated parts. */
    fun senderAllowed(sender: String?, filter: String): Boolean {
        val parts = filter.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return true
        val s = (sender ?: "").lowercase()
        return parts.any { s.contains(it) }
    }

    fun packageSet(csv: String): Set<String> =
        csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}
