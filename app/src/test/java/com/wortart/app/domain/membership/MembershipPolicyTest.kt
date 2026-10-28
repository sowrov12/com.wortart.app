package com.wortart.app.domain.membership

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import kotlin.random.Random

class MembershipPolicyTest {

    private val now = 1_800_000_000_000L // 15 Jan 2027, 08:00 UTC
    private val day = MembershipPolicy.DAY_MILLIS

    private fun profile(
        isPremium: Boolean = false,
        expiry: Long? = null,
        isAdmin: Boolean = false,
    ) = UserProfile(
        uid = "u1",
        email = "me@example.com",
        isPremium = isPremium,
        membershipExpiry = expiry,
        referralCode = "ABCD2345",
        isAdmin = isAdmin,
    )

    // ---- Active membership ----

    @Test
    fun noProfileMeansFreePlan() {
        assertFalse(MembershipPolicy.isActive(null, now))
    }

    @Test
    fun premiumWithFutureExpiryIsActive() {
        assertTrue(MembershipPolicy.isActive(profile(true, now + day), now))
    }

    @Test
    fun premiumWithPastExpiryIsNotActive() {
        assertFalse(MembershipPolicy.isActive(profile(true, now - 1), now))
    }

    @Test
    fun expiryExactlyNowIsNotActive() {
        assertFalse(MembershipPolicy.isActive(profile(true, now), now))
    }

    @Test
    fun futureExpiryWithoutThePremiumFlagIsNotActive() {
        assertFalse(MembershipPolicy.isActive(profile(false, now + day), now))
    }

    @Test
    fun premiumWithoutAnExpiryIsNotActive() {
        assertFalse(MembershipPolicy.isActive(profile(true, null), now))
    }

    // ---- Status text ----

    @Test
    fun statusTextForFreeUsers() {
        assertEquals("No active membership", MembershipPolicy.statusText(null, now, ZoneOffset.UTC))
        assertEquals("No active membership", MembershipPolicy.statusText(profile(), now, ZoneOffset.UTC))
    }

    @Test
    fun statusTextForActiveMembers() {
        val text = MembershipPolicy.statusText(profile(true, now + 8 * day), now, ZoneOffset.UTC)

        assertEquals("Premium member · until 23 Jan 2027 (8 days left)", text)
    }

    @Test
    fun partialDaysCountAsAFullDayLeft() {
        val text = MembershipPolicy.statusText(profile(true, now + day / 2), now, ZoneOffset.UTC)

        assertTrue(text, text.endsWith("(1 day left)"))
    }

    @Test
    fun statusTextForExpiredMembers() {
        val text = MembershipPolicy.statusText(profile(true, now - 2 * day), now, ZoneOffset.UTC)

        assertEquals("Membership expired on 13 Jan 2027", text)
    }

    // ---- Extending a membership ----

    @Test
    fun daysAreCountedFromTodayWhenThereIsNoMembership() {
        assertEquals(now + 7 * day, MembershipPolicy.extendedExpiry(null, now, 7))
    }

    @Test
    fun daysAreCountedFromTodayWhenTheMembershipHasEnded() {
        assertEquals(now + 7 * day, MembershipPolicy.extendedExpiry(now - 30 * day, now, 7))
    }

    @Test
    fun daysAreAddedToARunningMembership() {
        assertEquals(now + 10 * day + 5 * day, MembershipPolicy.extendedExpiry(now + 10 * day, now, 5))
    }

    @Test
    fun referralRewardsAreSevenDaysForTheFriendAndFiveForTheInviter() {
        assertEquals(7, MembershipPolicy.REFERRAL_FRIEND_DAYS)
        assertEquals(5, MembershipPolicy.REFERRAL_INVITER_DAYS)
    }

    @Test
    fun invitationStatusIsReadFromItsStoredValue() {
        assertEquals(InvitationStatus.APPROVED, InvitationStatus.from("approved"))
        assertEquals(InvitationStatus.REJECTED, InvitationStatus.from("rejected"))
        assertEquals(InvitationStatus.PENDING, InvitationStatus.from("pending"))
        assertEquals(InvitationStatus.PENDING, InvitationStatus.from(null))
    }

    // ---- Benefits ----

    @Test
    fun sixMemberBenefitsInTheAgreedOrder() {
        assertEquals(
            listOf(
                "Unlimited daily practice",
                "Unlimited favorites",
                "Unlimited grammar rules",
                "Unlimited pronunciation practice",
                "All categories",
                "Multi-category decks",
            ),
            MemberBenefits.ALL,
        )
    }

    // ---- Referral codes ----

    @Test
    fun generatedCodesAreEightValidCharacters() {
        val random = Random(42)
        repeat(1000) {
            val code = ReferralCodes.generate(random)
            assertEquals(ReferralCodes.LENGTH, code.length)
            assertTrue(code, ReferralCodes.isValid(code))
        }
    }

    @Test
    fun codesAvoidLookAlikeCharacters() {
        val forbidden = "01OI"
        assertTrue(ReferralCodes.ALPHABET.none { it in forbidden })
    }

    @Test
    fun differentRandomSourcesGiveDifferentCodes() {
        val codes = (1..200).map { ReferralCodes.generate(Random(it)) }.toSet()

        assertTrue(codes.size > 195)
    }

    @Test
    fun normalizeTrimsAndUpperCases() {
        assertEquals("ABCD2345", ReferralCodes.normalize("  abcd2345 \n"))
    }

    @Test
    fun invalidCodesAreRejected() {
        assertFalse(ReferralCodes.isValid("ABC"))
        assertFalse(ReferralCodes.isValid("ABCD234O"))
        assertFalse(ReferralCodes.isValid("abcd2345"))
        assertFalse(ReferralCodes.isValid(""))
    }
}
