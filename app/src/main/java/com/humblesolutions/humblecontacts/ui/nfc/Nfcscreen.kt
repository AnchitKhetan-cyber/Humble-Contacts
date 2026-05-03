package com.humblesolutions.humblecontacts.ui.nfc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblesolutions.humblecontacts.ui.components.BottomNavBar
import com.humblesolutions.humblecontacts.ui.components.NavTab


// ─── Screen ───────────────────────────────────────────────────────────────────

enum class NfcScreenState { SHARE, SUCCESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcScreen(
    onBack:              () -> Unit = {},
    onNavigateToHome:    () -> Unit = {},
    onNavigateToContacts:() -> Unit = {},
    onNavigateToScan:    () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    var screenState   by remember { mutableStateOf(NfcScreenState.SHARE) }
    var selectedShare by remember { mutableStateOf("Contact Only") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("NFC Exchange", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                selected = NavTab.NFC,
                onTabSelected = { tab ->
                    when (tab) {
                        NavTab.HOME     -> onNavigateToHome()
                        NavTab.CONTACTS -> onNavigateToContacts()
                        NavTab.SCAN     -> onNavigateToScan()
                        NavTab.PROFILE  -> onNavigateToProfile()
                        else            -> {}
                    }
                }
            )
        }
    ) { padding ->
        when (screenState) {
            NfcScreenState.SHARE   -> ShareView(
                selectedShare = selectedShare,
                onSelectShare = { selectedShare = it },
                onStartNfc    = { screenState = NfcScreenState.SUCCESS },
                modifier      = Modifier.padding(padding)
            )
            NfcScreenState.SUCCESS -> SuccessView(
                onDone   = onNavigateToHome,
                modifier = Modifier.padding(padding)
            )
        }
    }
}


// ─── Share View ───────────────────────────────────────────────────────────────

@Composable
private fun ShareView(
    selectedShare: String,
    onSelectShare: (String) -> Unit,
    onStartNfc:    () -> Unit,
    modifier:      Modifier = Modifier
) {
    val shareOptions = listOf(
        "Full Profile"   to "Share all contact details, notes, and media",
        "Contact Only"   to "Share basic contact information only",
        "Card Only"      to "Share digital business card"
    )

    val recentExchanges = listOf(
        "DP" to "David Park" to ("Stripe • 2 hours ago"),
        "LW" to "Lisa Wang"  to ("Shopify • Yesterday"),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        // ── Phone icon ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PhoneAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Share Your Contact",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Choose what to share, then tap phones together",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // ── Share options ────────────────────────────────────────────────────
        shareOptions.forEach { (title, desc) ->
            val isSelected = selectedShare == title
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectShare(title) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                border = if (isSelected)
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                else null,
                tonalElevation = 1.dp,
                shadowElevation = if (isSelected) 0.dp else 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        desc,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(8.dp))

        // ── Start NFC button ─────────────────────────────────────────────────
        Button(
            onClick = onStartNfc,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor   = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text("Start NFC Exchange", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(28.dp))

        // ── Recent exchanges ─────────────────────────────────────────────────
        Text(
            "Recent Exchanges",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))

        recentExchanges.forEach { (pair, meta) ->
            val (initials, name) = pair
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(meta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}


// ─── Success View ─────────────────────────────────────────────────────────────

@Composable
private fun SuccessView(
    onDone:   () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Contact Shared!",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Successfully exchanged information",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        // Shared contact card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("MJ", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Marcus Johnson", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Engineering Lead • Linear", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Phone", "Email", "LinkedIn").forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                tag,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor   = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}