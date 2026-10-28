package com.wortart.app.data.remote

import com.wortart.app.domain.membership.Invitation
import com.wortart.app.domain.membership.PendingInvitation
import com.wortart.app.domain.membership.UserProfile

/** Actions of the in-app admin panel. Firestore rules only allow them for admins. */
interface AdminRepository {

    suspend fun loadPendingInvitations(): Result<List<PendingInvitation>>

    /**
     * Approves an invitation: the friend gets 7 days, the inviter 5 days and one more referral.
     * Everything happens in one transaction, and an invitation can only be approved once.
     */
    suspend fun approve(invitation: Invitation, now: Long): Result<Unit>

    suspend fun reject(invitation: Invitation, now: Long): Result<Unit>

    /** Finds a user by User ID, or by email when the text contains "@". Null = not found. */
    suspend fun findUser(query: String): Result<UserProfile?>

    /** Enables Premium or adds [days] to a running membership. Returns the updated profile. */
    suspend fun extendMembership(uid: String, days: Int, now: Long): Result<UserProfile>

    /** Ends the membership immediately. Returns the updated profile. */
    suspend fun revokeMembership(uid: String, now: Long): Result<UserProfile>
}

object UnconfiguredAdminRepository : AdminRepository {
    private fun <T> notConfigured() =
        Result.failure<T>(IllegalStateException("Accounts are not set up yet."))

    override suspend fun loadPendingInvitations(): Result<List<PendingInvitation>> = notConfigured()
    override suspend fun approve(invitation: Invitation, now: Long): Result<Unit> = notConfigured()
    override suspend fun reject(invitation: Invitation, now: Long): Result<Unit> = notConfigured()
    override suspend fun findUser(query: String): Result<UserProfile?> = notConfigured()
    override suspend fun extendMembership(uid: String, days: Int, now: Long): Result<UserProfile> = notConfigured()
    override suspend fun revokeMembership(uid: String, now: Long): Result<UserProfile> = notConfigured()
}
