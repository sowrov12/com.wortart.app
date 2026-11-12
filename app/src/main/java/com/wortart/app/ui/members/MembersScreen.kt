package com.wortart.app.ui.members

import com.wortart.app.ui.components.AppIcon
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wortart.app.audio.VoiceStatus
import com.wortart.app.domain.membership.Invitation
import com.wortart.app.domain.membership.InvitationStatus
import com.wortart.app.domain.membership.MemberBenefits
import com.wortart.app.domain.membership.MembershipPolicy
import com.wortart.app.domain.membership.ReferralCodes
import com.wortart.app.domain.membership.SupportConfig
import com.wortart.app.ui.components.MessageBanner
import com.wortart.app.ui.theme.WortArtColors
import com.wortart.app.ui.theme.WortArtFonts

/** Connects [MembersScreen] to its [MembersViewModel] and handles clipboard, sharing, e-mail and voice setup. */
@Composable
fun MembersRoute(
    viewModel: MembersViewModel,
    favoritesCount: Int,
    voiceStatus: VoiceStatus,
    onOpenAdmin: () -> Unit,
    onOpenFavorites: () -> Unit,
    modifier: Modifier = Modifier,
    settings: ProfileSettings = ProfileSettings(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Inside the app this is the Activity, which Google's account chooser needs.
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val appVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.0"
    }

    LaunchedEffect(state.user?.uid) {
        viewModel.refreshInvitationProgress()
    }

    MembersScreen(
        state = state,
        favoritesCount = favoritesCount,
        voiceStatus = voiceStatus,
        appVersion = appVersion,
        onSignIn = { viewModel.signIn(context) },
        onSignOut = { viewModel.signOut(context) },
        onDeleteAccount = { viewModel.deleteAccount(context) },
        onOpenAdmin = onOpenAdmin,
        onOpenFavorites = onOpenFavorites,
        settings = settings,
        onFixVoice = { openVoiceInstall(context) },
        onCopyReferralCode = { code ->
            clipboard.setText(AnnotatedString(code))
            Toast.makeText(context, "Invite code copied", Toast.LENGTH_SHORT).show()
        },
        onShareReferralCode = { code ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Learn German with WortArt! Use my invitation code $code and we both get free member days.",
                )
            }
            context.startActivity(Intent.createChooser(send, "Invite a friend"))
        },
        onApplyInviteCode = viewModel::applyInviteCode,
        onTryAnotherCode = viewModel::tryAnotherCode,
        onContactSupport = {
            val mail = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${SupportConfig.EMAIL}")).apply {
                putExtra(Intent.EXTRA_SUBJECT, "WortArt feedback")
            }
            try {
                context.startActivity(mail)
            } catch (_: Exception) {
                Toast.makeText(context, "No e-mail app found.", Toast.LENGTH_SHORT).show()
            }
        },
        onDismissMessage = viewModel::dismissMessage,
        modifier = modifier,
    )
}

