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
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.humblesolutions.humblecontacts.data.model.Contact
import com.humblesolutions.humblecontacts.data.model.ContactNote
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.humblecontacts.ui.components.BottomNavBar
import com.humblesolutions.humblecontacts.ui.components.NavTab
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID


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
    onNavigateToProfile:  () -> Unit = {},
    contactViewModel:     ContactViewModel = viewModel()
) {
    // ── Load the real contact from Firestore ──────────────────────────────────
    var contact by remember { mutableStateOf<Contact?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    var showAddNoteDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteNoteDialog by remember {
        mutableStateOf(false)
    }

    var noteToDelete by remember {
        mutableStateOf<ContactNote?>(null)
    }

    var showEditNoteDialog by remember {
        mutableStateOf(false)
    }

    var noteToEdit by remember {
        mutableStateOf<ContactNote?>(null)
    }

    var selectedMedia by remember {
        mutableStateOf<String?>(null)
    }

    var showDeleteMediaDialog by remember {
        mutableStateOf(false)
    }

    var mediaToDelete by remember {
        mutableStateOf<String?>(null)
    }

    val context = LocalContext.current

    val galleryLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri == null) return@rememberLauncherForActivityResult

            contact?.let { currentContact ->

                scope.launch {

                    try {

                        val fileName = "${UUID.randomUUID()}.jpg"

                        val storageRef = Firebase.storage.reference
                            .child("contacts")
                            .child(currentContact.contactId)
                            .child("media")
                            .child(fileName)

                        storageRef.putFile(uri).await()

                        val downloadUrl = storageRef
                            .downloadUrl
                            .await()
                            .toString()

                        val updatedMedia = currentContact.media + downloadUrl

                        Firebase.firestore
                            .collection("contacts")
                            .document(currentContact.contactId)
                            .update(
                                "media",
                                updatedMedia
                            )
                            .await()

                        contact = currentContact.copy(
                            media = updatedMedia
                        )

                        Toast.makeText(
                            context,
                            "Media uploaded successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // We'll continue here in the next step.

                    } catch (e: Exception) {

                        Toast.makeText(
                            context,
                            e.localizedMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    LaunchedEffect(contactId) {
        if (contactId.isNotBlank()) {
            try {
                val doc = Firebase.firestore
                    .collection("contacts")
                    .document(contactId)
                    .get()
                    .await()
                try {
                    contact = doc.toObject(Contact::class.java)
                } catch (e: Exception) {
                    android.util.Log.e(
                        "CONTACT_DETAIL",
                        "Invalid contact",
                        e
                    )
                    contact = null
                }
            } catch (_: Exception) { }
        }
        isLoading = false
    }

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

                IconButton(
                    onClick = {
                        showDeleteDialog = true
                    },
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete Contact",
                            tint = MaterialTheme.colorScheme.error,
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
                        icon = Icons.Outlined.Whatsapp,
                        label = "WhatsApp",
                        isPrimary = false,
                        modifier = Modifier.weight(1f),
                        onClick = {

                            val number = c.phone
                                .replace(" ", "")
                                .replace("+", "")

                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://wa.me/$number")
                            )

                            intent.setPackage("com.whatsapp")

                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "WhatsApp is not installed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
                1 -> NotesTab(
                    notes = c.conversationNotes,

                    onAddNote = {
                        showAddNoteDialog = true
                    },

                    onEditNote = { note ->
                        noteToEdit = note
                        showEditNoteDialog = true
                    },

                    onDeleteNote = { note ->
                        noteToDelete = note
                        showDeleteNoteDialog = true
                    }
                )
                2 -> MediaTab(
                    contact = c,
                    onAddMedia = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onDeleteMediaClick = { mediaUrl ->
                        mediaToDelete = mediaUrl
                        showDeleteMediaDialog = true
                    },
                    onMediaClick = { mediaUrl ->
                        selectedMedia = mediaUrl
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAddNoteDialog) {

        var newNote by remember {
            mutableStateOf("")
        }

        AlertDialog(
            onDismissRequest = {
                showAddNoteDialog = false
            },

            title = {
                Text("Add Note")
            },

            text = {
                OutlinedTextField(
                    value = newNote,
                    onValueChange = {
                        newNote = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    placeholder = {
                        Text("Write your note here...")
                    }
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        contact?.let { currentContact ->

                            val trimmedNote = newNote.trim()

                            if (trimmedNote.isNotEmpty()) {
                                val updatedNotes = currentContact.conversationNotes + ContactNote(
                                    text = trimmedNote,
                                    createdAt = Timestamp.now()
                                )

                                Firebase.firestore
                                    .collection("contacts")
                                    .document(currentContact.contactId)
                                    .update("conversationNotes", updatedNotes)

                                contact = currentContact.copy(
                                    conversationNotes = updatedNotes
                                )
                            }
                        }

                        showAddNoteDialog = false
                    }
                ) {
                    Text("Save")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showAddNoteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditNoteDialog) {

        var editedNote by remember(noteToEdit) {
            mutableStateOf(noteToEdit?.text ?: "")
        }

        AlertDialog(
            onDismissRequest = {
                showEditNoteDialog = false
            },

            title = {
                Text("Edit Note")
            },

            text = {
                OutlinedTextField(
                    value = editedNote,
                    onValueChange = {
                        editedNote = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    placeholder = {
                        Text("Edit your note...")
                    }
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        contact?.let { currentContact ->

                            noteToEdit?.let { oldNote ->

                                val updatedNotes =
                                    currentContact.conversationNotes.map {

                                        if (it == oldNote) {

                                            it.copy(
                                                text = editedNote.trim()
                                            )

                                        } else {

                                            it
                                        }
                                    }

                                Firebase.firestore
                                    .collection("contacts")
                                    .document(currentContact.contactId)
                                    .update(
                                        "conversationNotes",
                                        updatedNotes
                                    )

                                contact = currentContact.copy(
                                    conversationNotes = updatedNotes
                                )
                            }
                        }

                        showEditNoteDialog = false
                        noteToEdit = null
                    }
                ) {
                    Text("Save")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showEditNoteDialog = false
                        noteToEdit = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteNoteDialog) {

        AlertDialog(
            onDismissRequest = {
                showDeleteNoteDialog = false
            },

            title = {
                Text("Delete Note")
            },

            text = {
                Text("Are you sure you want to delete this note?")
            },

            confirmButton = {
                TextButton(
                    onClick = {

                        contact?.let { currentContact ->

                            noteToDelete?.let { note ->

                                val updatedNotes =
                                    currentContact.conversationNotes.filter { it != note }

                                Firebase.firestore
                                    .collection("contacts")
                                    .document(currentContact.contactId)
                                    .update("conversationNotes", updatedNotes)

                                contact = currentContact.copy(
                                    conversationNotes = updatedNotes
                                )
                            }
                        }

                        showDeleteNoteDialog = false
                        noteToDelete = null
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteNoteDialog = false
                        noteToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },

            title = {
                Text("Delete Contact")
            },

            text = {
                Text(
                    "Are you sure you want to permanently delete this contact?"
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {

                        contact?.let { contact ->
                            showDeleteDialog = false
                            contactViewModel.deleteContact(contact)
                            onBack()
                        }
                    }
                ) {
                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedMedia?.let { imageUrl ->

        Dialog(
            onDismissRequest = {
                selectedMedia = null
            }
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {

                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = {
                        selectedMedia = null
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }

    if (showDeleteMediaDialog) {

        AlertDialog(
            onDismissRequest = {
                showDeleteMediaDialog = false
                mediaToDelete = null
            },

            title = {
                Text("Delete Media")
            },

            text = {
                Text("Are you sure you want to delete this media?")
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        mediaToDelete?.let { mediaUrl ->

                            contact?.let { currentContact ->

                                scope.launch {

                                    try {

                                        Firebase.storage
                                            .getReferenceFromUrl(mediaUrl)
                                            .delete()
                                            .await()

                                        val updatedMedia =
                                            currentContact.media.filter { it != mediaUrl }

                                        Firebase.firestore
                                            .collection("contacts")
                                            .document(currentContact.contactId)
                                            .update("media", updatedMedia)
                                            .await()

                                        contact = currentContact.copy(
                                            media = updatedMedia
                                        )

                                    } catch (e: Exception) {

                                        Toast.makeText(
                                            context,
                                            e.localizedMessage,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }

                        showDeleteMediaDialog = false
                        mediaToDelete = null
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDeleteMediaDialog = false
                        mediaToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                if (contact.address.isNotBlank()) {
                    InfoRow("Address", contact.address, isLink = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                if (contact.linkedIn.isNotBlank()) {
                    InfoRow(
                        label = "LinkedIn",
                        value = contact.linkedIn,
                        isLink = true
                    )
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
private fun InfoRow(
    label: String,
    value: String,
    isLink: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isLink) {
                if (isLink) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(value)
                        )
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isLink)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isLink)
                FontWeight.Medium
            else
                FontWeight.Normal
        )
    }
}


// ─── Notes Tab — real data ────────────────────────────────────────────────────

@Composable
private fun NotesTab(
    notes: List<ContactNote>,
    onAddNote: () -> Unit,
    onEditNote: (ContactNote) -> Unit,
    onDeleteNote: (ContactNote) -> Unit
){

    val formatter = remember {
        SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault())
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                val validNotes = notes
                    .filter { it.text.isNotBlank() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }

                if (validNotes.isEmpty()) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.StickyNote2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "No note added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                } else {

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        validNotes.forEachIndexed { index, note ->

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 1.dp
                            ) {

                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Text(
                                            text = "Note ${index + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = {
                                                onEditNote(note)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = "Edit Note"
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                onDeleteNote(note)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = "Delete Note",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = note.createdAt?.toDate()?.let {
                                            formatter.format(it)
                                        } ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = note.text,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onAddNote,
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
private fun MediaTab(
    contact: Contact,
    onAddMedia: () -> Unit,
    onDeleteMediaClick: (String) -> Unit,
    onMediaClick: (String) -> Unit
){

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {

        OutlinedButton(
            onClick = onAddMedia,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Outlined.AddPhotoAlternate,
                contentDescription = null
            )

            Spacer(Modifier.width(8.dp))

            Text("Add Media")
        }

        Spacer(Modifier.height(20.dp))


        if (contact.businessCardImage.isNotBlank()) {

            Text(
                text = "Business Card",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            AsyncImage(
                model = contact.businessCardImage,
                contentDescription = "Business Card",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            )

        } else if (contact.media.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "No media added yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Tap 'Add Media' to upload photos or documents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (contact.media.isNotEmpty()) {

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Gallery",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.heightIn(max = 500.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(contact.media) { mediaUrl ->

                    Box {

                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onMediaClick(mediaUrl)
                                }
                        )

                        IconButton(
                            onClick = {
                                onDeleteMediaClick(mediaUrl)
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ){
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
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