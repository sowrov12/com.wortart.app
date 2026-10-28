package com.wortart.app.domain.membership

/**
 * A signed-in learner, as stored in the Firestore document `users/{uid}`.
 *
 * @param membershipExpiry end of the membership in epoch milliseconds (a Firestore
 *   Timestamp in the database), or null if the user never had a membership.
 */
data class UserProfile(
    val uid: String,
    val email: String,
    val isPremium: Boolean = false,
    val membershipExpiry: Long? = null,
    val referralCode: String,
    val totalReferrals: Int = 0,
    val isAdmin: Boolean = false,
)

/** What a membership includes, as shown on the Members screen. */
object MemberBenefits {
    val ALL: List<String> = listOf(
        "Unlimited daily practice",
        "Unlimited favorites",
        "Unlimited grammar rules",
        "Unlimited pronunciation practice",
        "All categories",
        "Multi-category decks",
    )
}