/** Opens the system screen where a German text-to-speech voice can be installed. */
private fun openVoiceInstall(context: Context) {
    try {
        context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
    } catch (_: Exception) {
        try {
            context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Open Settings › Text-to-speech output to add a German voice.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}

/**
 * The Profile tab: account, favorites, offline status, support, about and, for members, the
 * benefits, invite code and admin entry. Stateless; every action is a callback.
 */
@Composable
fun MembersScreen(
    state: MembersUiState,
    favoritesCount: Int,
    voiceStatus: VoiceStatus,
    appVersion: String,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenFavorites: () -> Unit,
    onFixVoice: () -> Unit,
    onCopyReferralCode: (String) -> Unit,
    onShareReferralCode: (String) -> Unit,
    onApplyInviteCode: (String) -> Unit,
    onTryAnotherCode: () -> Unit,
    onContactSupport: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    supportEmail: String = SupportConfig.EMAIL,
    settings: ProfileSettings = ProfileSettings(),
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(
            isMember = state.isMemberActive,
            canDeleteAccount = state.isSignedIn && state.profile != null,
            onDeleteAccount = { showDeleteDialog = true },
        )

        state.message?.let { MessageBanner(text = it, onDismiss = onDismissMessage) }

        if (state.isBusy || state.isLoadingProfile) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = WortArtColors.Gold,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = if (state.isLoadingProfile) "Setting up your account…" else "Please wait…",
                    color = WortArtColors.TextSecondary,
                    fontSize = 14.sp,
                )
            }
        }

        when {
            !state.isConfigured -> NotConfiguredCard()
            !state.isSignedIn -> SignedOutCard(onSignIn = onSignIn, enabled = !state.isBusy)
            else -> {
                ProfileCard(state)
                if (state.isAdmin) {
                    PillButton(
                        text = "Admin panel",
                        icon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = WortArtColors.Gold) },
                        onClick = onOpenAdmin,
                    )
                }
            }
        }

        SettingsList(
            favoritesCount = favoritesCount,
            voiceStatus = voiceStatus,
            showSupport = supportEmail.isNotBlank(),
            showLogout = state.isSignedIn,
            logoutEnabled = !state.isBusy,
            onOpenFavorites = onOpenFavorites,
            onFixVoice = onFixVoice,
            onContactSupport = onContactSupport,
            onAbout = { showAbout = true },
            onLogout = onSignOut,
            settings = settings,
        )

        BenefitsCard()

        val profile = state.profile
        if (state.isSignedIn && profile != null) {
            InviteCard(
                code = profile.referralCode,
                totalReferrals = profile.totalReferrals,
                invitation = state.invitation,
                enabled = !state.isBusy,
                onCopy = { onCopyReferralCode(profile.referralCode) },
                onShare = { onShareReferralCode(profile.referralCode) },
                onApplyCode = onApplyInviteCode,
                onTryAnotherCode = onTryAnotherCode,
            )
        }

        if (supportEmail.isNotBlank()) {
            SupportCard(onContactSupport = onContactSupport)
        }
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteDialog = false
                onDeleteAccount()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
    if (showAbout) {
        AboutDialog(appVersion = appVersion, onDismiss = { showAbout = false })
    }
}

