package com.wortart.app.ui.members

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wortart.app.data.auth.AuthUser
import com.wortart.app.domain.membership.Invitation
import com.wortart.app.domain.membership.InvitationStatus
import com.wortart.app.domain.membership.MemberBenefits
import com.wortart.app.domain.membership.UserProfile
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MembersScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val profile = UserProfile(
        uid = "u1",
        email = "me@example.com",
        referralCode = "ABCD2345",
        totalReferrals = 3,
    )

    private fun invitation(status: InvitationStatus) = Invitation(
        friendUid = "u1",
        friendEmail = "me@example.com",
        inviterUid = "i1",
        inviterEmail = "inviter@example.com",
        referralCode = "QFV57FAH",
        status = status,
    )

    private fun signedIn(
        profile: UserProfile = this.profile,
        displayName: String? = null,
        statusText: String = "No active membership",
        isMemberActive: Boolean = false,
        invitation: Invitation? = null,
        message: String? = null,
    ) = MembersUiState(
        user = AuthUser(profile.uid, profile.email, displayName),
        profile = profile,
        invitation = invitation,
        statusText = statusText,
        isMemberActive = isMemberActive,
        message = message,
    )

    private fun show(
        state: MembersUiState,
        supportEmail: String = "",
        onSignIn: () -> Unit = {},
        onSignOut: () -> Unit = {},
        onDeleteAccount: () -> Unit = {},
        onOpenAdmin: () -> Unit = {},
        onCopyReferralCode: (String) -> Unit = {},
        onShareReferralCode: (String) -> Unit = {},
        onApplyInviteCode: (String) -> Unit = {},
        onTryAnotherCode: () -> Unit = {},
        onContactSupport: () -> Unit = {},
        onDismissMessage: () -> Unit = {},
    ) {
        composeRule.setContent {
            WortArtTheme {
                MembersScreen(
                    state = state,
                    onSignIn = onSignIn,
                    onSignOut = onSignOut,
                    onDeleteAccount = onDeleteAccount,
                    onOpenAdmin = onOpenAdmin,
                    onCopyReferralCode = onCopyReferralCode,
                    onShareReferralCode = onShareReferralCode,
                    onApplyInviteCode = onApplyInviteCode,
                    onTryAnotherCode = onTryAnotherCode,
                    onContactSupport = onContactSupport,
                    onDismissMessage = onDismissMessage,
                    supportEmail = supportEmail,
                )
            }
        }
    }

    // ---- Signed out / not configured ----

    @Test
    fun withoutFirebaseTheScreenExplainsTheSetup() {
        show(MembersUiState(isConfigured = false))

        composeRule.onNodeWithText("Member Access").assertExists()
        composeRule.onNodeWithText("Accounts are not set up yet").assertExists()
        composeRule.onNodeWithText("Sign in with Google").assertDoesNotExist()
    }

    @Test
    fun signedOutUsersSeeSignInAndAllBenefits() {
        var signIns = 0
        show(MembersUiState(), onSignIn = { signIns++ })

        composeRule.onNodeWithText("Member benefits").assertExists()
        MemberBenefits.ALL.forEach { composeRule.onNodeWithText(it).assertExists() }
        composeRule.onNodeWithText("Sign out").assertDoesNotExist()
        composeRule.onNodeWithText("Invite friends").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("More options").assertDoesNotExist()

        composeRule.onNodeWithText("Sign in with Google").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(1, signIns) }
    }

    // ---- Account card ----

    @Test
    fun signedInUsersSeeNameEmailAndStatusAndCanSignOut() {
        var signOuts = 0
        show(signedIn(displayName = "Sowrov Hin"), onSignOut = { signOuts++ })

        composeRule.onNodeWithText("Sowrov Hin").performScrollTo().assertExists()
        composeRule.onNodeWithText("me@example.com").assertExists()
        composeRule.onNodeWithText("No active membership").assertExists()

        composeRule.onNodeWithText("Sign out").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(1, signOuts) }
    }

    @Test
    fun withoutANameTheEmailIsTheTitle() {
        show(signedIn(displayName = null))

        composeRule.onNodeWithText("me@example.com").performScrollTo().assertExists()
    }

    @Test
    fun activeMembersSeeTheirMembershipStatus() {
        show(
            signedIn(
                statusText = "Premium member · until 23 Jan 2027 (8 days left)",
                isMemberActive = true,
            ),
        )

        composeRule.onNodeWithText("Premium member · until 23 Jan 2027 (8 days left)")
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun adminButtonIsOnlyShownToAdmins() {
        show(signedIn(profile = profile.copy(isAdmin = false)))

        composeRule.onNodeWithText("Admin panel").assertDoesNotExist()
    }

    @Test
    fun adminsCanOpenTheAdminPanel() {
        var opened = 0
        show(signedIn(profile = profile.copy(isAdmin = true)), onOpenAdmin = { opened++ })

        composeRule.onNodeWithText("Admin panel").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(1, opened) }
    }

    // ---- Invite friends ----

    @Test
    fun inviteCardShowsTheStepsTheCodeAndTheReferralCount() {
        show(signedIn())

        composeRule.onNodeWithText("Invite friends").performScrollTo().assertExists()
        composeRule.onNodeWithText("Invite a friend and you both get free member days.").assertExists()
        composeRule.onNodeWithText("Download WortArt app").assertExists()
        composeRule.onNodeWithText("Copy your invitation code").assertExists()
        composeRule.onNodeWithText("ABCD2345").assertExists()
        composeRule.onNodeWithText("Total referrals: 3").assertExists()
    }

    @Test
    fun theCodeCanBeCopiedAndShared() {
        val copied = mutableListOf<String>()
        val shared = mutableListOf<String>()
        show(
            signedIn(),
            onCopyReferralCode = { copied += it },
            onShareReferralCode = { shared += it },
        )

        composeRule.onNodeWithContentDescription("Copy invite code").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Share invite").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("ABCD2345"), copied)
            assertEquals(listOf("ABCD2345"), shared)
        }
    }

    @Test
    fun aFriendCanEnterACodeOnceItHasEightCharacters() {
        val applied = mutableListOf<String>()
        show(signedIn(invitation = null), onApplyInviteCode = { applied += it })

        composeRule.onNodeWithText("Have an invite code?").performScrollTo().assertExists()
        composeRule.onNodeWithText("Apply").performScrollTo().assertIsNotEnabled()

        composeRule.onNodeWithTag("inviteCodeField").performTextInput("abcd23")
        composeRule.onNodeWithText("Apply").assertIsNotEnabled()

        composeRule.onNodeWithTag("inviteCodeField").performTextInput("45")
        composeRule.onNodeWithText("Apply").assertIsEnabled()
        composeRule.onNodeWithText("Apply").performClick()

        composeRule.runOnIdle { assertEquals(listOf("ABCD2345"), applied) }
    }

    @Test
    fun aPendingInvitationShowsTheWaitingMessage() {
        show(signedIn(invitation = invitation(InvitationStatus.PENDING)))

        composeRule.onNodeWithText("Code applied. We will activate your 7 days within 24 hours.")
            .performScrollTo()
            .assertExists()
        composeRule.onNodeWithText("Have an invite code?").assertDoesNotExist()
    }

    @Test
    fun anApprovedInvitationShowsTheGoodNews() {
        show(signedIn(invitation = invitation(InvitationStatus.APPROVED)))

        composeRule.onNodeWithText("Your invite code was approved. Enjoy your free days!")
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun aRejectedInvitationOffersAnotherTry() {
        var retries = 0
        show(signedIn(invitation = invitation(InvitationStatus.REJECTED)), onTryAnotherCode = { retries++ })

        composeRule.onNodeWithText("Your invite code was not approved.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Try another code").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    // ---- Support card ----

    @Test
    fun supportCardIsHiddenWithoutASupportEmail() {
        show(signedIn(), supportEmail = "")

        composeRule.onNodeWithText("Member activation").assertDoesNotExist()
    }

    @Test
    fun supportCardOpensTheMailApp() {
        var contacts = 0
        show(signedIn(), supportEmail = "help@example.com", onContactSupport = { contacts++ })

        composeRule.onNodeWithText("Member activation").performScrollTo().assertExists()
        composeRule.onNodeWithText("Enrolled in our course or community? Contact support to activate your access.")
            .assertExists()
        composeRule.onNodeWithText("Contact support").performScrollTo().performClick()

        composeRule.runOnIdle { assertEquals(1, contacts) }
    }

    // ---- Delete account (in the overflow menu) ----

    @Test
    fun deletingAnAccountNeedsConfirmation() {
        var deletes = 0
        show(signedIn(), onDeleteAccount = { deletes++ })

        // Open the menu and choose Delete account: a confirmation dialog appears.
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Delete account").performClick()
        composeRule.onNodeWithText("Delete account?").assertExists()

        // Cancel keeps the account.
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Delete account?").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, deletes) }

        // Confirm deletes it.
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Delete account").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.runOnIdle { assertEquals(1, deletes) }
    }

    // ---- Messages and progress ----

    @Test
    fun messagesAreShownAndCanBeDismissed() {
        var dismissed = 0
        show(signedIn(message = "Your account was deleted."), onDismissMessage = { dismissed++ })

        composeRule.onNodeWithText("Your account was deleted.").assertExists()
        composeRule.onNodeWithText("OK").performClick()

        composeRule.runOnIdle { assertEquals(1, dismissed) }
    }

    @Test
    fun waitingForTheProfileShowsAProgressNote() {
        show(MembersUiState(user = AuthUser("u1", "me@example.com"), isLoadingProfile = true))

        composeRule.onNodeWithText("Setting up your account…").assertExists()
    }
}
