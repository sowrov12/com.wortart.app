package com.wortart.app.ui.admin

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
import com.wortart.app.domain.membership.Invitation
import com.wortart.app.domain.membership.PendingInvitation
import com.wortart.app.domain.membership.UserProfile
import com.wortart.app.ui.theme.WortArtTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pending = PendingInvitation(
        invitation = Invitation(
            friendUid = "f1",
            friendEmail = "friend@example.com",
            inviterUid = "i1",
            inviterEmail = "inviter@example.com",
            referralCode = "QFV57FAH",
            friendCardsPlayed = 3,
        ),
        inviterReferralNumber = 5,
    )

    private val foundUser = UserProfile(uid = "u1", email = "ann@example.com", referralCode = "CODE2345")

    private fun show(
        state: AdminUiState,
        onBack: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onApprove: (PendingInvitation) -> Unit = {},
        onReject: (PendingInvitation) -> Unit = {},
        onQueryChange: (String) -> Unit = {},
        onFindUser: () -> Unit = {},
        onExtendMembership: (Int) -> Unit = {},
        onRevokeMembership: () -> Unit = {},
        onDismissMessage: () -> Unit = {},
    ) {
        composeRule.setContent {
            WortArtTheme {
                AdminScreen(
                    state = state,
                    onBack = onBack,
                    onRefresh = onRefresh,
                    onApprove = onApprove,
                    onReject = onReject,
                    onQueryChange = onQueryChange,
                    onFindUser = onFindUser,
                    onExtendMembership = onExtendMembership,
                    onRevokeMembership = onRevokeMembership,
                    onDismissMessage = onDismissMessage,
                )
            }
        }
    }

    @Test
    fun showsTheHeaderAndTheInvitationDetails() {
        show(AdminUiState(invitations = listOf(pending)))

        composeRule.onNodeWithText("Admin").assertExists()
        composeRule.onNodeWithText("Find a user by User ID or email, then set their Premium.").assertExists()
        composeRule.onNodeWithText("Invitations (1)").assertExists()
        composeRule.onNodeWithText("Friend: friend@example.com").assertExists()
        composeRule.onNodeWithText("Invited by: inviter@example.com").assertExists()
        composeRule.onNodeWithText("Code QFV57FAH · friend has answered 3 cards").assertExists()
        composeRule.onNodeWithText("Gives: friend 7 days, inviter 5 days (referral #5)").assertExists()
    }

    @Test
    fun approveAndRejectReportTheInvitation() {
        val approved = mutableListOf<PendingInvitation>()
        val rejected = mutableListOf<PendingInvitation>()
        show(
            AdminUiState(invitations = listOf(pending)),
            onApprove = { approved += it },
            onReject = { rejected += it },
        )

        composeRule.onNodeWithText("Approve").performScrollTo().performClick()
        composeRule.onNodeWithText("Reject").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(pending), approved)
            assertEquals(listOf(pending), rejected)
        }
    }

    @Test
    fun buttonsAreDisabledWhileWorking() {
        show(AdminUiState(invitations = listOf(pending), isWorking = true))

        composeRule.onNodeWithText("Approve").assertIsNotEnabled()
        composeRule.onNodeWithText("Reject").assertIsNotEnabled()
    }

    @Test
    fun emptyListSaysThereAreNoInvitations() {
        show(AdminUiState())

        composeRule.onNodeWithText("Invitations (0)").assertExists()
        composeRule.onNodeWithText("No pending invitations.").assertExists()
    }

    @Test
    fun refreshAndBackButtonsWork() {
        var refreshes = 0
        var backs = 0
        show(AdminUiState(), onRefresh = { refreshes++ }, onBack = { backs++ })

        composeRule.onNodeWithText("Refresh").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.runOnIdle {
            assertEquals(1, refreshes)
            assertEquals(1, backs)
        }
    }

    @Test
    fun findUserNeedsTextAndReportsTyping() {
        val typed = mutableListOf<String>()
        var finds = 0

        show(AdminUiState(query = ""), onQueryChange = { typed += it }, onFindUser = { finds++ })
        composeRule.onNodeWithText("Find user").assertIsNotEnabled()
        composeRule.onNodeWithTag("adminSearchField").performTextInput("ann@example.com")
        composeRule.runOnIdle {
            assertEquals(listOf("ann@example.com"), typed)
            assertEquals(0, finds)
        }
    }

    @Test
    fun findUserIsEnabledWithTextAndSearches() {
        var finds = 0
        show(AdminUiState(query = "ann@example.com"), onFindUser = { finds++ })

        composeRule.onNodeWithText("Find user").assertIsEnabled()
        composeRule.onNodeWithText("Find user").performClick()

        composeRule.runOnIdle { assertEquals(1, finds) }
    }

    @Test
    fun aFoundUserCanGetDaysOrLosePremium() {
        val extended = mutableListOf<Int>()
        var revoked = 0
        show(
            AdminUiState(
                query = "ann@example.com",
                foundUser = foundUser,
                foundUserStatus = "No active membership",
            ),
            onExtendMembership = { extended += it },
            onRevokeMembership = { revoked++ },
        )

        composeRule.onNodeWithText("ann@example.com").performScrollTo().assertExists()
        composeRule.onNodeWithText("ID: u1").assertExists()
        composeRule.onNodeWithText("No active membership").assertExists()

        composeRule.onNodeWithText("+7 days").performScrollTo().performClick()
        composeRule.onNodeWithText("+30 days").performScrollTo().performClick()
        composeRule.onNodeWithText("+365 days").performScrollTo().performClick()
        composeRule.onNodeWithText("Remove Premium").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(7, 30, 365), extended)
            assertEquals(1, revoked)
        }
    }

    @Test
    fun unknownUsersShowAMessage() {
        show(AdminUiState(query = "nobody", notFound = true))

        composeRule.onNodeWithText("No user found.").performScrollTo().assertExists()
    }

    @Test
    fun messagesCanBeDismissed() {
        var dismissed = 0
        show(AdminUiState(message = "Added 7 days for ann@example.com."), onDismissMessage = { dismissed++ })

        composeRule.onNodeWithText("Added 7 days for ann@example.com.").assertExists()
        composeRule.onNodeWithText("OK").performClick()

        composeRule.runOnIdle { assertEquals(1, dismissed) }
    }
}