@Composable
private fun Header(isMember: Boolean, canDeleteAccount: Boolean, onDeleteAccount: () -> Unit) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Profile",
                    color = WortArtColors.TextPrimary,
                    fontSize = 28.sp,
                    fontFamily = WortArtFonts.Serif,
                    fontWeight = FontWeight.Bold,
                )
                if (isMember) {
                    Spacer(Modifier.size(8.dp))
                    AppIcon(name = "Crown", emoji = "👑", size = 26.dp)
                }
            }
            Text(
                text = "Your membership and support.",
                color = WortArtColors.TextSecondary,
                fontSize = 14.sp,
            )
        }

        if (canDeleteAccount) {
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.background(WortArtColors.Surface, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options",
                        tint = WortArtColors.TextPrimary,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(text = "Delete account", color = WortArtColors.Wrong) },
                        onClick = {
                            menuOpen = false
                            onDeleteAccount()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CardColumn(
    modifier: Modifier = Modifier,
    borderAlpha: Float = 0.25f,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Gold.copy(alpha = borderAlpha), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

@Composable
private fun PillButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, WortArtColors.Gold.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = WortArtColors.TextPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        icon()
        Spacer(Modifier.size(10.dp))
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BenefitsCard() {
    CardColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(WortArtColors.Gold.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = WortArtColors.Gold)
            }
            Spacer(Modifier.size(14.dp))
            Text(
                text = "Member benefits",
                color = WortArtColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        MemberBenefits.ALL.forEach { benefit ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = WortArtColors.Correct,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(12.dp))
                Text(text = benefit, color = WortArtColors.TextPrimary, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun NotConfiguredCard() {
    CardColumn {
        Text(
            text = "Accounts are not set up yet",
            color = WortArtColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Add the Firebase file google-services.json to the app to turn on sign-in " +
                "(see docs/FIREBASE_SETUP.md).",
            color = WortArtColors.TextSecondary,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun SignedOutCard(onSignIn: () -> Unit, enabled: Boolean) {
    CardColumn(borderAlpha = 0.5f) {
        Text(
            text = "Sign in to become a member",
            color = WortArtColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Your membership and invite code are tied to your Google account.",
            color = WortArtColors.TextSecondary,
            fontSize = 14.sp,
        )
        Button(
            onClick = onSignIn,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WortArtColors.Gold,
                contentColor = WortArtColors.OnGold,
            ),
        ) {
            Text(text = "Sign in with Google", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProfileCard(state: MembersUiState) {
    val email = state.user?.email ?: state.profile?.email.orEmpty()
    val name = state.user?.displayName?.takeIf { it.isNotBlank() } ?: email
    val initial = name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

    CardColumn(borderAlpha = if (state.isMemberActive) 0.6f else 0.25f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(WortArtColors.SurfaceDark, CircleShape)
                    .border(2.dp, WortArtColors.GoldGradient, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    color = WortArtColors.Gold,
                    fontSize = 28.sp,
                    fontFamily = WortArtFonts.Serif,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    color = WortArtColors.TextPrimary,
                    fontSize = 20.sp,
                    fontFamily = WortArtFonts.Serif,
                    fontWeight = FontWeight.Bold,
                )
                if (name != email) {
                    Text(text = email, color = WortArtColors.TextSecondary, fontSize = 14.sp)
                }
                Text(
                    text = if (state.isMemberActive) "Premium" else "Free",
                    color = if (state.isMemberActive) WortArtColors.OnGold else WortArtColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (state.isMemberActive) WortArtColors.Gold else WortArtColors.Elevated,
                            RoundedCornerShape(50),
                        )
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                )
            }
        }
        Text(
            text = state.statusText,
            color = if (state.isMemberActive) WortArtColors.Gold else WortArtColors.TextSecondary,
            fontSize = 15.sp,
        )
    }
}

/** The practice and language switches that are also shown on the profile page. */
data class ProfileSettings(
    val reminders: Boolean = true,
    val readFlashcards: Boolean = true,
    val showBengali: Boolean = true,
    val onReminders: (Boolean) -> Unit = {},
    val onReadFlashcards: (Boolean) -> Unit = {},
    val onShowBengali: (Boolean) -> Unit = {},
)

@Composable
private fun SettingsList(
    favoritesCount: Int,
    voiceStatus: VoiceStatus,
    showSupport: Boolean,
    showLogout: Boolean,
    logoutEnabled: Boolean,
    onOpenFavorites: () -> Unit,
    onFixVoice: () -> Unit,
    onContactSupport: () -> Unit,
    onAbout: () -> Unit,
    onLogout: () -> Unit,
    settings: ProfileSettings,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(WortArtColors.Surface)
            .border(1.dp, WortArtColors.Border, shape),
    ) {
        ProfileRow(
            emoji = "⭐",
            title = "Favorites ($favoritesCount)",
            chevron = true,
            onClick = onOpenFavorites,
        )
        RowDivider()
        ProfileRow(
            emoji = "📴",
            title = "Offline mode",
            subtitle = "All set for offline! Words, rules and progress are stored on this device.",
        )
        RowDivider()
        when (voiceStatus) {
            VoiceStatus.READY -> ProfileRow(
                emoji = "🔊",
                title = "German voice",
                subtitle = "Ready for pronunciation without internet.",
            )
            VoiceStatus.CHECKING -> ProfileRow(
                emoji = "🔊",
                title = "German voice",
                subtitle = "Checking…",
            )
            VoiceStatus.MISSING -> ProfileRow(
                emoji = "🔊",
                title = "German voice",
                subtitle = "Not installed. Tap to install it for offline audio.",
                chevron = true,
                onClick = onFixVoice,
            )
        }
        RowDivider()
        SwitchRow("🔔", "Reminders", settings.reminders, settings.onReminders)
        RowDivider()
        SwitchRow("🎧", "Read flashcards", settings.readFlashcards, settings.onReadFlashcards)
        RowDivider()
        SwitchRow("🌐", "Bangla language", settings.showBengali, settings.onShowBengali)
        if (showSupport) {
            RowDivider()
            ProfileRow(emoji = "💬", title = "Support & Feedback", chevron = true, onClick = onContactSupport)
        }
        RowDivider()
        ProfileRow(emoji = "ℹ️", title = "About App", chevron = true, onClick = onAbout)
        if (showLogout) {
            RowDivider()
            ProfileRow(
                emoji = "🚪",
                title = "Logout",
                onClick = if (logoutEnabled) onLogout else null,
            )
        }
    }
}

@Composable
private fun SwitchRow(emoji: String, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(Modifier.size(14.dp))
        Text(
            text = title,
            color = WortArtColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WortArtColors.OnGold,
                checkedTrackColor = WortArtColors.Gold,
            ),
        )
    }
}

@Composable
private fun ProfileRow(
    emoji: String,
    title: String,
    subtitle: String? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val tap = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(tap)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = WortArtColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            if (subtitle != null) {
                Text(text = subtitle, color = WortArtColors.TextSecondary, fontSize = 13.sp)
            }
        }
        if (chevron) {
            Text(text = "›", color = WortArtColors.TextMuted, fontSize = 24.sp)
        }
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WortArtColors.Border.copy(alpha = 0.6f)),
    )
}

@Composable
private fun AboutDialog(appVersion: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WortArtColors.Surface,
        title = { Text(text = "WortArt", color = WortArtColors.TextPrimary, fontFamily = WortArtFonts.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Version $appVersion", color = WortArtColors.Gold, fontWeight = FontWeight.SemiBold)
                Text(text = "German Grammar & Vocab Master", color = WortArtColors.TextPrimary)
                Text(
                    text = "Vocabulary, grammar lessons and your progress are stored on this device, " +
                        "so you can practice without a connection.",
                    color = WortArtColors.TextSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "Close", color = WortArtColors.Gold) }
        },
    )
}

@Composable
private fun InviteCard(
    code: String,
    totalReferrals: Int,
    invitation: Invitation?,
    enabled: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onApplyCode: (String) -> Unit,
    onTryAnotherCode: () -> Unit,
) {
    CardColumn {
        Text(
            text = "Invite friends",
            color = WortArtColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Invite a friend and you both get free member days.",
            color = WortArtColors.TextSecondary,
            fontSize = 14.sp,
        )

        // Step 1: share the app
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepNumber(1)
            Spacer(Modifier.size(14.dp))
            Text(
                text = "Download WortArt app",
                color = WortArtColors.TextPrimary,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(WortArtColors.Gold)
                    .clickable(onClick = onShare)
                    .semantics { contentDescription = "Share invite" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, tint = WortArtColors.OnGold)
            }
        }

        // Step 2: copy the code
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepNumber(2)
            Spacer(Modifier.size(14.dp))
            Text(
                text = "Copy your invitation code",
                color = WortArtColors.TextPrimary,
                fontSize = 16.sp,
            )
        }
        val chipShape = RoundedCornerShape(16.dp)
        Row(
            modifier = Modifier
                .padding(start = 46.dp)
                .clip(chipShape)
                .background(WortArtColors.SurfaceDark)
                .clickable(onClick = onCopy)
                .semantics { contentDescription = "Copy invite code" }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = code,
                color = WortArtColors.Gold,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.size(12.dp))
            CopyIcon(color = WortArtColors.TextPrimary, fill = WortArtColors.SurfaceDark)
        }

        Text(
            text = "Total referrals: $totalReferrals",
            color = WortArtColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )

        InvitationStatusSection(
            invitation = invitation,
            enabled = enabled,
            onApplyCode = onApplyCode,
            onTryAnotherCode = onTryAnotherCode,
        )
    }
}

