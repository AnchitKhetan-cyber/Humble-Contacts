package com.humblesolutions.humblecontacts.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblesolutions.humblecontacts.data.repository.ProfileRepository
import com.humblesolutions.humblecontacts.utils.generateQrBitmap

data class LinkedAccount(
    val title: String,
    val value: String
)

/**
 * Builds the content that gets encoded into the QR code for a given account.
 * Using proper URI schemes (tel:, mailto:, https://wa.me/, https://...) means
 * scanning the code with any standard QR scanner will redirect straight to
 * the relevant action (dialer, email composer, WhatsApp chat, or web page).
 */
fun qrContentFor(account: LinkedAccount): String {
    return when (account.title) {
        "Phone" -> "tel:${account.value}"
        "Email" -> "mailto:${account.value}"
        "WhatsApp" -> {
            val digitsOnly = account.value.filter { it.isDigit() }
            "https://wa.me/$digitsOnly"
        }
        "LinkedIn" -> {
            val url = account.value.trim()
            // Ensure URL starts with https://
            when {
                url.startsWith("https://") -> url
                url.startsWith("http://") -> url.replace("http://", "https://")
                url.startsWith("www.") -> "https://$url"
                else -> "https://www.$url"
            }
        }
        else -> account.value
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedAccountsScreen(
    onBack: () -> Unit
) {

    val repository = remember { ProfileRepository() }

    val accounts = remember {
        mutableStateListOf<LinkedAccount>()
    }

    var isLoading by remember { mutableStateOf(true) }

    var selectedAccount by remember { mutableStateOf<LinkedAccount?>(null) }

    LaunchedEffect(Unit) {

        isLoading = true

        val profile = repository.getCurrentUserProfile()

        accounts.clear()

        if (profile != null) {

            // Honour the per-field share settings (#20): omit fields the user
            // has switched off from the shareable QR list.
            val share = profile.shareSettings

            if (share.sharePhone && profile.phone.isNotBlank()) {
                val fullPhoneNumber = "${profile.countryCode}${profile.phone}"

                accounts.add(
                    LinkedAccount("Phone", fullPhoneNumber)
                )

                accounts.add(
                    LinkedAccount("WhatsApp", fullPhoneNumber)
                )
            }

            if (share.shareEmail && profile.email.isNotBlank()) {
                accounts.add(
                    LinkedAccount("Email", profile.email)
                )
            }

            if (share.shareLinkedIn && profile.linkedInUrl.isNotBlank()) {
                accounts.add(
                    LinkedAccount("LinkedIn", profile.linkedInUrl)
                )
            }
        }

        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Linked Accounts",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        when {
            isLoading -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            accounts.isEmpty() -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No linked accounts yet",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(accounts) { account ->
                        LinkedAccountCard(
                            account = account,
                            onClick = { selectedAccount = account }
                        )
                    }
                }
            }
        }
    }

    selectedAccount?.let { account ->
        QrCodeDialog(
            account = account,
            onDismiss = { selectedAccount = null }
        )
    }
}

@Composable
fun QrCodeDialog(
    account: LinkedAccount,
    onDismiss: () -> Unit
) {

    val content = remember(account) { qrContentFor(account) }

    val qrBitmap = remember(content) {
        generateQrBitmap(content, sizePx = 512)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                account.title,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code for ${account.title}",
                        modifier = Modifier
                            .size(220.dp)
                            .padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Scan to ${actionLabel(account.title)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    account.value,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun actionLabel(title: String): String = when (title) {
    "Phone" -> "call this number"
    "Email" -> "send an email"
    "WhatsApp" -> "open WhatsApp chat"
    "LinkedIn" -> "open LinkedIn profile"
    else -> "open this link"
}

@Composable
private fun LinkedAccountCard(
    account: LinkedAccount,
    onClick: () -> Unit
) {

    val (icon, accentColor) = when (account.title) {
        "Phone" -> Icons.Outlined.Call to MaterialTheme.colorScheme.primary
        "Email" -> Icons.Outlined.Email to MaterialTheme.colorScheme.tertiary
        "WhatsApp" -> Icons.Outlined.PhoneAndroid to MaterialTheme.colorScheme.secondary
        else -> Icons.Outlined.Link to MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 0.dp
        )
    ) {

        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            ),

            headlineContent = {
                Text(
                    account.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },

            supportingContent = {
                Text(
                    account.value,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },

            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },

            trailingContent = {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Connected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

        )
    }
}