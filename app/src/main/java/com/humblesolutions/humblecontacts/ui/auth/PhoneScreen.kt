package com.humblesolutions.humblecontacts.ui.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.humblecontacts.ui.theme.Gold400
import kotlinx.coroutines.delay


// ─────────────────────────────────────────────────────────────────────────────
//  Screen 1 — Phone Number Entry
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhoneInputScreen(
    onBack: () -> Unit,
    onOtpSent: () -> Unit,
    viewModel: PhoneAuthViewModel = viewModel()
) {
    val context     = LocalContext.current
    val dark        = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    val state       = viewModel.state
    var phone       by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is AuthState.CodeSent) onOtpSent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {

        // ── Decorative blob — identical to LoginScreen ────────────────────────
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 70.dp, y = (-70).dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(50))
                .background(Gold400.copy(alpha = if (dark) 0.08f else 0.05f))
        )

        // ── Back button ───────────────────────────────────────────────────────
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 8.dp)
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint               = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            HumbleContactsLogo()

            Spacer(Modifier.height(36.dp))

            // ── Card ──────────────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(28.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {

                    // Icon badge
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Gold400.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Outlined.Phone,
                            contentDescription = null,
                            tint               = Gold400,
                            modifier           = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text  = "Phone sign in",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text     = "We'll send a one-time code to verify your number",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Error banner — same as LoginScreen
                    ErrorMessage(
                        message   = (state as? AuthState.Error)?.message,
                        onDismiss = { viewModel.clearError() }
                    )

                    FieldLabel("Mobile Number")

                    PhoneNumberField(
                        value         = phone,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() } && it.length <= 10) phone = it
                        },
                        isError       = state is AuthState.Error,
                        onDone        = {
                            focusManager.clearFocus()
                            if (phone.length == 10)
                                viewModel.sendOtp("+91$phone", context as Activity)
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text  = "Enter your 10-digit Indian mobile number",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(Modifier.height(24.dp))

                    // Send OTP button — same HumbleButton used in LoginScreen
                    HumbleButton(
                        text      = "Send OTP",
                        onClick   = {
                            focusManager.clearFocus()
                            viewModel.sendOtp("+91$phone", context as Activity)
                        },
                        isLoading = state is AuthState.Loading,
                        enabled   = phone.length == 10 && state !is AuthState.Loading
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text      = "By continuing, you agree to receive an SMS.\nStandard rates may apply.",
                        style     = MaterialTheme.typography.labelSmall,
                        color     = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  Screen 2 — OTP Verification
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OtpVerifyScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: PhoneAuthViewModel = viewModel()
) {
    val dark         = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    val state        = viewModel.state
    var otp          by remember { mutableStateOf("") }
    var countdown    by remember { mutableIntStateOf(30) }
    var canResend    by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is AuthState.Success) onSuccess()
    }

    // 30-second resend countdown
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
        canResend = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {

        // ── Decorative blob — identical to LoginScreen ────────────────────────
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = 70.dp, y = (-70).dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(50))
                .background(Gold400.copy(alpha = if (dark) 0.08f else 0.05f))
        )

        // ── Back button ───────────────────────────────────────────────────────
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 8.dp)
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint               = MaterialTheme.colorScheme.onBackground
            )
        }

        // ── Scrollable content ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            HumbleContactsLogo()

            Spacer(Modifier.height(36.dp))

            // ── Card ──────────────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(28.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(28.dp)) {

                    // Icon badge
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Gold400.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = "✉️",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text  = "Enter OTP",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text     = "A 6-digit code was sent to your phone",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )

                    // Error banner
                    ErrorMessage(
                        message   = (state as? AuthState.Error)?.message,
                        onDismiss = { viewModel.clearError() }
                    )

                    FieldLabel("Verification Code")

                    Spacer(Modifier.height(4.dp))

                    // 6-box OTP input
                    OtpBoxRow(
                        value         = otp,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() } && it.length <= 6) {
                                otp = it
                                // Auto-submit when all 6 digits are entered
                                if (it.length == 6) viewModel.verifyOtp(it)
                            }
                        },
                        isError   = state is AuthState.Error,
                        isLoading = state is AuthState.Loading
                    )

                    Spacer(Modifier.height(28.dp))

                    HumbleButton(
                        text      = "Verify & Continue",
                        onClick   = { viewModel.verifyOtp(otp) },
                        isLoading = state is AuthState.Loading,
                        enabled   = otp.length == 6 && state !is AuthState.Loading
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Resend row ─────────────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "Didn't receive the code?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(4.dp))
                        if (canResend) {
                            TextButton(onClick = {
                                // Reset and trigger resend
                                otp       = ""
                                countdown = 30
                                canResend = false
                                viewModel.resendOtp()
                            }) {
                                Text(
                                    text  = "Resend OTP",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = Gold400
                                )
                            }
                        } else {
                            Text(
                                text  = "Resend in ${countdown}s",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // ── Change number row ──────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text  = "Wrong number?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        TextButton(onClick = onBack) {
                            Text(
                                text  = "Change number",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Gold400
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  Reusable Components
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Outlined phone field with a fixed +91 🇮🇳 prefix pill.
 * Styled to match HumbleTextField's border/corner language.
 */
@Composable
fun PhoneNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    onDone: () -> Unit = {}
) {
    val borderColor = if (isError)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Country code pill
        Box(
            modifier = Modifier
                .padding(start = 12.dp, end = 0.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Gold400.copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "🇮🇳  +91",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Gold400
            )
        }

        Spacer(Modifier.width(8.dp))

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(26.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )

        // Number input
        BasicTextField(
            value           = value,
            onValueChange   = onValueChange,
            modifier        = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
            textStyle       = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush     = SolidColor(Gold400),
            singleLine      = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction    = ImeAction.Done
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { onDone() }
            ),
            decorationBox   = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text  = "98765 43210",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    )
                }
                innerTextField()
            }
        )
    }
}

/**
 * Six individual OTP digit boxes.
 * A tiny hidden BasicTextField handles real input; the boxes are the visual layer.
 * Gold400 active border, auto-focuses on composition, auto-submits at 6 digits.
 */
@Composable
fun OtpBoxRow(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    isLoading: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(contentAlignment = Alignment.Center) {

        // Hidden text field that captures real keyboard input
        BasicTextField(
            value           = value,
            onValueChange   = onValueChange,
            modifier        = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction    = ImeAction.Done
            ),
            singleLine      = true
        )

        // 6 visible boxes
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(6) { index ->
                val char      = value.getOrNull(index)
                val isCurrent = index == value.length && !isLoading

                val bgColor = when {
                    isError    -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                    isCurrent  -> Gold400.copy(alpha = 0.07f)
                    char != null -> Gold400.copy(alpha = 0.04f)
                    else       -> MaterialTheme.colorScheme.surface
                }
                val strokeColor = when {
                    isError    -> MaterialTheme.colorScheme.error
                    isCurrent  -> Gold400
                    char != null -> Gold400.copy(alpha = 0.5f)
                    else       -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }
                val strokeWidth = if (isCurrent) 2.dp else 1.dp

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(strokeWidth, strokeColor, RoundedCornerShape(12.dp))
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { focusRequester.requestFocus() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading && char != null) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = Gold400
                        )
                    } else {
                        Text(
                            text  = char?.toString() ?: "",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isError)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}