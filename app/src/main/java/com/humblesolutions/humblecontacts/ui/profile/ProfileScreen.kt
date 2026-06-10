package com.humblesolutions.humblecontacts.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.humblesolutions.humblecontacts.ui.home.HomeViewModel
import com.humblesolutions.humblecontacts.ui.components.BottomNavBar
import com.humblesolutions.humblecontacts.ui.components.NavTab
import com.humblesolutions.humblecontacts.ui.settings.SettingsViewModel


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
    onLogout: () -> Unit = {}
) {

    val viewModel: HomeViewModel = viewModel()
    val user = FirebaseAuth.getInstance().currentUser

    val displayName = user?.displayName ?: "User"
    val email = user?.email ?: ""

    val context = LocalContext.current

    val initials = displayName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()

    val photoUrl = user?.photoUrl

    val settingsViewModel: SettingsViewModel = viewModel()

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
                        NavTab.NFC      -> onNavigateToNfc()
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

            Spacer(Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                SettingsSection(title = "ACCOUNT") {
                    SettingsRow(
                        icon     = Icons.Outlined.Person,
                        iconBg   = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title    = "Edit Profile",
                        subtitle = "Update your information",
                        onClick  = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        icon     = Icons.Outlined.Lock,
                        iconBg   = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title    = "Change Password",
                        subtitle = "Update your password",
                        onClick  = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        icon     = Icons.Outlined.Link,
                        iconBg   = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title    = "Linked Accounts",
                        subtitle = "Google, LinkedIn",
                        onClick  = {}
                    )
                }

                Spacer(Modifier.height(16.dp))

                SettingsSection(title = "PRIVACY & DATA") {
                    SettingsRow(
                        icon     = Icons.Outlined.Shield,
                        iconBg   = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title    = "Data Visibility",
                        subtitle = "Control who sees your data",
                        onClick  = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
                        onClick    = {}
                    )
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
                        icon = Icons.Outlined.Notifications,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Notifications",
                        subtitle = "Follow-up reminders",
                        checked = notificationsEnabled,
                        onCheckedChange = {
                            settingsViewModel.setNotifications(it)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        icon     = Icons.Outlined.Help,
                        iconBg   = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title    = "Help & Support",
                        subtitle = "FAQs and contact support",
                        onClick  = {}
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick  = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                    )
                ) {
                    Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(32.dp))
            }
        }
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