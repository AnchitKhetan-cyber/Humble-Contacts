package com.humblesolutions.humblecontacts.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblesolutions.humblecontacts.ui.components.BottomNavBar
import com.humblesolutions.humblecontacts.ui.components.NavTab


// ─── Data model ───────────────────────────────────────────────────────────────

data class Contact(
    val id: String,
    val name: String,
    val role: String,
    val company: String,
    val metOn: String,
    val tags: List<String>,
    val initials: String
)


// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onNavigateToContact:  (String) -> Unit = {},
    onNavigateToHome:     () -> Unit = {},
    onNavigateToScan:     () -> Unit = {},
    onNavigateToNfc:      () -> Unit = {},
    onNavigateToProfile:  () -> Unit = {},
    onNavigateToAdd:      () -> Unit = {}
) {
    val contacts = listOf(
        Contact("1", "Sarah Chen",       "Product Designer", "Figma",   "Mar 15, 2026", listOf("Design", "AI/ML"),        "SC"),
        Contact("2", "Marcus Johnson",   "Engineering Lead", "Linear",  "Mar 10, 2026", listOf("Engineering", "SaaS"),    "MJ"),
        Contact("3", "Elena Rodriguez",  "VP of Marketing",  "Notion",  "Feb 28, 2026", listOf("Marketing", "Growth"),   "ER"),
        Contact("4", "David Park",       "Founder & CEO",    "Stripe",  "Feb 20, 2026", listOf("Finance", "SaaS"),       "DP"),
        Contact("5", "Lisa Wang",        "Product Manager",  "Shopify", "Feb 15, 2026", listOf("Product", "E-commerce"), "LW"),
    )

    val filterTabs = listOf("All", "By Industry", "By Event", "By Date")
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery    by remember { mutableStateOf("") }

    val filtered = contacts.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = CircleShape
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add contact")
            }
        },
        bottomBar = {
            BottomNavBar(
                selected = NavTab.CONTACTS,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.HOME    -> onNavigateToHome()
                        NavTab.SCAN    -> onNavigateToScan()
                        NavTab.NFC     -> onNavigateToNfc()
                        NavTab.PROFILE -> onNavigateToProfile()
                        else           -> {}
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "All Contacts",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Outlined.FilterList,
                        contentDescription = "Filter",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp)
                    )
                }
            }

            // ── Search ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text("Search contacts...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedBorderColor    = androidx.compose.ui.graphics.Color.Transparent,
                    focusedBorderColor      = MaterialTheme.colorScheme.primary,
                ),
                singleLine = true
            )

            Spacer(Modifier.height(14.dp))

            // ── Filter chips ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTabs.forEach { tab ->
                    val isSelected = tab == selectedFilter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedFilter = tab }
                    ) {
                        Text(
                            tab,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── List ─────────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { contact ->
                    ContactCard(
                        contact = contact,
                        onClick = { onNavigateToContact(contact.id) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}


// ─── Contact Card ─────────────────────────────────────────────────────────────

@Composable
private fun ContactCard(
    contact: Contact,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    contact.initials,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${contact.role} • ${contact.company}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Met on ${contact.metOn}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    contact.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
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