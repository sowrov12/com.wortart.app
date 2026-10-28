package com.wortart.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wortart.app.data.remote.AdminRepository
import com.wortart.app.data.remote.friendlyDataMessage
import com.wortart.app.domain.membership.MembershipPolicy
import com.wortart.app.domain.membership.PendingInvitation
import com.wortart.app.domain.membership.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminUiState(
    val isLoadingInvitations: Boolean = false,
    val invitations: List<PendingInvitation> = emptyList(),
    /** The text in the "User ID or email" field. */
    val query: String = "",
    val foundUser: UserProfile? = null,
    /** Status line of [foundUser], e.g. "Premium member · until …". */
    val foundUserStatus: String = "",
    /** The last search found nobody. */
    val notFound: Boolean = false,
    /** An approve / reject / search / membership change is running. */
    val isWorking: Boolean = false,
    val message: String? = null,
)

/**
 * The in-app admin panel: approve or reject invitations and enable or extend Premium for
 * a single user. Firestore rules decide who may do this; the screen only hides the entry
 * point from non-admins.
 */
class AdminViewModel(
    private val admin: AdminRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val state = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = state.asStateFlow()

    /** Loads the pending invitations. Called when the panel opens. */
    fun refresh() {
        viewModelScope.launch {
            state.update { it.copy(isLoadingInvitations = true) }
            reloadInvitations()
            state.update { it.copy(isLoadingInvitations = false) }
        }
    }

    fun approve(pending: PendingInvitation) = work {
        val invitation = pending.invitation
        admin.approve(invitation, clock())
            .onSuccess {
                notify(
                    "Approved: ${invitation.friendEmail} +${MembershipPolicy.REFERRAL_FRIEND_DAYS} days, " +
                        "${invitation.inviterEmail} +${MembershipPolicy.REFERRAL_INVITER_DAYS} days.",
                )
            }
            .onFailure { notify(friendlyDataMessage(it)) }
        reloadInvitations()
    }

    fun reject(pending: PendingInvitation) = work {
        admin.reject(pending.invitation, clock())
            .onSuccess { notify("Rejected the invitation from ${pending.invitation.friendEmail}.") }
            .onFailure { notify(friendlyDataMessage(it)) }
        reloadInvitations()
    }

    fun onQueryChange(text: String) {
        state.update { it.copy(query = text, foundUser = null, foundUserStatus = "", notFound = false) }
    }

    fun findUser() {
        val text = state.value.query.trim()
        if (text.isEmpty()) return
        work {
            admin.findUser(text)
                .onSuccess { profile -> showUser(profile, notFound = profile == null) }
                .onFailure { error ->
                    showUser(null, notFound = false)
                    notify(friendlyDataMessage(error))
                }
        }
    }

    /** Enables Premium for the found user, or adds [days] to a running membership. */
    fun extendMembership(days: Int) {
        val user = state.value.foundUser ?: return
        work {
            admin.extendMembership(user.uid, days, clock())
                .onSuccess { updated ->
                    showUser(updated, notFound = false)
                    notify("Added $days days for ${updated.email}.")
                }
                .onFailure { notify(friendlyDataMessage(it)) }
        }
    }

    fun revokeMembership() {
        val user = state.value.foundUser ?: return
        work {
            admin.revokeMembership(user.uid, clock())
                .onSuccess { updated ->
                    showUser(updated, notFound = false)
                    notify("Ended the membership of ${updated.email}.")
                }
                .onFailure { notify(friendlyDataMessage(it)) }
        }
    }

    fun dismissMessage() {
        state.update { it.copy(message = null) }
    }

    private suspend fun reloadInvitations() {
        admin.loadPendingInvitations()
            .onSuccess { list -> state.update { it.copy(invitations = list) } }
            .onFailure { notify(friendlyDataMessage(it)) }
    }

    private fun showUser(profile: UserProfile?, notFound: Boolean) {
        state.update {
            it.copy(
                foundUser = profile,
                foundUserStatus = profile?.let { user -> MembershipPolicy.statusText(user, clock()) }.orEmpty(),
                notFound = notFound,
            )
        }
    }

    private fun notify(text: String) {
        state.update { it.copy(message = text) }
    }

    private fun work(block: suspend () -> Unit) {
        viewModelScope.launch {
            state.update { it.copy(isWorking = true) }
            try {
                block()
            } finally {
                state.update { it.copy(isWorking = false) }
            }
        }
    }

    companion object {
        fun factory(admin: AdminRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AdminViewModel(admin) }
        }
    }
}
