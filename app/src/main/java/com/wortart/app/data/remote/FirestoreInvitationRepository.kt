package com.wortart.app.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.wortart.app.data.auth.AuthUser
import com.wortart.app.data.auth.resultOf
import com.wortart.app.domain.membership.Invitation
import com.wortart.app.domain.membership.InvitationStatus
import com.wortart.app.domain.membership.ReferralCodes
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreInvitationRepository(firestore: FirebaseFirestore) : InvitationRepository {

    private val invitations = firestore.collection(FirestorePaths.INVITATIONS)
    private val codes = firestore.collection(FirestorePaths.REFERRAL_CODES)

    override fun observeMyInvitation(friendUid: String): Flow<Invitation?> = callbackFlow {
        val registration = invitations.document(friendUid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
            } else {
                trySend(snapshot?.toInvitation())
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun applyCode(friend: AuthUser, rawCode: String, cardsPlayed: Int): Result<Unit> =
        resultOf {
            val code = ReferralCodes.normalize(rawCode)
            check(ReferralCodes.isValid(code)) {
                "That is not a valid invite code. Codes have 8 letters and numbers."
            }

            val myInvitation = invitations.document(friend.uid)
            check(!myInvitation.get().await().exists()) { "You already used an invite code." }

            val owner = codes.document(code).get().await()
            check(owner.exists()) { "We could not find that code." }
            val inviterUid = owner.getString("uid").orEmpty()
            check(inviterUid != friend.uid) { "You cannot use your own invite code." }

            myInvitation.set(
                mapOf(
                    "friendUid" to friend.uid,
                    "friendEmail" to friend.email.orEmpty(),
                    "inviterUid" to inviterUid,
                    "inviterEmail" to owner.getString("email").orEmpty(),
                    "referralCode" to code,
                    "status" to InvitationStatus.PENDING.value,
                    "friendCardsPlayed" to cardsPlayed,
                    "createdAt" to Timestamp.now(),
                ),
            ).await()
            Unit
        }

    override suspend fun discardMyInvitation(friendUid: String): Result<Unit> = resultOf {
        invitations.document(friendUid).delete().await()
        Unit
    }

    override suspend fun reportProgress(friendUid: String, cardsPlayed: Int): Result<Unit> = resultOf {
        invitations.document(friendUid).update("friendCardsPlayed", cardsPlayed).await()
        Unit
    }
}

internal fun DocumentSnapshot.toInvitation(): Invitation? {
    if (!exists()) return null
    return Invitation(
        friendUid = getString("friendUid") ?: id,
        friendEmail = getString("friendEmail").orEmpty(),
        inviterUid = getString("inviterUid").orEmpty(),
        inviterEmail = getString("inviterEmail").orEmpty(),
        referralCode = getString("referralCode").orEmpty(),
        status = InvitationStatus.from(getString("status")),
        friendCardsPlayed = (getLong("friendCardsPlayed") ?: 0L).toInt(),
    )
}
