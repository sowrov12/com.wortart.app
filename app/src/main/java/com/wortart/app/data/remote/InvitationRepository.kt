package com.wortart.app.data.remote

import com.wortart.app.data.auth.AuthUser
import com.wortart.app.domain.membership.Invitation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The friend's side of the referral system: entering an invite code. */
interface InvitationRepository {

    /** The invitation created by [friendUid] (null if no code was entered). */
    fun observeMyInvitation(friendUid: String): Flow<Invitation?>

    /**
     * Enters an invite code. Fails with a readable message if the code is invalid, unknown,
     * the person's own, or if a code was already used.
     */
    suspend fun applyCode(friend: AuthUser, rawCode: String, cardsPlayed: Int): Result<Unit>

    /** Removes the person's own invitation (to enter a different code after a rejection). */
    suspend fun discardMyInvitation(friendUid: String): Result<Unit>

    /** Tells the admin how many cards the friend answered so far (pending invitations only). */
    suspend fun reportProgress(friendUid: String, cardsPlayed: Int): Result<Unit>
}

object UnconfiguredInvitationRepository : InvitationRepository {
    private fun notConfigured() =
        Result.failure<Unit>(IllegalStateException("Accounts are not set up yet."))

    override fun observeMyInvitation(friendUid: String): Flow<Invitation?> = flowOf(null)
    override suspend fun applyCode(friend: AuthUser, rawCode: String, cardsPlayed: Int): Result<Unit> = notConfigured()
    override suspend fun discardMyInvitation(friendUid: String): Result<Unit> = notConfigured()
    override suspend fun reportProgress(friendUid: String, cardsPlayed: Int): Result<Unit> = notConfigured()
}
