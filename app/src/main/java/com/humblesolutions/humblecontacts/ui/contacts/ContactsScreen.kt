package com.humblesolutions.humblecontacts.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.humblecontacts.ui.components.BottomNavBar
import com.humblesolutions.humblecontacts.ui.components.NavTab
import com.humblesolutions.humblecontacts.data.model.Contact


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
    val viewModel: ContactViewModel = viewModel()
    val focusManager = LocalFocusManager.current

    val filterTabs = listOf("All", "Favourites", "By Industry", "By Event", "By Date")
    // Chips that filter to a value the user picks from a dropdown.
    val dimensionTabs = setOf("By Industry", "By Event", "By Date")
    var selectedFilter by remember { mutableStateOf("All") }
    // The value chosen from a dimension chip's dropdown (null = none picked).
    var selectedFilterValue by remember { mutableStateOf<String?>(null) }
    // Which dimension chip's dropdown is currently open (null = none).
    var dropdownFor by remember { mutableStateOf<String?>(null) }
    var searchQuery    by remember { mutableStateOf("") }

    val filtered by remember(viewModel.contacts, searchQuery, selectedFilter, selectedFilterValue) {
        mutableStateOf(
            viewModel.filtered(searchQuery, selectedFilter, selectedFilterValue)
                .sortedByDescending { it.favourite }
        )
    }

    // Cursor pagination (ticket #25): as the list nears its end, ask the
    // ViewModel to load the next page (it grows the single listener's limit).
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 5
        }
    }
    // Scrolling the results is a "done typing" signal — drop focus so the
    // keyboard gets out of the way of the list the user is now reading.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) focusManager.clearFocus()
    }

    LaunchedEffect(shouldLoadMore) {
        // Only auto-page the full list. When a search/filter is active the view is
        // page-scoped by design (#25 decision), so we don't keep loading pages to
        // chase matches — that's what the P2-6 search backend is for.
        if (shouldLoadMore && selectedFilter == "All" && searchQuery.isBlank()) {
            viewModel.loadMore()
        }
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
                // Tap on empty space clears focus and dismisses the keyboard.
                // Interactive children consume their own taps, so this only
                // fires for taps outside a field/chip/card.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
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
                    when {
                        selectedFilter == "All" -> "All Contacts"
                        selectedFilterValue != null -> selectedFilterValue!!
                        else -> selectedFilter
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
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
                    unfocusedBorderColor    = Color.Transparent,
                    focusedBorderColor      = MaterialTheme.colorScheme.primary,
                ),
                // Filtering is live, so "Search" just confirms and closes the
                // keyboard rather than kicking off a query.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                singleLine = true
            )

            Spacer(Modifier.height(14.dp))

            // ── Filter chips ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterTabs.forEach { tab ->
                    val isSelected = tab == selectedFilter
                    val isDimension = tab in dimensionTabs
                    // Distinct values available for a dimension chip's dropdown.
                    val values = when (tab) {
                        "By Industry" -> viewModel.industries()
                        "By Event" -> viewModel.events()
                        "By Date" -> viewModel.meetingDates()
                        else -> emptyList()
                    }

                    Box {
                        Surface(
                            onClick = {
                                when {
                                    !isDimension -> {
                                        // All / Favourites: plain toggle, clear any picked value.
                                        selectedFilter = tab
                                        selectedFilterValue = null
                                        dropdownFor = null
                                    }
                                    values.isEmpty() -> {
                                        // No data behind this dimension yet — select it so the
                                        // list shows the empty state for it.
                                        selectedFilter = tab
                                        selectedFilterValue = null
                                        dropdownFor = null
                                    }
                                    else -> dropdownFor = tab   // open the value picker
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (tab == "Favourites") {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFFFFD700)
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = if (isSelected && selectedFilterValue != null)
                                        "$tab: $selectedFilterValue"
                                    else tab,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold
                                                 else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isDimension) {
                                    Icon(
                                        Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (isDimension) {
                            DropdownMenu(
                                expanded = dropdownFor == tab,
                                onDismissRequest = { dropdownFor = null }
                            ) {
                                values.forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text(value) },
                                        onClick = {
                                            selectedFilter = tab
                                            selectedFilterValue = value
                                            dropdownFor = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── List ─────────────────────────────────────────────────────────
            if (filtered.isEmpty() && selectedFilter != "All") {
                // Per-filter empty state. Favourites keeps its star + hint; the
                // dimension chips explain that no data exists for them yet.
                val (emptyIcon, emptyTitle, emptyHint) = when (selectedFilter) {
                    "Favourites" -> Triple(
                        Icons.Filled.Star,
                        "No favourites yet",
                        "Tap the ★ on any contact to add it here"
                    )
                    "By Industry" -> Triple(
                        Icons.Outlined.Business,
                        "No industries yet",
                        "Add an industry to a contact to filter by it"
                    )
                    "By Event" -> Triple(
                        Icons.Outlined.Event,
                        "No events yet",
                        "Contacts captured at an event will appear here"
                    )
                    "By Date" -> Triple(
                        Icons.Outlined.CalendarMonth,
                        "No dated contacts yet",
                        "Contacts with a meeting date will appear here"
                    )
                    else -> Triple(
                        Icons.Outlined.FilterList,
                        "Nothing here",
                        "No contacts match this filter"
                    )
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            emptyIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            emptyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            emptyHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.contactId }) { contact ->
                        ContactCard(
                            contact   = contact,
                            onClick   = { onNavigateToContact(contact.contactId) },
                            onFavouriteClick = { viewModel.toggleFavourite(contact) }
                        )
                    }
                    if (viewModel.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}


// ─── Contact Card ─────────────────────────────────────────────────────────────

@Composable
fun ContactCard(
    contact: Contact,
    onClick: () -> Unit,
    onFavouriteClick: (() -> Unit)? = null
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
                    contact.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${contact.jobRole} • ${contact.company}",
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

            // Favourite star
            if (onFavouriteClick != null) {
                IconButton(
                    onClick = onFavouriteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = if (contact.favourite) "Remove from favourites"
                                             else "Add to favourites",
                        tint = if (contact.favourite) Color(0xFFFFD700)
                               else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
