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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.ui.components.BottomNavBar
import com.humblesolutions.humblecontacts.ui.components.NavTab
import kotlinx.coroutines.tasks.await


// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId:            String = "",
    onBack:               () -> Unit = {},
    onNavigateToHome:     () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToScan:     () -> Unit = {},
    onNavigateToNfc:      () -> Unit = {},
    onNavigateToProfile:  () -> Unit = {}
) {
    // ── Load the real contact from Firestore ──────────────────────────────────
    var contact by remember { mutableStateOf<Contact?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(contactId) {
        if (contactId.isNotBlank()) {
            try {
                val doc = Firebase.firestore
                    .collection("contacts")
                    .document(contactId)
                    .get()
                    .await()
                contact = doc.toObject(Contact::class.java)
            } catch (_: Exception) { }
        }
        isLoading = false
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Notes", "Media")
    val context = LocalContext.current

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

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val c = contact
        if (c == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Contact not found", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onBack) { Text("Go back") }
                }
            }
            return@Scaffold
        }

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

                // Avatar straddling header/body seam
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
                        c.initials,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(52.dp))

            // ── Name & Title ─────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    c.fullName,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                val subtitle = listOf(c.jobRole, c.company).filter { it.isNotBlank() }.joinToString(" • ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (c.industry.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            c.industry,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Action buttons ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (c.phone.isNotBlank()) {
                    ActionButton(
                        icon = Icons.Outlined.Call,
                        label = "Call",
                        isPrimary = true,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}"))
                            context.startActivity(intent)
                        }
                    )
                }
                if (c.email.isNotBlank()) {
                    ActionButton(
                        icon = Icons.Outlined.Email,
                        label = "Email",
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${c.email}"))
                            context.startActivity(intent)
                        }
                    )
                }
                if (c.phone.isNotBlank()) {
                    ActionButton(
                        icon = Icons.Outlined.Chat,
                        label = "Chat",
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${c.phone}"))
                            context.startActivity(intent)
                        }
                    )
                }
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

            when (selectedTab) {
                0 -> OverviewTab(contact = c)
                1 -> NotesTab(notes = c.conversationNotes, eventName = c.eventName, metOn = c.metOn)
                2 -> MediaTab()
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}


// ─── Overview Tab — real data ─────────────────────────────────────────────────

@Composable
private fun OverviewTab(contact: Contact) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {

        // Contact info card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (contact.phone.isNotBlank()) {
                    InfoRow("Phone", contact.phone, isLink = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (contact.email.isNotBlank()) {
                    InfoRow("Email", contact.email, isLink = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (contact.company.isNotBlank()) {
                    InfoRow("Company", contact.company, isLink = false)
                }
                if (contact.phone.isBlank() && contact.email.isBlank() && contact.company.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No contact info available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Meeting details card
        if (contact.eventName.isNotBlank() || contact.meetingLocation.isNotBlank() || contact.metOn.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
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
                    if (contact.eventName.isNotBlank() || contact.meetingLocation.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                if (contact.eventName.isNotBlank())
                                    Text(contact.eventName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (contact.meetingLocation.isNotBlank())
                                    Text(contact.meetingLocation, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    if (contact.metOn.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(contact.metOn, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // Tags
        if (contact.tags.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tags", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        contact.tags.forEach { tag ->
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    tag,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
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


// ─── Notes Tab — real data ────────────────────────────────────────────────────

@Composable
private fun NotesTab(notes: String, eventName: String, metOn: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (eventName.isNotBlank() || metOn.isNotBlank()) {
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
                            if (eventName.isNotBlank())
                                Text(eventName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (metOn.isNotBlank())
                                Text(metOn, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (notes.isNotBlank()) {
                    Text(
                        notes,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                } else {
                    Text(
                        "No notes added for this contact.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Note")
        }
    }
}


// ─── Media Tab ────────────────────────────────────────────────────────────────

@Composable
private fun MediaTab() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("No media yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
    }
}


// ─── Action Button ────────────────────────────────────────────────────────────

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isPrimary) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isPrimary) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}