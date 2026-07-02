package com.humblesolutions.humblecontacts.ui.profile

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.humblesolutions.humblecontacts.ui.auth.CountryCodeDropdown
import com.humblesolutions.humblecontacts.ui.auth.ErrorMessage
import com.humblesolutions.humblecontacts.ui.auth.FieldLabel
import com.humblesolutions.humblecontacts.ui.auth.HumbleButton
import com.humblesolutions.humblecontacts.ui.auth.HumbleContactsLogo
import com.humblesolutions.humblecontacts.ui.auth.HumbleInputField
import com.humblesolutions.humblecontacts.ui.theme.Gold400

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    isEditMode: Boolean = false,
    onNavigateHome: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: CompleteProfileViewModel = viewModel(
        factory = CompleteProfileViewModel.Factory(
            LocalContext.current.applicationContext as Application
        )
    )
){

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    val dark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CompleteProfileEvent.NavigateHome ->
                    onNavigateHome()
            }
        }
    }

    Scaffold(

        topBar = {

            if (isEditMode) {

                TopAppBar(

                    title = {
                        Text("Edit Profile")
                    },

                    navigationIcon = {

                        IconButton(
                            onClick = onNavigateBack
                        ) {

                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )

                        }

                    }

                )

            }

        }

    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 70.dp, y = (-70).dp)
                    .clip(RoundedCornerShape(50))
                    .background(Gold400.copy(alpha = if (dark) 0.08f else 0.05f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(64.dp))

                HumbleContactsLogo()

                Spacer(Modifier.height(36.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.5.dp, Gold400.copy(alpha = 0.6f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(28.dp)
                    ) {

                        Text(
                            text = if (isEditMode)
                                "Edit Profile"
                            else
                                "Complete Your Profile",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Text(
                            text =
                                if (isEditMode)
                                    "Update your information"
                                else
                                    "Help others know more about you",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(
                                top = 4.dp,
                                bottom = 24.dp
                            )
                        )

                        ErrorMessage(
                            message = uiState.errorMessage,
                            onDismiss = viewModel::dismissError
                        )

                        Spacer(Modifier.height(8.dp))

                        // ───────────── User Card ─────────────
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 2.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                if (uiState.photoUrl.isNotBlank()) {

                                    AsyncImage(
                                        model = uiState.photoUrl,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                    )

                                } else {

                                    val avatarLetter = if (uiState.isPhoneUser)
                                        uiState.nameInput.take(1).uppercase()
                                    else
                                        uiState.name.take(1).uppercase()

                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .background(Gold400),
                                        contentAlignment = Alignment.Center
                                    ) {

                                        Text(
                                            text = avatarLetter,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )

                                    }
                                }

                                Spacer(Modifier.width(16.dp))

                                Column {

                                    Text(
                                        text = if (uiState.isPhoneUser)
                                            uiState.nameInput.ifBlank { "Your Name" }
                                        else
                                            uiState.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isPhoneUser && uiState.nameInput.isBlank())
                                            MaterialTheme.colorScheme.outline
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = if (uiState.isPhoneUser)
                                            uiState.emailInput.ifBlank { "your@email.com" }
                                        else
                                            uiState.email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // Name and email fields — shown only for phone-authenticated users
                        if (uiState.isPhoneUser) {

                            FieldLabel("Full Name *")

                            HumbleInputField(
                                value          = uiState.nameInput,
                                onValueChange  = viewModel::onNameInputChange,
                                placeholder    = "Name",
                                leadingIcon    = Icons.Outlined.Person,
                                keyboardType   = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Words,
                                imeAction      = ImeAction.Next,
                                isError        = uiState.nameError != null,
                                errorMessage   = uiState.nameError,
                                onImeAction    = { focusManager.moveFocus(FocusDirection.Down) }
                            )

                            Spacer(Modifier.height(16.dp))

                            FieldLabel("Email *")

                            HumbleInputField(
                                value         = uiState.emailInput,
                                onValueChange = viewModel::onEmailInputChange,
                                placeholder   = "you@example.com",
                                leadingIcon   = Icons.Outlined.Email,
                                keyboardType  = KeyboardType.Email,
                                imeAction     = ImeAction.Next,
                                isError       = uiState.emailError != null,
                                errorMessage  = uiState.emailError,
                                onImeAction   = { focusManager.moveFocus(FocusDirection.Down) }
                            )

                            Spacer(Modifier.height(16.dp))

                        }

                        // Profession
                        FieldLabel("Profession *")

                        HumbleInputField(
                            value          = uiState.profession,
                            onValueChange  = viewModel::onProfessionChange,
                            placeholder    = "Role/Position",
                            leadingIcon    = Icons.Outlined.WorkOutline,
                            keyboardType   = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words,
                            imeAction      = ImeAction.Next,
                            isError        = uiState.professionError != null,
                            errorMessage   = uiState.professionError,
                            onImeAction    = { focusManager.moveFocus(FocusDirection.Down) }
                        )

                        Spacer(Modifier.height(16.dp))

                        // Company
                        FieldLabel("Company")

                        HumbleInputField(
                            value          = uiState.company,
                            onValueChange  = viewModel::onCompanyChange,
                            placeholder    = "Company Name",
                            leadingIcon    = Icons.Outlined.Business,
                            keyboardType   = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words,
                            imeAction      = ImeAction.Next,
                            onImeAction    = { focusManager.moveFocus(FocusDirection.Down) }
                        )

                        Spacer(Modifier.height(16.dp))

                        // Phone — country code dropdown + number
                        FieldLabel("Phone Number *")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            CountryCodeDropdown(
                                selectedCountry   = uiState.countryCode,
                                onCountrySelected = viewModel::onCountryCodeChange,
                                enabled           = !uiState.phoneLocked
                            )

                            HumbleInputField(
                                value         = uiState.phone,
                                onValueChange = viewModel::onPhoneChange,
                                placeholder   = "9876543210",
                                leadingIcon   = Icons.Outlined.Phone,
                                keyboardType  = KeyboardType.Phone,
                                imeAction     = ImeAction.Next,
                                isError       = uiState.phoneError != null,
                                errorMessage  = uiState.phoneError,
                                readOnly      = uiState.phoneLocked,
                                onImeAction   = { focusManager.moveFocus(FocusDirection.Down) },
                                modifier      = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // LinkedIn
                        FieldLabel("LinkedIn")

                        HumbleInputField(
                            value         = uiState.linkedInUrl,
                            onValueChange = viewModel::onLinkedInChange,
                            placeholder   = "username",
                            leadingIcon   = Icons.Outlined.Link,
                            keyboardType  = KeyboardType.Uri,
                            imeAction     = ImeAction.Next,
                            onImeAction   = { focusManager.moveFocus(FocusDirection.Down) }
                        )

                        Spacer(Modifier.height(16.dp))

                        // Address
                        FieldLabel("Address")

                        HumbleInputField(
                            value          = uiState.address,
                            onValueChange  = viewModel::onAddressChange,
                            placeholder    = "Your Address",
                            leadingIcon    = Icons.Outlined.LocationOn,
                            keyboardType   = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words,
                            imeAction      = ImeAction.Next,
                            onImeAction    = { focusManager.moveFocus(FocusDirection.Down) }
                        )

                        Spacer(Modifier.height(16.dp))

                        // Bio
                        FieldLabel("Bio")

                        HumbleInputField(
                            value          = uiState.bio,
                            onValueChange  = viewModel::onBioChange,
                            placeholder    = "Tell people something about yourself...",
                            leadingIcon    = Icons.Outlined.Info,
                            keyboardType   = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Sentences,
                            singleLine     = false,
                            minLines       = 4,
                            maxLines       = 5,
                            imeAction      = ImeAction.Done,
                            onImeAction    = { focusManager.clearFocus() }
                        )

                        Spacer(Modifier.height(28.dp))

                        HumbleButton(
                            text =
                                if (isEditMode)
                                    "Update Profile"
                                else
                                    "Save Profile",
                            onClick = {
                                focusManager.clearFocus()
                                if (isEditMode) {

                                    viewModel.updateProfile()

                                } else {

                                    viewModel.saveProfile()

                                }
                            },
                            isLoading = uiState.isLoading
                        )

                        Spacer(Modifier.height(12.dp))

                        if (!isEditMode) {
                            TextButton(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.skipProfile()
                                }
                            ) {
                                Text(
                                    text = "Skip For Now",
                                    color = Gold400,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }

}