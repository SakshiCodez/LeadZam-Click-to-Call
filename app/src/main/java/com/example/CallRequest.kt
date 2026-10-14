package com.example

import java.util.UUID

/**
 * Represents an incoming click-to-call request from the CRM dashboard.
 */
data class CallRequest(
    val phoneNumber: String,
    val contactName: String = "",
    val requestId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "LeadZam CRM"
) {
    companion object {
        /**
         * Validates phone number format.
         * Ensures the number is non-empty and contains at least 7 digits,
         * with optional leading '+' and standard formatting separators.
         */
        fun isValidPhoneNumber(number: String?): Boolean {
            if (number.isNullOrBlank()) return false
            val digitsOnly = number.filter { it.isDigit() }
            if (digitsOnly.length < 7 || digitsOnly.length > 15) return false
            // Regex permits optional '+', digits, spaces, hyphens, and parentheses
            val phoneRegex = Regex("^\\+?[0-9\\s\\-\\(\\)]{7,25}\$")
            return phoneRegex.matches(number.trim())
        }

        /**
         * Cleans the phone number for the tel: URI scheme,
         * keeping only the leading '+' (if present) and digits.
         */
        fun sanitizeForDialer(number: String): String {
            val trimmed = number.trim()
            val hasPlus = trimmed.startsWith("+")
            val digits = trimmed.filter { it.isDigit() }
            return if (hasPlus) "+$digits" else digits
        }
    }
}
