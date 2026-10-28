package com.wortart.app.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.wortart.app.data.auth.resultOf
import com.wortart.app.domain.membership.Invitation
import com.wortart.app.domain.membership.InvitationStatus
import com.wortart.app.domain.membership.MembershipPolicy
import com.wortart.app.domain.membership.PendingInvitation
import com.wortart.app.domain.membership.UserProfile
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestoreAdminRepository(private val firestore: FirebaseFirestore) : AdminRepository {

    private val users = firestore.collection(FirestorePaths.USERS)
    private val invitations = firestore.collection(FirestorePaths.INVITATIONS)

    override suspend fun loadPendingInvitations(): Result<List<PendingInvitation>> = resultOf {
        val snapshot = invitations
            .whereEqualTo("status", InvitationStatus.PENDING.value)
            .get()
            .await()

        // Oldest first; sorted here so no Firestore index is needed.
        snapshot.documents
            .sortedBy { it.getTimestamp("createdAt")?.toDate()?.time ?: 0L }
            .mapNotNull { document ->
                document.toInvitation()?.let { invitation ->
                    val inviter = users.document(invitation.inviterUid).get().await()
                    val referrals = (inviter.getLong("totalReferrals") ?: 0L).toInt()
                    PendingInvitation(invitation, inviterReferralNumber = referrals + 1)
                }
            }
    }

    override suspend fun approve(invitation: Invitation, now: Long): Result<Unit> = resultOf {
        val invitationRef = invitations.document(invitation.friendUid)
        val friendRef = users.document(invitation.friendUid)
        val inviterRef = users.document(invitation.inviterUid)

        firestore.runTransaction { transaction ->
            val invitationSnapshot = transaction.get(invitationRef)
            check(
                invitationSnapshot.exists() &&
                    invitationSnapshot.getString("status") == InvitationStatus.PENDING.value,
            ) { "This invitation was already handled." }

            val friendSnapshot = transaction.get(friendRef)
            val inviterSnapshot = transaction.get(inviterRef)
            check(friendSnapshot.exists()) { "The friend's account no longer exists." }
            check(inviterSnapshot.exists()) { "The inviter's account no longer exists." }

            val friendExpiry = MembershipPolicy.extendedExpiry(
                currentExpiry = friendSnapshot.getTimestamp("membershipExpiry")?.toDate()?.time,
                now = now,
                days = MembershipPolicy.REFERRAL_FRIEND_DAYS,
            )
            val inviterExpiry = MembershipPolicy.extendedExpiry(
                currentExpiry = inviterSnapshot.getTimestamp("membershipExpiry")?.toDate()?.time,
                now = now,
                days = MembershipPolicy.REFERRAL_INVITER_DAYS,
            )

            transaction.update(
                friendRef,
                mapOf(
                    "isPremium" to true,
                    "membershipExpiry" to Timestamp(Date(friendExpiry)),
                ),
            )
            transaction.update(
                inviterRef,
                mapOf(
                    "isPremium" to true,
                    "membershipExpiry" to Timestamp(Date(inviterExpiry)),
                    "totalReferrals" to FieldValue.increment(1),
                ),
            )
            transaction.update(
                invitationRef,
                mapOf(
                    "status" to InvitationStatus.APPROVED.value,
                    "reviewedAt" to Timestamp(Date(now)),
                ),
            )
            Unit
        }.await()
        Unit
    }

    override suspend fun reject(invitation: Invitation, now: Long): Result<Unit> = resultOf {
        val invitationRef = invitations.document(invitation.friendUid)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(invitationRef)
            check(snapshot.exists() && snapshot.getString("status") == InvitationStatus.PENDING.value) {
                "This invitation was already handled."
            }
            transaction.update(
                invitationRef,
                mapOf(
                    "status" to InvitationStatus.REJECTED.value,
                    "reviewedAt" to Timestamp(Date(now)),
                ),
            )
            Unit
        }.await()
        Unit
    }

    override suspend fun findUser(query: String): Result<UserProfile?> = resultOf {
        val text = query.trim()
        check(text.isNotEmpty()) { "Enter a User ID or an email." }

        if ('@' in text) {
            users.whereEqualTo("email", text.lowercase()).limit(1).get().await()
                .documents.firstOrNull()?.toProfile()
        } else {
            users.document(text).get().await().toProfile()
        }
    }

    override suspend fun extendMembership(uid: String, days: Int, now: Long): Result<UserProfile> = resultOf {
        val ref = users.document(uid)
        firestore.runTransaction { transaction ->
            val profile = checkNotNull(transaction.get(ref).toProfile()) { "User not found." }
            val expiry = MembershipPolicy.extendedExpiry(profile.membershipExpiry, now, days)
            transaction.update(
                ref,
                mapOf("isPremium" to true, "membershipExpiry" to Timestamp(Date(expiry))),
            )
            profile.copy(isPremium = true, membershipExpiry = expiry)
        }.await()
    }

    override suspend fun revokeMembership(uid: String, now: Long): Result<UserProfile> = resultOf {
        val ref = users.document(uid)
        firestore.runTransaction { transaction ->
            val profile = checkNotNull(transaction.get(ref).toProfile()) { "User not found." }
            transaction.update(
                ref,
                mapOf("isPremium" to false, "membershipExpiry" to Timestamp(Date(now))),
            )
            profile.copy(isPremium = false, membershipExpiry = now)
        }.await()
    }
}
