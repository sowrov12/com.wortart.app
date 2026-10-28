package com.wortart.app.domain.membership

enum class InvitationStatus(val value: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    ;

    companion object {
        fun from(value: String?): InvitationStatus = entries.firstOrNull { it.value == value } ?: PENDING
    }
}

/** A friend who entered someone's invite code, stored as `invitations/{friendUid}`. */
data class Invitation(
    val friendUid: String,
    val friendEmail: String,
    val inviterUid: String,
    val inviterEmail: String,
    val referralCode: String,
    val status: InvitationStatus = InvitationStatus.PENDING,
    /** How many flashcards the friend answered, as last reported by the friend's app. */
    val friendCardsPlayed: Int = 0,
)

/** An invitation waiting for the admin, with the number this referral would be for the inviter. */
data class PendingInvitation(
    val invitation: Invitation,
    val inviterReferralNumber: Int,
)
