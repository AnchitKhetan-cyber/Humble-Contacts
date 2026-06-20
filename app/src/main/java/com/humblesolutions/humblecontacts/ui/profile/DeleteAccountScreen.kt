package com.humblesolutions.humblecontacts.ui.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    onBack: () -> Unit,
    onDeleteSuccess: () -> Unit
) {

    val context = LocalContext.current

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val profileViewModel: ProfileViewModel = viewModel()

    var showConfirmationDialog by remember {
        mutableStateOf(false)
    }

    var isDeleting by remember {
        mutableStateOf(false)
    }

    BackHandler(enabled = isDeleting) {
        // Disable back press while deleting
    }

    var acknowledge1 by remember { mutableStateOf(false) }
    var acknowledge2 by remember { mutableStateOf(false) }
    var acknowledge3 by remember { mutableStateOf(false) }
    var acknowledge4 by remember { mutableStateOf(false) }
    var acknowledge5 by remember { mutableStateOf(false) }
    var acknowledge6 by remember { mutableStateOf(false) }

    var deleteText by remember {
        mutableStateOf(TextFieldValue(""))
    }

    val canDelete =
        acknowledge1 &&
                acknowledge2 &&
                acknowledge3 &&
                acknowledge4 &&
                acknowledge5 &&
                acknowledge6 &&
                deleteText.text.trim() == "DELETE"

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        "Delete Account",
                        fontWeight = FontWeight.SemiBold
                    )

                },

                navigationIcon = {

                    IconButton(
                        enabled = !isDeleting,
                        onClick = onBack
                    ) {

                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = null
                        )

                    }

                }

            )

        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),

            horizontalAlignment = Alignment.CenterHorizontally

        ) {

            Spacer(Modifier.height(16.dp))

            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text("Step 1 of 2")
                }
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )

            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Delete Your Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Please read the following carefully before continuing.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Please confirm the following",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(16.dp))

            DeleteChecklistItem(
                checked = acknowledge1,
                title = "Permanent Account Deletion",
                description = "I understand my Humble Contacts account will be permanently deleted."
            ) {
                acknowledge1 = it
            }

            DeleteChecklistItem(
                checked = acknowledge2,
                title = "Delete Contacts",
                description = "All saved contacts, notes and business card information will be removed."
            ) {
                acknowledge2 = it
            }

            DeleteChecklistItem(
                checked = acknowledge3,
                title = "Delete Uploaded Media",
                description = "Uploaded business card images and profile information will be deleted."
            ) {
                acknowledge3 = it
            }

            DeleteChecklistItem(
                checked = acknowledge4,
                title = "Irreversible Action",
                description = "I understand this action cannot be undone."
            ) {
                acknowledge4 = it
            }

            DeleteChecklistItem(
                checked = acknowledge5,
                title = "Export Complete",
                description = "I have exported any contacts I wish to keep."
            ) {
                acknowledge5 = it
            }

            DeleteChecklistItem(
                checked = acknowledge6,
                title = "No Recovery",
                description = "Humble Solutions cannot recover deleted accounts."
            ) {
                acknowledge6 = it
            }

            Spacer(Modifier.height(28.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    Text(
                        "Final Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "To prevent accidental deletion, type",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {

                        Text(
                            "DELETE",
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 8.dp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = deleteText,
                        onValueChange = {
                            deleteText = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        label = {
                            Text("Type DELETE here")
                        },
                        supportingText = {
                            Text(
                                "Account deletion is case-sensitive."
                            )
                        }
                    )

                }

            }

            Spacer(Modifier.height(30.dp))

            Button(
                onClick = {
                    showConfirmationDialog = true
                },
                enabled = canDelete && !isDeleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {

                if (isDeleting) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )

                    Spacer(Modifier.width(12.dp))

                    Text("Deleting Account...")

                } else {

                    Icon(
                        Icons.Outlined.DeleteForever,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        "Delete Account",
                        fontWeight = FontWeight.Bold
                    )

                }

            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                enabled = !isDeleting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {

                Text("Cancel")

            }

            Spacer(Modifier.height(32.dp))

        }
    }

    if (showConfirmationDialog) {

        AlertDialog(

            onDismissRequest = {
                if (!isDeleting) {
                    showConfirmationDialog = false
                }
            },

            icon = {

                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )

            },

            title = {

                Text(
                    "Delete Account?"
                )

            },

            text = {

                Column {

                    Text(
                        "Are you absolutely sure you want to permanently delete your account?"
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "This will permanently remove:",
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("• Your profile")
                    Text("• All saved contacts")
                    Text("• Uploaded business cards")
                    Text("• Meeting history")
                    Text("• Cloud data")

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "This action cannot be undone.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )

                }

            },

            dismissButton = {

                TextButton(

                    enabled = !isDeleting,

                    onClick = {
                        showConfirmationDialog = false
                    }

                ) {

                    Text("Cancel")

                }

            },

            confirmButton = {

                Button(

                    enabled = !isDeleting,

                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),

                    onClick = {

                        isDeleting = true

                        profileViewModel.deleteAccount(

                            onSuccess = {

                                isDeleting = false
                                showConfirmationDialog = false

                                Toast.makeText(
                                    context,
                                    "Account deleted successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                onDeleteSuccess()

                            },

                            onError = { message ->

                                isDeleting = false
                                showConfirmationDialog = false

                                if (message == "REAUTH_REQUIRED") {

                                    Toast.makeText(
                                        context,
                                        "Please sign in again before deleting your account.",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    onDeleteSuccess()

                                } else {

                                    Toast.makeText(
                                        context,
                                        message,
                                        Toast.LENGTH_LONG
                                    ).show()

                                }

                            }

                        )

                    }

                ) {

                    if (isDeleting) {

                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError
                        )

                    } else {

                        Text("Delete Forever")

                    }

                }

            }

        )

    }
}

@Composable
private fun DeleteChecklistItem(
    checked: Boolean,
    title: String,
    description: String,
    onCheckedChange: (Boolean) -> Unit
) {

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {

            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            }

        }

    }

}


