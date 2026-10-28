package com.wortart.app.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.wortart.app.data.auth.resultOf
import com.wortart.app.domain.membership.ReferralCodes
import com.wortart.app.domain.membership.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Collection names used in Firestore (see firebase/firestore.rules). */
object FirestorePaths {
    const val USERS = "users"
    const val REFERRAL_CODES = "referralCodes"
    const val INVITATIONS = "invitations"
}

class FirestoreUserRepository(private val firestore: FirebaseFirestore) : UserRepository {

    private val users = firestore.collection(FirestorePaths.USERS)
    private val codes = firestore.collection(FirestorePaths.REFERRAL_CODES)

    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = users.document(uid).addSnapshotListener { snapshot, error ->
            // After sign-out or deletion the listener is denied; treat that as "no profile".
            if (error != null) {
                trySend(null)
            } else {
                trySend(snapshot?.toProfile())
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun ensureProfile(uid: String, email: String): Result<UserProfile> = resultOf {
        val userRef = users.document(uid)
        // A transaction makes sure two callers can never create two different referral codes.
        firestore.runTransaction { transaction ->
            transaction.get(userRef).toProfile() ?: run {
                val profile = UserProfile(
                    uid = uid,
                    email = email,
                    referralCode = ReferralCodes.generate(),
                )
                transaction.set(userRef, profile.toNewDocument())
                transaction.set(
                    codes.document(profile.referralCode),
                    mapOf("uid" to uid, "email" to email),
                )
                profile
            }
        }.await()
    }

    override suspend fun deleteProfile(profile: UserProfile): Result<Unit> = resultOf {
        val batch = firestore.batch()
        batch.delete(users.document(profile.uid))
        if (profile.referralCode.isNotBlank()) {
            batch.delete(codes.document(profile.referralCode))
        }
        batch.commit().await()
        Unit
    }
}

internal fun DocumentSnapshot.toProfile(): UserProfile? {
    if (!exists()) return null
    return UserProfile(
        uid = getString("uid") ?: id,
        email = getString("email").orEmpty(),
        isPremium = getBoolean("isPremium") ?: false,
        membershipExpiry = getTimestamp("membershipExpiry")?.toDate()?.time,
        referralCode = getString("referralCode").orEmpty(),
        totalReferrals = (getLong("totalReferrals") ?: 0L).toInt(),
        isAdmin = getBoolean("isAdmin") ?: false,
    )
}

/** Field values for a brand-new profile (never premium, never admin). */
internal fun UserProfile.toNewDocument(): Map<String, Any> = buildMap {
    put("uid", uid)
    put("email", email)
    put("isPremium", isPremium)
    put("referralCode", referralCode)
    put("totalReferrals", totalReferrals)
    put("isAdmin", isAdmin)
    membershipExpiry?.let { put("membershipExpiry", Timestamp(Date(it))) }
}
