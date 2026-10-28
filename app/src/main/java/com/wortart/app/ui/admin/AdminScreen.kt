package com.wortart.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wortart.app.domain.membership.MembershipPolicy
import com.wortart.app.domain.membership.PendingInvitation
import com.wortart.app.domain.membership.UserProfile
import com.wortart.app.ui.components.MessageBanner
import com.wortart.app.ui.theme.WortArtColors

/** Opens the admin panel and loads the pending invitations; also handles the Back button. */
@Composable
fun AdminRoute(viewModel: AdminViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }
    BackHandler(onBack = onBack)

    AdminScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onApprove = viewModel::approve,
        onReject = viewModel::reject,
        onQueryChange = viewModel::onQueryChange,
        onFindUser = viewModel::findUser,
        onExtendMembership = viewModel::extendMembership,
        onRevokeMembership = viewModel::revokeMembership,
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

/** In-app admin panel: pending invitations, and manual Premium for one user. Stateless. */
@Composable
fun AdminScreen(
    state: AdminUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onApprove: (PendingInvitation) -> Unit,
    onReject: (PendingInvitation) -> Unit,
    onQueryChange: (String) -> Unit,
    onFindUser: () -> Unit,
    onExtendMembership: (Int) -> Unit,
    onRevokeMembership: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(WortArtColors.Surface, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = WortArtColors.TextPrimary,
                )
            }
            Spacer(Modifier.size(14.dp))
            Text(
                text = "Admin",
                color = WortArtColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = "Find a user by User ID or email, then set their Premium.",
            color = WortArtColors.TextSecondary,
            fontSize = 14.sp,
        )

        state.message?.let { MessageBanner(text = it, onDismiss = onDismissMessage) }

        InvitationsCard(
            state = state,
            onRefresh = onRefresh,
            onApprove = onApprove,
            onReject = onReject,
        )

        UserSearch(
            state = state,
            onQueryChange = onQueryChange,
            onFindUser = onFindUser,
        )

        state.foundUser?.let { user ->
            FoundUserCard(
                user = user,
                status = state.foundUserStatus,
                enabled = !state.isWorking,
                onExtendMembership = onExtendMembership,
                onRevokeMembership = onRevokeMembership,
            )
        }
        if (state.notFound) {
            Text(text = "No user found.", color = WortArtColors.TextSecondary, fontSize = 15.sp)
        }
    }
}

@Composable
private fun InvitationsCard(
    state: AdminUiState,
    onRefresh: () -> Unit,
    onApprove: (PendingInvitation) -> Unit,
    onReject: (PendingInvitation) -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Gold.copy(alpha = 0.25f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Invitations (${state.invitations.size})",
                color = WortArtColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (state.isLoadingInvitations || state.isWorking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = WortArtColors.Gold,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(8.dp))
            }
            TextButton(onClick = onRefresh, enabled = !state.isLoadingInvitations && !state.isWorking) {
                Text(text = "Refresh", color = WortArtColors.Gold, fontWeight = FontWeight.Medium)
            }
        }

        if (state.invitations.isEmpty() && !state.isLoadingInvitations) {
            Text(
                text = "No pending invitations.",
                color = WortArtColors.TextSecondary,
                fontSize = 14.sp,
            )
        }

        state.invitations.forEach { pending ->
            InvitationItem(
                pending = pending,
                enabled = !state.isWorking,
                onApprove = { onApprove(pending) },
                onReject = { onReject(pending) },
            )
        }
    }
}

@Composable
private fun InvitationItem(
    pending: PendingInvitation,
    enabled: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val invitation = pending.invitation
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.SurfaceDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Friend: ${invitation.friendEmail}",
            color = WortArtColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Invited by: ${invitation.inviterEmail}",
            color = WortArtColors.TextSecondary,
            fontSize = 14.sp,
        )
        Text(
            text = "Code ${invitation.referralCode} · friend has answered ${invitation.friendCardsPlayed} cards",
            color = WortArtColors.TextSecondary,
            fontSize = 14.sp,
        )
        Text(
            text = "Gives: friend ${MembershipPolicy.REFERRAL_FRIEND_DAYS} days, " +
                "inviter ${MembershipPolicy.REFERRAL_INVITER_DAYS} days " +
                "(referral #${pending.inviterReferralNumber})",
            color = WortArtColors.Gold,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onApprove,
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WortArtColors.Gold,
                    contentColor = WortArtColors.OnGold,
                ),
            ) {
                Text(text = "Approve", fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick = onReject,
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, WortArtColors.Wrong.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WortArtColors.Wrong),
            ) {
                Text(text = "Reject", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun UserSearch(
    state: AdminUiState,
    onQueryChange: (String) -> Unit,
    onFindUser: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val canSearch = state.query.isNotBlank() && !state.isWorking

    OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adminSearchField"),
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        placeholder = { Text("User ID or email", color = WortArtColors.TextMuted, fontSize = 17.sp) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                focusManager.clearFocus()
                if (canSearch) onFindUser()
            },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = WortArtColors.TextPrimary,
            unfocusedTextColor = WortArtColors.TextPrimary,
            focusedContainerColor = WortArtColors.Surface,
            unfocusedContainerColor = WortArtColors.Surface,
            cursorColor = WortArtColors.Gold,
            focusedBorderColor = WortArtColors.Gold,
            unfocusedBorderColor = WortArtColors.Surface,
        ),
    )

    Button(
        onClick = onFindUser,
        enabled = canSearch,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WortArtColors.Gold,
            contentColor = WortArtColors.OnGold,
            disabledContainerColor = WortArtColors.Surface,
            disabledContentColor = WortArtColors.TextMuted,
        ),
    ) {
        Text(text = "Find user", fontWeight = FontWeight.Medium, fontSize = 16.sp)
    }
}

@Composable
private fun FoundUserCard(
    user: UserProfile,
    status: String,
    enabled: Boolean,
    onExtendMembership: (Int) -> Unit,
    onRevokeMembership: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Gold.copy(alpha = 0.4f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = user.email,
            color = WortArtColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = "ID: ${user.uid}", color = WortArtColors.TextMuted, fontSize = 12.sp)
        Text(text = status, color = WortArtColors.Gold, fontSize = 14.sp)
        if (user.isAdmin) {
            Text(text = "Admin", color = WortArtColors.TextSecondary, fontSize = 13.sp)
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Enable or extend Premium",
            color = WortArtColors.TextSecondary,
            fontSize = 13.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EXTEND_OPTIONS.forEach { days ->
                Button(
                    onClick = { onExtendMembership(days) },
                    enabled = enabled,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WortArtColors.Gold,
                        contentColor = WortArtColors.OnGold,
                    ),
                ) {
                    Text(text = "+$days days", fontWeight = FontWeight.Medium)
                }
            }
        }
        OutlinedButton(
            onClick = onRevokeMembership,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, WortArtColors.Wrong.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WortArtColors.Wrong),
        ) {
            Text(text = "Remove Premium")
        }
    }
}

private val EXTEND_OPTIONS = listOf(7, 30, 365)