@Composable
private fun StepNumber(number: Int) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(WortArtColors.Gold.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$number",
            color = WortArtColors.Gold,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** The friend's side: enter someone's code, or see what happened to the code already entered. */
@Composable
private fun InvitationStatusSection(
    invitation: Invitation?,
    enabled: Boolean,
    onApplyCode: (String) -> Unit,
    onTryAnotherCode: () -> Unit,
) {
    when (invitation?.status) {
        null -> {
            var entered by rememberSaveable { mutableStateOf("") }
            Text(
                text = "Have an invite code?",
                color = WortArtColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = entered,
                    onValueChange = { text ->
                        entered = text.uppercase().filter { it.isLetterOrDigit() }.take(ReferralCodes.LENGTH)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inviteCodeField"),
                    singleLine = true,
                    placeholder = { Text("Friend's code", color = WortArtColors.TextMuted) },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = WortArtColors.TextPrimary,
                        unfocusedTextColor = WortArtColors.TextPrimary,
                        cursorColor = WortArtColors.Gold,
                        focusedBorderColor = WortArtColors.Gold,
                        unfocusedBorderColor = WortArtColors.Gold.copy(alpha = 0.4f),
                    ),
                )
                Spacer(Modifier.size(10.dp))
                Button(
                    onClick = { onApplyCode(entered) },
                    enabled = enabled && entered.length == ReferralCodes.LENGTH,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WortArtColors.Gold,
                        contentColor = WortArtColors.OnGold,
                    ),
                ) {
                    Text(text = "Apply")
                }
            }
        }

        InvitationStatus.PENDING -> Text(
            text = "Code applied. We will activate your ${MembershipPolicy.REFERRAL_FRIEND_DAYS} days within 24 hours.",
            color = WortArtColors.TextSecondary,
            fontSize = 14.sp,
        )

        InvitationStatus.APPROVED -> Text(
            text = "Your invite code was approved. Enjoy your free days!",
            color = WortArtColors.Correct,
            fontSize = 14.sp,
        )

        InvitationStatus.REJECTED -> {
            Text(
                text = "Your invite code was not approved.",
                color = WortArtColors.Wrong,
                fontSize = 14.sp,
            )
            OutlinedButton(
                onClick = onTryAnotherCode,
                enabled = enabled,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WortArtColors.Gold),
            ) {
                Text(text = "Try another code")
            }
        }
    }
}

