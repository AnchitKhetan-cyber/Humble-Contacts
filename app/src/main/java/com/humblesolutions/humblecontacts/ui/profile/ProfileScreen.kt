package com.humblesolutions.humblecontacts.ui.profile

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.humblesolutions.humblecontacts.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.humblesolutions.humblecontacts.data.model.UserProfile
import com.humblesolutions.humblecontacts.data.model.ShareSettings
import com.humblesolutions.humblecontacts.data.repository.ProfileRepository
import kotlinx.coroutines.launch
import com.humblesolutions.humblecontacts.ui.home.HomeViewModel
import com.humblesolutions.humblecontacts.ui.components.BottomNavBar
import com.humblesolutions.humblecontacts.ui.components.NavTab
import com.humblesolutions.humblecontacts.ui.settings.SettingsViewModel


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToScan: () -> Unit = {},
    onNavigateToNfc: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDeleteAccount: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToLinkedAccounts: () -> Unit = {},
    onLogout: () -> Unit = {}
) {

    val viewModel: HomeViewModel = viewModel()
    val user = FirebaseAuth.getInstance().currentUser

    // Firestore profile — the source of truth for phone users, whose Firebase Auth
    // account carries no email (and sometimes no display name).
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: userProfile?.name?.takeIf { it.isNotBlank() }
        ?: "User"
    val email = user?.email?.takeIf { it.isNotBlank() }
        ?: userProfile?.email
        ?: ""

    val context = LocalContext.current

    val initials = displayName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    val photoUrl = user?.photoUrl

    val settingsViewModel: SettingsViewModel = viewModel()

    val profileViewModel: ProfileViewModel = viewModel()

    var showLogoutDialog  by remember { mutableStateOf(false) }
    var selectedQrAccount by remember { mutableStateOf<LinkedAccount?>(null) }
    val qrAccounts = remember { mutableStateListOf<LinkedAccount>() }

    val loadScope = rememberCoroutineScope()

    // Rebuilds the shareable QR list from a profile, honouring the per-field
    // share settings (#20) — fields switched off are omitted. Reused by the
    // resume-load and by the privacy toggles so the list updates immediately.
    val rebuildQrAccounts: (UserProfile?) -> Unit = { profile ->
        qrAccounts.clear()
        if (profile != null) {
            val share = profile.shareSettings
            if (share.sharePhone && profile.phone.isNotBlank()) {
                val full = "${profile.countryCode}${profile.phone}"
                qrAccounts.add(LinkedAccount("Phone", full))
                qrAccounts.add(LinkedAccount("WhatsApp", full))
            }
            if (share.shareEmail && profile.email.isNotBlank())
                qrAccounts.add(LinkedAccount("Email", profile.email))
            if (share.shareLinkedIn && profile.linkedInUrl.isNotBlank())
                qrAccounts.add(LinkedAccount("LinkedIn", profile.linkedInUrl))
        }
    }

    // Reload on every resume so edits made on the Edit Profile screen are reflected
    // when the user navigates back here.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        loadScope.launch {
            val repo    = ProfileRepository()
            val profile = repo.getCurrentUserProfile()
            userProfile = profile
            rebuildQrAccounts(profile)
        }
    }

    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                selected = NavTab.PROFILE,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.HOME     -> onNavigateToHome()
                        NavTab.CONTACTS -> onNavigateToContacts()
                        NavTab.SCAN     -> onNavigateToScan()
                        else            -> {}
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Profile hero ─────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (photoUrl != null) {

                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                        )

                    } else {

                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        email,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat(value = viewModel.totalContacts.toString(), label = "Contacts")
                        VerticalDivider(
                            modifier = Modifier.height(32.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ProfileStat(value = viewModel.thisMonthCount.toString(), label = "This Month")
                        VerticalDivider(
                            modifier = Modifier.height(32.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ProfileStat(value = viewModel.uniqueEventsCount.toString(), label = "Events")
                    }
                }
            }

            // ── QR icon cards ─────────────────────────────────────────────────
            if (qrAccounts.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        "MY QR CODES",
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        qrAccounts.forEach { account ->
                            QrIconCard(
                                account = account,
                                onClick = { selectedQrAccount = account }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                SettingsSection(title = "ACCOUNT") {
                    SettingsRow(
                        icon = Icons.Outlined.Person,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Edit Profile",
                        subtitle = "Update your information",
                        onClick = onNavigateToEditProfile
                    )
                }

                Spacer(Modifier.height(16.dp))

                SettingsSection(title = "PRIVACY & DATA") {
                    SettingsRow(
                        icon = Icons.Outlined.Download,
                        iconBg = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Export Data",
                        subtitle = "Download all contacts as CSV",
                        onClick = {
                            viewModel.exportContacts(context)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        icon       = Icons.Outlined.Delete,
                        iconBg     = MaterialTheme.colorScheme.errorContainer,
                        iconTint   = MaterialTheme.colorScheme.error,
                        title      = "Delete Account",
                        subtitle   = "Permanently remove your account",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick    = onNavigateToDeleteAccount
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Per-field sharing (privacy) toggles (#20) ────────────────
                SettingsSection(title = "SHARING") {
                    val share = userProfile?.shareSettings ?: ShareSettings()

                    // Persist a changed ShareSettings and refresh the QR list live.
                    val applyShare: (ShareSettings) -> Unit = { updated ->
                        val newProfile = userProfile?.copy(shareSettings = updated)
                        userProfile = newProfile
                        rebuildQrAccounts(newProfile)
                        loadScope.launch { ProfileRepository().updateShareSettings(updated) }
                    }

                    SettingsToggleRow(
                        icon            = Icons.Outlined.Phone,
                        iconBg          = MaterialTheme.colorScheme.primaryContainer,
                        iconTint        = MaterialTheme.colorScheme.primary,
                        title           = "Share phone number",
                        subtitle        = "Include your phone in shared cards",
                        checked         = share.sharePhone,
                        onCheckedChange = { applyShare(share.copy(sharePhone = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsToggleRow(
                        icon            = Icons.Outlined.Email,
                        iconBg          = MaterialTheme.colorScheme.primaryContainer,
                        iconTint        = MaterialTheme.colorScheme.primary,
                        title           = "Share email",
                        subtitle        = "Include your email in shared cards",
                        checked         = share.shareEmail,
                        onCheckedChange = { applyShare(share.copy(shareEmail = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsToggleRow(
                        icon            = Icons.Outlined.Link,
                        iconBg          = MaterialTheme.colorScheme.primaryContainer,
                        iconTint        = MaterialTheme.colorScheme.primary,
                        title           = "Share LinkedIn",
                        subtitle        = "Include your LinkedIn in shared cards",
                        checked         = share.shareLinkedIn,
                        onCheckedChange = { applyShare(share.copy(shareLinkedIn = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }

                Spacer(Modifier.height(16.dp))

                SettingsSection(title = "APP SETTINGS") {
                    // ✅ Fixed: pass ProfileScreen's darkMode and onDarkModeChange directly
                    SettingsToggleRow(
                        icon            = Icons.Outlined.DarkMode,
                        iconBg          = MaterialTheme.colorScheme.primaryContainer,
                        iconTint        = MaterialTheme.colorScheme.primary,
                        title           = "Dark Mode",
                        subtitle        = "Toggle dark theme",
                        checked         = darkMode,
                        onCheckedChange = onDarkModeChange
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsToggleRow(
                        icon            = Icons.Outlined.Notifications,
                        iconBg          = MaterialTheme.colorScheme.primaryContainer,
                        iconTint        = MaterialTheme.colorScheme.primary,
                        title           = "Notifications",
                        subtitle        = "Get reminders and updates",
                        checked         = notificationsEnabled,
                        onCheckedChange = { settingsViewModel.setNotifications(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        showLogoutDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.error
                        )
                    )
                ) {
                    Icon(
                        Icons.Outlined.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Log Out",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // NOTE: account deletion is handled by DeleteAccountScreen (reached via
    // onNavigateToDeleteAccount), which performs the required re-authentication
    // before deleting. The old inline delete dialog here was unreachable
    // (showDeleteDialog was never set to true) and bypassed re-auth, so it was
    // removed as part of ticket #1.

    if (showLogoutDialog) {

        AlertDialog(

            onDismissRequest = {
                showLogoutDialog = false
            },

            icon = {
                Icon(
                    Icons.Outlined.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },

            title = {
                Text("Log Out")
            },

            text = {
                Text(
                    "Are you sure you want to log out?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        showLogoutDialog = false

                        onLogout()

                    }
                ) {

                    Text(
                        "Log Out",
                        color = MaterialTheme.colorScheme.error
                    )

                }

            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showLogoutDialog = false
                    }
                ) {

                    Text("Cancel")

                }

            }

        )
    }

    selectedQrAccount?.let { account ->
        QrCodeDialog(account = account, onDismiss = { selectedQrAccount = null })
    }
}


// ─── QR Icon Card ─────────────────────────────────────────────────────────────

@Composable
private fun QrIconCard(
    account: LinkedAccount,
    onClick: () -> Unit
) {
    val (icon, bg, tint) = when (account.title) {
        "Phone"    -> Triple(Icons.Default.Phone,         Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "Email"    -> Triple(Icons.Default.Email,        Color(0xFFE3F2FD), Color(0xFF1565C0))
        "WhatsApp" -> Triple(Icons.Default.Whatsapp, Color(0xFFE8F5E9), Color(0xFF1B5E20))
        "LinkedIn" -> Triple(Icons.Default.Link,         Color(0xFFE3F2FD), Color(0xFF0D47A1))
        else       -> Triple(Icons.Outlined.Link,         MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = account.title,
            tint               = tint,
            modifier           = Modifier.size(26.dp)
        )
    }
}


// ─── Settings Section ─────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        title,
        fontSize     = 12.sp,
        fontWeight   = FontWeight.SemiBold,
        color        = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp
    )
    Spacer(Modifier.height(8.dp))
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        color           = MaterialTheme.colorScheme.surface,
        tonalElevation  = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column { content() }
    }
}


// ─── Settings Row ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconBg: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = titleColor)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}


// ─── Settings Toggle Row ──────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconBg: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // ✅ Fixed: removed local `var state` — use `checked` prop directly so the
    //    switch always reflects the real source of truth (MainActivity's darkMode state)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}


// ─── Profile Stat ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize   = 20.sp,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            fontSize = 12.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}