package com.wortart.app.data.remote

import com.wortart.app.domain.membership.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Reads and writes the user profile in Firestore. */
interface UserRepository {

    /** Live profile of [uid]; emits null while the document does not exist. */
    fun observeProfile(uid: String): Flow<UserProfile?>

    /** Returns the profile of [uid], creating it (with a new referral code) on first sign-in. */
    suspend fun ensureProfile(uid: String, email: String): Result<UserProfile>

    /** Deletes the profile and its referral code document. */
    suspend fun deleteProfile(profile: UserProfile): Result<Unit>
}

/** Used when Firebase is not set up. */
object UnconfiguredUserRepository : UserRepository {
    override fun observeProfile(uid: String): Flow<UserProfile?> = flowOf(null)

    override suspend fun ensureProfile(uid: String, email: String): Result<UserProfile> =
        Result.failure(IllegalStateException("Accounts are not set up yet."))

    override suspend fun deleteProfile(profile: UserProfile): Result<Unit> =
        Result.failure(IllegalStateException("Accounts are not set up yet."))
}
