package com.wortart.app.ui.members

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wortart.app.data.auth.AuthRepository
import com.wortart.app.data.auth.AuthUser
import com.wortart.app.data.auth.friendlyAuthMessage
import com.wortart.app.data.membership.MembershipRepository
import com.wortart.app.data.remote.InvitationRepository
import com.wortart.app.data.remote.UserRepository
import com.wortart.app.data.remote.friendlyDataMessage
import com.wortart.app.domain.membership.Invitation
import com.wortart.app.domain.membership.InvitationStatus
import com.wortart.app.domain.membership.MembershipPolicy
import com.wortart.app.domain.membership.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MembersUiState(
    val isConfigured: Boolean = true,
    val user: AuthUser? = null,
    val profile: UserProfile? = null,
    /** The invite code this person entered, if any. */
    val invitation: Invitation? = null,
    /** Signed in, waiting for the profile from Firestore. */
    val isLoadingProfile: Boolean = false,
    val isMemberActive: Boolean = false,
    val statusText: String = "No active membership",
    /** A sign-in, sign-out, invite or delete action is running. */
    val isBusy: Boolean = false,
    /** A short notice or error for the learner. */
    val message: String? = null,
) {
    val isSignedIn: Boolean get() = user != null
    val isAdmin: Boolean get() = profile?.isAdmin == true
}

/** Drives the Members tab: sign-in, account status, invite codes and account deletion. */
@OptIn(ExperimentalCoroutinesApi::class)
class MembersViewModel(
    private val membership: MembershipRepository,
    private val auth: AuthRepository,
    private val users: UserRepository,
    private val invitations: InvitationRepository,
    /** The number of flashcards answered on this device so far. */
    private val cardsPlayed: suspend () -> Int,
    private val resetLocalData: suspend () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val myInvitation: Flow<Invitation?> = membership.session
        .map { it.user?.uid }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid == null) flowOf(null) else invitations.observeMyInvitation(uid)
        }

    val uiState: StateFlow<MembersUiState> =
        combine(membership.session, myInvitation, busy, message) { session, invitation, isBusy, notice ->
            val now = clock()
            MembersUiState(
                isConfigured = session.isConfigured,
                user = session.user,
                profile = session.profile,
                invitation = invitation,
                isLoadingProfile = session.isLoadingProfile,
                isMemberActive = MembershipPolicy.isActive(session.profile, now),
                statusText = MembershipPolicy.statusText(session.profile, now),
                isBusy = isBusy,
                message = notice ?: session.error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MembersUiState(isConfigured = membership.session.value.isConfigured),
        )

    fun signIn(context: Context) {
        viewModelScope.launch {
            busy.value = true
            auth.signInWithGoogle(context).onFailure { message.value = friendlyAuthMessage(it) }
            busy.value = false
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            busy.value = true
            auth.signOut(context)
            busy.value = false
        }
    }

    /** Enters a friend's invite code. The admin then approves it. */
    fun applyInviteCode(code: String) {
        val user = membership.session.value.user ?: return
        viewModelScope.launch {
            busy.value = true
            invitations.applyCode(user, code, cardsPlayed())
                .onFailure { message.value = friendlyDataMessage(it) }
            busy.value = false
        }
    }

    /** After a rejection: removes the old invitation so another code can be entered. */
    fun tryAnotherCode() {
        val user = membership.session.value.user ?: return
        viewModelScope.launch {
            invitations.discardMyInvitation(user.uid)
                .onFailure { message.value = friendlyDataMessage(it) }
        }
    }

    /**
     * Tells the admin how many cards this person answered, while the invitation is pending.
     * Called when the Members tab opens.
     */
    fun refreshInvitationProgress() {
        val user = membership.session.value.user ?: return
        viewModelScope.launch {
            val invitation = myInvitation.first()
            if (invitation?.status == InvitationStatus.PENDING) {
                invitations.reportProgress(user.uid, cardsPlayed())
            }
        }
    }

    /**
     * Deletes the account for good (Google Play requirement):
     * 1. the person confirms their Google account again,
     * 2. the invitation, the Firestore profile and the referral code are deleted,
     * 3. the Firebase Auth account is deleted,
     * 4. local progress and favorites on this device are reset.
     */
    fun deleteAccount(context: Context) {
        viewModelScope.launch {
            val profile = membership.session.value.profile ?: return@launch
            busy.value = true
            try {
                auth.reauthenticate(context).onFailure {
                    message.value = friendlyAuthMessage(it)
                    return@launch
                }
                invitations.discardMyInvitation(profile.uid).onFailure {
                    message.value = "Could not delete your data. Please try again."
                    return@launch
                }
                users.deleteProfile(profile).onFailure {
                    message.value = "Could not delete your data. Please try again."
                    return@launch
                }
                auth.deleteCurrentUser().onFailure {
                    message.value = "Your data was deleted, but the sign-in account could not be " +
                        "removed. Please sign in again and retry."
                    return@launch
                }
                resetLocalData()
                message.value = "Your account was deleted."
            } finally {
                busy.value = false
            }
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    companion object {
        fun factory(
            membership: MembershipRepository,
            auth: AuthRepository,
            users: UserRepository,
            invitations: InvitationRepository,
            cardsPlayed: suspend () -> Int,
            resetLocalData: suspend () -> Unit,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MembersViewModel(membership, auth, users, invitations, cardsPlayed, resetLocalData)
            }
        }
    }
}
