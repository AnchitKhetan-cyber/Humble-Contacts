package com.humblesolutions.humblecontacts.ui.contacts

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblesolutions.humblecontacts.ui.components.BottomNavBar
import com.humblesolutions.humblecontacts.ui.components.NavTab


// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId:           String = "1",
    onBack:              () -> Unit = {},
    onNavigateToHome:    () -> Unit = {},
    onNavigateToContacts:() -> Unit = {},
    onNavigateToScan:    () -> Unit = {},
    onNavigateToNfc:     () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Notes", "Media")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                selected = NavTab.CONTACTS,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.HOME     -> onNavigateToHome()
                        NavTab.CONTACTS -> onNavigateToContacts()
                        NavTab.SCAN     -> onNavigateToScan()
                        NavTab.NFC      -> onNavigateToNfc()
                        NavTab.PROFILE  -> onNavigateToProfile()
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

            // ── Hero Header ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                // Back button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                // Avatar — straddling the header/body seam
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SC",
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Space for avatar overflow
            Spacer(Modifier.height(52.dp))

            // ── Name & Title ─────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Sarah Chen",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Product Designer • Figma",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Action buttons ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionButton(
                    icon = Icons.Outlined.Call,
                    label = "Call",
                    isPrimary = true,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Outlined.Email,
                    label = "Email",
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Outlined.Person,
                    label = "LinkedIn",
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )
                ActionButton(
                    icon = Icons.Outlined.Chat,
                    label = "Chat",
                    isPrimary = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Tab Row ──────────────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = index },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else androidx.compose.ui.graphics.Color.Transparent
                        ) {
                            Text(
                                title,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Tab Content ──────────────────────────────────────────────────
            when (selectedTab) {
                0 -> OverviewTab()
                1 -> NotesTab()
                2 -> MediaTab()
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}


// ─── Overview Tab ─────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {

        // Contact info card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                InfoRow("Phone",    "+1 (555) 123-4567", isLink = false)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow("Email",    "sarah.chen@figma.com", isLink = false)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                InfoRow("LinkedIn", "View Profile", isLink = true)
            }
        }

        Spacer(Modifier.height(16.dp))

        // First meeting card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "First Meeting Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("TechCrunch Disrupt", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("AI & Design Panel", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("March 15, 2026", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isLink: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontSize = 14.sp,
            color = if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isLink) FontWeight.Medium else FontWeight.Normal
        )
    }
}


// ─── Notes Tab ────────────────────────────────────────────────────────────────

@Composable
private fun NotesTab() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {

        // Note card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("TechCrunch Disrupt", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Mar 15, 2026", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Discussed collaboration on design systems. Very interested in our AI features. Follow up in 2 weeks.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Add note button
        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Text("Add New Note", modifier = Modifier.padding(vertical = 4.dp), fontSize = 15.sp)
        }
    }
}


// ─── Media Tab ────────────────────────────────────────────────────────────────

@Composable
private fun MediaTab() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(
            Icons.Outlined.PermMedia,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No media yet",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Photos and files shared with this contact will appear here.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}


// ─── Action Button ────────────────────────────────────────────────────────────

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isPrimary) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isPrimary) 0.dp else 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}