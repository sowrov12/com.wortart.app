package com.wortart.app.data.membership

import com.wortart.app.data.auth.AuthRepository
import com.wortart.app.data.auth.AuthUser
import com.wortart.app.data.remote.UserRepository
import com.wortart.app.domain.membership.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Who is signed in and what their profile says.
 *
 * @param error set when the profile could not be loaded or created.
 */
data class MembershipSession(
    val isConfigured: Boolean,
    val user: AuthUser? = null,
    val profile: UserProfile? = null,
    val error: String? = null,
) {
    val isSignedIn: Boolean get() = user != null

    /** Signed in, but the profile has not arrived yet. */
    val isLoadingProfile: Boolean get() = user != null && profile == null && error == null
}

/**
 * One shared, always up-to-date view of the signed-in user and their Firestore profile.
 * Screens and access checks all read [session], so the profile is created and listened to
 * only once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MembershipRepository(
    private val auth: AuthRepository,
    private val users: UserRepository,
    scope: CoroutineScope,
) {

    val session: StateFlow<MembershipSession> = auth.currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(MembershipSession(isConfigured = auth.isConfigured))
            } else {
                flow {
                    val ensured = users.ensureProfile(user.uid, user.email.orEmpty())
                    if (ensured.isFailure) {
                        emit(
                            MembershipSession(
                                isConfigured = auth.isConfigured,
                                user = user,
                                error = ensured.exceptionOrNull()?.message
                                    ?: "Could not load your profile.",
                            ),
                        )
                    } else {
                        emitAll(
                            users.observeProfile(user.uid).map { profile ->
                                MembershipSession(auth.isConfigured, user, profile)
                            },
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MembershipSession(isConfigured = auth.isConfigured),
        )
}
