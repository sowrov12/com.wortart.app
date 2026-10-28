package com.wortart.app.ui.admin

import com.wortart.app.data.remote.AdminRepository
import com.wortart.app.domain.membership.Invitation
import com.wortart.app.domain.membership.MembershipPolicy
import com.wortart.app.domain.membership.PendingInvitation
import com.wortart.app.domain.membership.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val now = 1_800_000_000_000L
    private val day = MembershipPolicy.DAY_MILLIS

    private class FakeAdmin : AdminRepository {
        val calls = mutableListOf<String>()
        var pending: List<PendingInvitation> = emptyList()
        var loadResult: Result<List<PendingInvitation>>? = null
        var approveResult: Result<Unit> = Result.success(Unit)
        val users = mutableMapOf<String, UserProfile>()

        override suspend fun loadPendingInvitations(): Result<List<PendingInvitation>> =
            loadResult ?: Result.success(pending)

        override suspend fun approve(invitation: Invitation, now: Long): Result<Unit> {
            calls += "approve:${invitation.friendUid}"
            if (approveResult.isSuccess) pending = pending.filter { it.invitation.friendUid != invitation.friendUid }
            return approveResult
        }

        override suspend fun reject(invitation: Invitation, now: Long): Result<Unit> {
            calls += "reject:${invitation.friendUid}"
            pending = pending.filter { it.invitation.friendUid != invitation.friendUid }
            return Result.success(Unit)
        }

        override suspend fun findUser(query: String): Result<UserProfile?> {
            calls += "find:$query"
            return Result.success(users.values.firstOrNull { it.uid == query || it.email == query.lowercase() })
        }

        override suspend fun extendMembership(uid: String, days: Int, now: Long): Result<UserProfile> {
            calls += "extend:$uid:$days"
            val user = users.getValue(uid)
            val updated = user.copy(
                isPremium = true,
                membershipExpiry = MembershipPolicy.extendedExpiry(user.membershipExpiry, now, days),
            )
            users[uid] = updated
            return Result.success(updated)
        }

        override suspend fun revokeMembership(uid: String, now: Long): Result<UserProfile> {
            calls += "revoke:$uid"
            val updated = users.getValue(uid).copy(isPremium = false, membershipExpiry = now)
            users[uid] = updated
            return Result.success(updated)
        }
    }

    private fun invitation(friendUid: String, referralNumber: Int = 1) = PendingInvitation(
        invitation = Invitation(
            friendUid = friendUid,
            friendEmail = "$friendUid@example.com",
            inviterUid = "inviter",
            inviterEmail = "inviter@example.com",
            referralCode = "ABCD2345",
        ),
        inviterReferralNumber = referralNumber,
    )

    private fun user(uid: String, email: String) = UserProfile(uid = uid, email = email, referralCode = "CODE2345")

    private val admin = FakeAdmin()
    private lateinit var viewModel: AdminViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AdminViewModel(admin) { now }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- Invitations ----

    @Test
    fun refreshLoadsThePendingInvitations() {
        admin.pending = listOf(invitation("f1"), invitation("f2"))

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals(listOf("f1", "f2"), state.invitations.map { it.invitation.friendUid })
        assertFalse(state.isLoadingInvitations)
    }

    @Test
    fun aFailedLoadShowsAMessage() {
        admin.loadResult = Result.failure(IllegalStateException("Permission denied."))

        viewModel.refresh()

        assertEquals("Permission denied.", viewModel.uiState.value.message)
        assertTrue(viewModel.uiState.value.invitations.isEmpty())
        assertFalse(viewModel.uiState.value.isLoadingInvitations)
    }

    @Test
    fun approvingRemovesTheInvitationAndSaysWhatWasGranted() {
        admin.pending = listOf(invitation("f1"), invitation("f2"))
        viewModel.refresh()

        viewModel.approve(viewModel.uiState.value.invitations.first())

        val state = viewModel.uiState.value
        assertEquals(listOf("approve:f1"), admin.calls)
        assertEquals(listOf("f2"), state.invitations.map { it.invitation.friendUid })
        assertEquals("Approved: f1@example.com +7 days, inviter@example.com +5 days.", state.message)
        assertFalse(state.isWorking)
    }

    @Test
    fun aFailedApprovalKeepsTheInvitationAndShowsTheReason() {
        admin.pending = listOf(invitation("f1"))
        admin.approveResult = Result.failure(IllegalStateException("This invitation was already handled."))
        viewModel.refresh()

        viewModel.approve(viewModel.uiState.value.invitations.single())

        val state = viewModel.uiState.value
        assertEquals("This invitation was already handled.", state.message)
        assertEquals(1, state.invitations.size)
    }

    @Test
    fun rejectingRemovesTheInvitation() {
        admin.pending = listOf(invitation("f1"))
        viewModel.refresh()

        viewModel.reject(viewModel.uiState.value.invitations.single())

        assertEquals(listOf("reject:f1"), admin.calls)
        assertTrue(viewModel.uiState.value.invitations.isEmpty())
        assertEquals("Rejected the invitation from f1@example.com.", viewModel.uiState.value.message)
    }

    // ---- Finding users ----

    @Test
    fun aUserCanBeFoundByEmailAndShowsTheirStatus() {
        admin.users["u1"] = user("u1", "ann@example.com")

        viewModel.onQueryChange("  Ann@Example.com ")
        viewModel.findUser()

        val state = viewModel.uiState.value
        assertEquals("u1", state.foundUser?.uid)
        assertEquals("No active membership", state.foundUserStatus)
        assertFalse(state.notFound)
        assertEquals(listOf("find:Ann@Example.com"), admin.calls)
    }

    @Test
    fun aUserCanBeFoundByUserId() {
        admin.users["u1"] = user("u1", "ann@example.com")

        viewModel.onQueryChange("u1")
        viewModel.findUser()

        assertNotNull(viewModel.uiState.value.foundUser)
    }

    @Test
    fun anUnknownUserShowsNotFound() {
        viewModel.onQueryChange("nobody@example.com")
        viewModel.findUser()

        val state = viewModel.uiState.value
        assertNull(state.foundUser)
        assertTrue(state.notFound)
    }

    @Test
    fun aBlankQueryDoesNotSearch() {
        viewModel.onQueryChange("   ")
        viewModel.findUser()

        assertTrue(admin.calls.isEmpty())
    }

    @Test
    fun editingTheQueryClearsThePreviousResult() {
        admin.users["u1"] = user("u1", "ann@example.com")
        viewModel.onQueryChange("u1")
        viewModel.findUser()

        viewModel.onQueryChange("u2")

        val state = viewModel.uiState.value
        assertNull(state.foundUser)
        assertFalse(state.notFound)
        assertEquals("u2", state.query)
    }

    // ---- Membership changes ----

    @Test
    fun addingDaysEnablesPremiumAndUpdatesTheStatus() {
        admin.users["u1"] = user("u1", "ann@example.com")
        viewModel.onQueryChange("u1")
        viewModel.findUser()

        viewModel.extendMembership(30)

        val state = viewModel.uiState.value
        assertEquals(now + 30 * day, state.foundUser?.membershipExpiry)
        assertTrue(state.foundUser?.isPremium == true)
        assertTrue(state.foundUserStatus, state.foundUserStatus.contains("(30 days left)"))
        assertEquals("Added 30 days for ann@example.com.", state.message)
    }

    @Test
    fun addingDaysTwiceKeepsAddingToTheRunningMembership() {
        admin.users["u1"] = user("u1", "ann@example.com")
        viewModel.onQueryChange("u1")
        viewModel.findUser()

        viewModel.extendMembership(7)
        viewModel.extendMembership(30)

        assertEquals(now + 37 * day, viewModel.uiState.value.foundUser?.membershipExpiry)
    }

    @Test
    fun removingPremiumEndsTheMembership() {
        admin.users["u1"] = user("u1", "ann@example.com").copy(isPremium = true, membershipExpiry = now + 10 * day)
        viewModel.onQueryChange("u1")
        viewModel.findUser()

        viewModel.revokeMembership()

        val state = viewModel.uiState.value
        assertFalse(state.foundUser?.isPremium == true)
        assertTrue(state.foundUserStatus, state.foundUserStatus.startsWith("Membership expired on"))
        assertEquals("Ended the membership of ann@example.com.", state.message)
    }

    @Test
    fun membershipChangesNeedAFoundUser() {
        viewModel.extendMembership(7)
        viewModel.revokeMembership()

        assertTrue(admin.calls.isEmpty())
    }

    @Test
    fun messagesCanBeDismissed() {
        admin.loadResult = Result.failure(IllegalStateException("Boom"))
        viewModel.refresh()

        viewModel.dismissMessage()

        assertNull(viewModel.uiState.value.message)
    }
}
