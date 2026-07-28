package com.humblesolutions.humblecontacts.ui.profile

import android.app.Activity
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.humblesolutions.humblecontacts.data.auth.GoogleSignInHelper
import kotlinx.coroutines.launch

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

    val scope = rememberCoroutineScope()

    var showConfirmationDialog by remember {
        mutableStateOf(false)
    }

    var isDeleting by remember {
        mutableStateOf(false)
    }

    // Re-authentication state (a fresh credential must be collected before any delete).
    var showPasswordReauth by remember { mutableStateOf(false) }
    var passwordReauth by remember { mutableStateOf("") }

    var showOtpReauth by remember { mutableStateOf(false) }
    var otpReauth by remember { mutableStateOf("") }
    var phoneVerificationId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = isDeleting) {
        // Disable back press while deleting
    }

    // Runs the actual delete once we hold a fresh credential. On ANY failure it keeps
    // the user on this screen with an error — it never calls onDeleteSuccess().
    fun proceedDelete(credential: com.google.firebase.auth.AuthCredential) {
        isDeleting = true
        profileViewModel.deleteAccount(
            credential = credential,
            onSuccess = {
                isDeleting = false
                Toast.makeText(
                    context,
                    "Account deleted successfully.",
                    Toast.LENGTH_SHORT
                ).show()
                onDeleteSuccess()
            },
            onError = { message ->
                isDeleting = false
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Decides how to re-authenticate based on the user's sign-in provider, then
    // gathers the credential (password dialog / Google picker / OTP) and deletes.
    fun startReauth() {
        when (profileViewModel.currentProviderId()) {
            "email" -> {
                passwordReauth = ""
                showPasswordReauth = true
            }

            "google" -> {
                isDeleting = true
                scope.launch {
                    when (val result = GoogleSignInHelper(context).signIn()) {
                        is GoogleSignInHelper.GoogleSignInResult.Success ->
                            proceedDelete(profileViewModel.buildGoogleCredential(result.idToken))

                        is GoogleSignInHelper.GoogleSignInResult.Cancelled -> {
                            isDeleting = false
                            Toast.makeText(
                                context,
                                "Re-authentication cancelled. Your account was not deleted.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        is GoogleSignInHelper.GoogleSignInResult.Error -> {
                            isDeleting = false
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            "phone" -> {
                val activity = context as? Activity
                if (activity == null) {
                    Toast.makeText(
                        context,
                        "Unable to re-authenticate. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                isDeleting = true
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(cred: PhoneAuthCredential) {
                        // Instant / auto-retrieval — re-auth straight away.
                        proceedDelete(cred)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        isDeleting = false
                        Toast.makeText(
                            context,
                            e.message ?: "Could not send verification code.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        phoneVerificationId = verificationId
                        isDeleting = false
                        otpReauth = ""
                        showOtpReauth = true
                    }
                }
                val sent = profileViewModel.sendReauthOtp(activity, callbacks)
                if (!sent) {
                    isDeleting = false
                    Toast.makeText(
                        context,
                        "No phone number on file for this account.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            else -> {
                Toast.makeText(
                    context,
                    "Unable to determine your sign-in method. Please contact support.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
                        // Close the confirmation and start the mandatory re-auth step.
                        // Nothing is deleted until re-authentication succeeds.
                        showConfirmationDialog = false
                        startReauth()
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

    // ── Re-auth: Email/Password ──────────────────────────────────────────────────

    if (showPasswordReauth) {

        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) showPasswordReauth = false
            },
            title = { Text("Confirm it's you") },
            text = {
                Column {
                    Text(
                        "For your security, enter your password to permanently delete your account."
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passwordReauth,
                        onValueChange = { passwordReauth = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDeleting && passwordReauth.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        val credential =
                            profileViewModel.buildEmailCredential(passwordReauth)
                        if (credential == null) {
                            Toast.makeText(
                                context,
                                "Unable to re-authenticate. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            showPasswordReauth = false
                            proceedDelete(credential)
                        }
                    }
                ) { Text("Delete Forever") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = { showPasswordReauth = false }
                ) { Text("Cancel") }
            }
        )
    }

    // ── Re-auth: Phone OTP ───────────────────────────────────────────────────────

    if (showOtpReauth) {

        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) showOtpReauth = false
            },
            title = { Text("Enter verification code") },
            text = {
                Column {
                    Text(
                        "Enter the 6-digit code we sent to your phone to permanently delete your account."
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otpReauth,
                        onValueChange = { otpReauth = it.filter { c -> c.isDigit() }.take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Verification code") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isDeleting && otpReauth.length == 6 && phoneVerificationId != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        val vid = phoneVerificationId
                        if (vid == null) {
                            Toast.makeText(
                                context,
                                "Verification expired. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()
                            showOtpReauth = false
                        } else {
                            showOtpReauth = false
                            proceedDelete(
                                profileViewModel.buildPhoneCredential(vid, otpReauth)
                            )
                        }
                    }
                ) { Text("Delete Forever") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = { showOtpReauth = false }
                ) { Text("Cancel") }
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


