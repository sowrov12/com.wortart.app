package com.wortart.app.domain.membership

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

/** Rules that decide who is a member and how the membership is described. */
object MembershipPolicy {

    const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    /** Free days an approved invitation gives. */
    const val REFERRAL_FRIEND_DAYS = 7
    const val REFERRAL_INVITER_DAYS = 5

    /**
     * A membership is active when `isPremium` is true and the expiry date is still in the
     * future. Everything else is the free plan.
     */
    fun isActive(profile: UserProfile?, now: Long): Boolean {
        if (profile == null || !profile.isPremium) return false
        val expiry = profile.membershipExpiry ?: return false
        return expiry > now
    }

    /** One line for the Members screen, e.g. "Premium member · until 3 Nov 2026 (8 days left)". */
    fun statusText(profile: UserProfile?, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val expiry = profile?.membershipExpiry
        return when {
            isActive(profile, now) -> {
                val millisLeft = expiry!! - now
                val daysLeft = (millisLeft + DAY_MILLIS - 1) / DAY_MILLIS
                val dayWord = if (daysLeft == 1L) "day" else "days"
                "Premium member · until ${formatDate(expiry, zone)} ($daysLeft $dayWord left)"
            }
            expiry != null -> "Membership expired on ${formatDate(expiry, zone)}"
            else -> "No active membership"
        }
    }

    /**
     * The new expiry when [days] are added: counted from today, or from the current expiry when
     * the membership is still running (so days are never lost).
     */
    fun extendedExpiry(currentExpiry: Long?, now: Long, days: Int): Long =
        maxOf(currentExpiry ?: 0L, now) + days * DAY_MILLIS

    private fun formatDate(epochMillis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
}

/** Invite codes: 8 characters without look-alikes (no 0/O, 1/I). */
object ReferralCodes {

    const val LENGTH = 8
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(random: Random = Random.Default): String =
        buildString(LENGTH) { repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /** Upper-cases and trims what a person typed. */
    fun normalize(input: String): String = input.trim().uppercase(Locale.ROOT)

    fun isValid(code: String): Boolean = code.length == LENGTH && code.all { it in ALPHABET }
}