@Composable
private fun SupportCard(onContactSupport: () -> Unit) {
    CardColumn {
        Text(
            text = "Member activation",
            color = WortArtColors.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Enrolled in our course or community? Contact support to activate your access.",
            color = WortArtColors.TextSecondary,
            fontSize = 14.sp,
        )
        OutlinedButton(
            onClick = onContactSupport,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = WortArtColors.Gold),
        ) {
            Text(text = "Contact support")
        }
    }
}

/** Two overlapping pages, drawn on a Canvas so no icon library is needed. */
@Composable
private fun CopyIcon(color: Color, fill: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val w = size.width
        val stroke = Stroke(width = w * 0.09f)
        val corner = CornerRadius(w * 0.08f)
        val page = Size(w * 0.56f, w * 0.64f)

        drawRoundRect(color = color, topLeft = Offset(w * 0.32f, w * 0.06f), size = page, cornerRadius = corner, style = stroke)
        drawRoundRect(color = fill, topLeft = Offset(w * 0.12f, w * 0.30f), size = page, cornerRadius = corner, style = Fill)
        drawRoundRect(color = color, topLeft = Offset(w * 0.12f, w * 0.30f), size = page, cornerRadius = corner, style = stroke)
    }
}

@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WortArtColors.Surface,
        title = {
            Text(text = "Delete account?", color = WortArtColors.TextPrimary)
        },
        text = {
            Text(
                text = "This permanently deletes your account, membership and invite code, " +
                    "and resets your progress and favorites on this device. " +
                    "You will be asked to confirm your Google account. This cannot be undone.",
                color = WortArtColors.TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete", color = WortArtColors.Wrong, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = WortArtColors.Gold)
            }
        },
    )
}
