package com.wortart.app.ui.members

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.wortart.app.data.auth.AuthRepository
import com.wortart.app.data.auth.AuthUser
import com.wortart.app.data.membership.MembershipRepository
import com.wortart.app.data.remote.InvitationRepository
import com.wortart.app.data.remote.UserRepository
import com.wortart.app.domain.membership.Invitation
import com.wortart.app.domain.membership.InvitationStatus
import com.wortart.app.domain.membership.MembershipPolicy
import com.wortart.app.domain.membership.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class MembersViewModelTest {

    private val now = 1_800_000_000_000L
    private val day = MembershipPolicy.DAY_MILLIS
    private val context: Context = Mockito.mock(Context::class.java)

    private class FakeAuth(
        override val isConfigured: Boolean,
        private val calls: MutableList<String>,
    ) : AuthRepository {
        val user = MutableStateFlow<AuthUser?>(null)
        var signInResult: Result<Unit> = Result.success(Unit)
        var reauthResult: Result<Unit> = Result.success(Unit)

        override val currentUser: Flow<AuthUser?> = user

        override suspend fun signInWithGoogle(context: Context): Result<Unit> {
            calls += "signIn"
            signInResult.onSuccess { user.value = AuthUser("u1", "me@example.com") }
            return signInResult
        }

        override suspend fun reauthenticate(context: Context): Result<Unit> {
            calls += "reauth"
            return reauthResult
        }

        override suspend fun deleteCurrentUser(): Result<Unit> {
            calls += "deleteAuth"
            user.value = null
            return Result.success(Unit)
        }

        override suspend fun signOut(context: Context) {
            calls += "signOut"
            user.value = null
        }
    }

    private class FakeUsers(private val calls: MutableList<String>) : UserRepository {
        val profiles = MutableStateFlow<Map<String, UserProfile>>(emptyMap())

        override fun observeProfile(uid: String): Flow<UserProfile?> = profiles.map { it[uid] }

        override suspend fun ensureProfile(uid: String, email: String): Result<UserProfile> {
            val existing = profiles.value[uid]
            if (existing != null) return Result.success(existing)
            val created = UserProfile(uid = uid, email = email, referralCode = "ABCD2345")
            profiles.value = profiles.value + (uid to created)
            return Result.success(created)
        }

        override suspend fun deleteProfile(profile: UserProfile): Result<Unit> {
            calls += "deleteProfile"
            profiles.value = profiles.value - profile.uid
            return Result.success(Unit)
        }
    }

    private class FakeInvitations(private val calls: MutableList<String>) : InvitationRepository {
        val stored = MutableStateFlow<Map<String, Invitation>>(emptyMap())
        var applyResult: Result<Unit>? = null
        val progress = mutableListOf<Pair<String, Int>>()

        override fun observeMyInvitation(friendUid: String): Flow<Invitation?> = stored.map { it[friendUid] }

        override suspend fun applyCode(friend: AuthUser, rawCode: String, cardsPlayed: Int): Result<Unit> {
            calls += "applyCode:$rawCode:$cardsPlayed"
            applyResult?.let { return it }
            stored.value = stored.value + (
                friend.uid to Invitation(
                    friendUid = friend.uid,
                    friendEmail = friend.email.orEmpty(),
                    inviterUid = "inviter",
                    inviterEmail = "inviter@example.com",
                    referralCode = rawCode,
                )
                )
            return Result.success(Unit)
        }

        override suspend fun discardMyInvitation(friendUid: String): Result<Unit> {
            calls += "discardInvitation"
            stored.value = stored.value - friendUid
            return Result.success(Unit)
        }

        override suspend fun reportProgress(friendUid: String, cardsPlayed: Int): Result<Unit> {
            progress += friendUid to cardsPlayed
            return Result.success(Unit)
        }
    }

    private class Fixture(scope: CoroutineScope, configured: Boolean) {
        val calls = mutableListOf<String>()
        val auth = FakeAuth(configured, calls)
        val users = FakeUsers(calls)
        val invitations = FakeInvitations(calls)
        val viewModel: MembersViewModel

        init {
            val membership = MembershipRepository(auth, users, scope)
            viewModel = MembersViewModel(
                membership = membership,
                auth = auth,
                users = users,
                invitations = invitations,
                cardsPlayed = { 42 },
                resetLocalData = { calls += "resetLocal" },
                clock = { 1_800_000_000_000L },
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Builds the fixture and keeps the ViewModel state active while [block] runs. */
    private fun runFixture(
        configured: Boolean = true,
        block: suspend TestScope.(Fixture) -> Unit,
    ) = runTest {
        val scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val fixture = Fixture(scope, configured)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.viewModel.uiState.collect { }
        }
        this.block(fixture)
    }

    private fun profile(
        isPremium: Boolean = false,
        expiry: Long? = null,
        isAdmin: Boolean = false,
    ) = UserProfile(
        uid = "u1",
        email = "me@example.com",
        isPremium = isPremium,
        membershipExpiry = expiry,
        referralCode = "ABCD2345",
        isAdmin = isAdmin,
    )

    /** Signs in as "u1" with an existing [profile]. */
    private fun Fixture.signedInWith(profile: UserProfile) {
        users.profiles.value = mapOf(profile.uid to profile)
        auth.user.value = AuthUser(profile.uid, profile.email)
    }

    // ---- States ----

    @Test
    fun withoutFirebaseTheScreenKnowsAccountsAreNotSetUp() = runFixture(configured = false) { f ->
        assertFalse(f.viewModel.uiState.value.isConfigured)
        assertFalse(f.viewModel.uiState.value.isSignedIn)
    }

    @Test
    fun signedOutUsersAreOnTheFreePlan() = runFixture { f ->
        val state = f.viewModel.uiState.value

        assertTrue(state.isConfigured)
        assertFalse(state.isSignedIn)
        assertFalse(state.isMemberActive)
        assertEquals("No active membership", state.statusText)
    }

    @Test
    fun signingInCreatesTheProfileAndShowsTheAccount() = runFixture { f ->
        f.viewModel.signIn(context)

        val state = f.viewModel.uiState.value
        assertTrue(state.isSignedIn)
        assertEquals("me@example.com", state.user?.email)
        assertEquals("ABCD2345", state.profile?.referralCode)
        assertFalse(state.isMemberActive)
        assertFalse(state.isBusy)
    }

    @Test
    fun activePremiumShowsMemberStatus() = runFixture { f ->
        f.signedInWith(profile(isPremium = true, expiry = now + 5 * day))

        val state = f.viewModel.uiState.value
        assertTrue(state.isMemberActive)
        assertTrue(state.statusText, state.statusText.startsWith("Premium member"))
        assertTrue(state.statusText, state.statusText.contains("(5 days left)"))
        assertFalse(state.isAdmin)
    }

    @Test
    fun expiredPremiumIsTreatedAsFree() = runFixture { f ->
        f.signedInWith(profile(isPremium = true, expiry = now - day))

        val state = f.viewModel.uiState.value
        assertFalse(state.isMemberActive)
        assertTrue(state.statusText, state.statusText.startsWith("Membership expired on"))
    }

    @Test
    fun adminFlagIsExposedForTheAdminButton() = runFixture { f ->
        f.signedInWith(profile(isAdmin = true))

        assertTrue(f.viewModel.uiState.value.isAdmin)
    }

    // ---- Sign-in, sign-out ----

    @Test
    fun failedSignInShowsAMessageAndStopsBeingBusy() = runFixture { f ->
        f.auth.signInResult = Result.failure(IllegalStateException("Boom"))

        f.viewModel.signIn(context)

        val state = f.viewModel.uiState.value
        assertEquals("Boom", state.message)
        assertFalse(state.isBusy)
        assertFalse(state.isSignedIn)
    }

    @Test
    fun cancellingTheAccountChooserShowsNoMessage() = runFixture { f ->
        f.auth.signInResult = Result.failure(GetCredentialCancellationException())

        f.viewModel.signIn(context)

        assertNull(f.viewModel.uiState.value.message)
    }

    @Test
    fun signOutEndsTheSession() = runFixture { f ->
        f.signedInWith(profile())

        f.viewModel.signOut(context)

        assertFalse(f.viewModel.uiState.value.isSignedIn)
        assertTrue("signOut" in f.calls)
    }

    @Test
    fun messagesCanBeDismissed() = runFixture { f ->
        f.auth.signInResult = Result.failure(IllegalStateException("Boom"))
        f.viewModel.signIn(context)

        f.viewModel.dismissMessage()

        assertNull(f.viewModel.uiState.value.message)
    }

    // ---- Delete account ----

    @Test
    fun deletingTheAccountRunsAllStepsInOrder() = runFixture { f ->
        f.signedInWith(profile(isPremium = true, expiry = now + day))

        f.viewModel.deleteAccount(context)

        assertEquals(listOf("reauth", "discardInvitation", "deleteProfile", "deleteAuth", "resetLocal"), f.calls)
        val state = f.viewModel.uiState.value
        assertEquals("Your account was deleted.", state.message)
        assertFalse(state.isSignedIn)
        assertFalse(state.isBusy)
    }

    @Test
    fun deletionStopsWhenTheGoogleAccountIsNotConfirmed() = runFixture { f ->
        f.signedInWith(profile())
        f.auth.reauthResult = Result.failure(IllegalStateException("Not confirmed"))

        f.viewModel.deleteAccount(context)

        assertEquals(listOf("reauth"), f.calls)
        assertEquals("Not confirmed", f.viewModel.uiState.value.message)
        assertTrue(f.viewModel.uiState.value.isSignedIn)
        assertFalse(f.viewModel.uiState.value.isBusy)
    }

    @Test
    fun deletionDoesNothingWithoutAProfile() = runFixture { f ->
        f.viewModel.deleteAccount(context)

        assertTrue(f.calls.isEmpty())
    }

    // ---- Invite codes ----

    @Test
    fun enteringACodeCreatesAPendingInvitation() = runFixture { f ->
        f.signedInWith(profile())

        f.viewModel.applyInviteCode("ABCD2345")

        val invitation = f.viewModel.uiState.value.invitation
        assertEquals(InvitationStatus.PENDING, invitation?.status)
        assertEquals("ABCD2345", invitation?.referralCode)
        assertTrue("applyCode:ABCD2345:42" in f.calls)
        assertFalse(f.viewModel.uiState.value.isBusy)
    }

    @Test
    fun aRefusedCodeShowsTheReason() = runFixture { f ->
        f.signedInWith(profile())
        f.invitations.applyResult = Result.failure(IllegalStateException("We could not find that code."))

        f.viewModel.applyInviteCode("ZZZZ2222")

        assertEquals("We could not find that code.", f.viewModel.uiState.value.message)
        assertNull(f.viewModel.uiState.value.invitation)
    }

    @Test
    fun enteringACodeWhileSignedOutDoesNothing() = runFixture { f ->
        f.viewModel.applyInviteCode("ABCD2345")

        assertTrue(f.calls.isEmpty())
    }

    @Test
    fun aRejectedInvitationCanBeReplacedByAnotherCode() = runFixture { f ->
        f.signedInWith(profile())
        f.invitations.stored.value = mapOf(
            "u1" to Invitation("u1", "me@example.com", "x", "x@example.com", "ABCD2345", InvitationStatus.REJECTED),
        )
        assertEquals(InvitationStatus.REJECTED, f.viewModel.uiState.value.invitation?.status)

        f.viewModel.tryAnotherCode()

        assertNull(f.viewModel.uiState.value.invitation)
    }

    @Test
    fun cardCountIsReportedForAPendingInvitation() = runFixture { f ->
        f.signedInWith(profile())
        f.invitations.stored.value = mapOf(
            "u1" to Invitation("u1", "me@example.com", "x", "x@example.com", "ABCD2345"),
        )

        f.viewModel.refreshInvitationProgress()

        assertEquals(listOf("u1" to 42), f.invitations.progress)
    }

    @Test
    fun cardCountIsNotReportedOnceTheInvitationWasHandled() = runFixture { f ->
        f.signedInWith(profile())
        f.invitations.stored.value = mapOf(
            "u1" to Invitation("u1", "me@example.com", "x", "x@example.com", "ABCD2345", InvitationStatus.APPROVED),
        )

        f.viewModel.refreshInvitationProgress()

        assertTrue(f.invitations.progress.isEmpty())
    }
}
