package com.humblesolutions.humblecontacts.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblesolutions.humblecontacts.ui.theme.Error
import com.humblesolutions.humblecontacts.ui.theme.Gold400

// ─────────────────────────────────────────────────────────────
// HumbleInputField  (pill + divider + BasicTextField style)
// ─────────────────────────────────────────────────────────────

@Composable
fun HumbleInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    readOnly: Boolean = false,
    onImeAction: () -> Unit = {}
) {
    val borderColor = if (isError)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    val keyboardActions = when (imeAction) {
        ImeAction.Done   -> KeyboardActions(onDone   = { onImeAction() })
        ImeAction.Search -> KeyboardActions(onSearch = { onImeAction() })
        else             -> KeyboardActions(onNext   = { onImeAction() })
    }

    Column(modifier = modifier) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .then(
                    if (singleLine) Modifier.height(56.dp)
                    else Modifier.heightIn(min = 56.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        top   = if (singleLine) 0.dp else 14.dp
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gold400.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = leadingIcon,
                    contentDescription = null,
                    tint               = Gold400,
                    modifier           = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .padding(top = if (singleLine) 0.dp else 14.dp)
                    .width(1.dp)
                    .height(26.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            BasicTextField(
                value                = value,
                onValueChange        = onValueChange,
                readOnly             = readOnly,
                modifier             = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = 14.dp,
                        vertical   = if (singleLine) 0.dp else 14.dp
                    ),
                textStyle            = MaterialTheme.typography.bodyLarge.copy(
                    color = if (readOnly)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush          = SolidColor(Gold400),
                singleLine           = singleLine,
                minLines             = minLines,
                maxLines             = if (singleLine) 1 else maxLines,
                keyboardOptions      = KeyboardOptions(
                    keyboardType   = keyboardType,
                    imeAction      = imeAction,
                    capitalization = capitalization
                ),
                keyboardActions      = keyboardActions,
                visualTransformation = visualTransformation,
                decorationBox        = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text  = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        )
                    }
                    innerTextField()
                }
            )

            trailingIcon?.invoke()
        }

        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            errorMessage?.let {
                Text(
                    text     = it,
                    color    = MaterialTheme.colorScheme.error,
                    style    = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HumbleTextField
// ─────────────────────────────────────────────────────────────

@Composable
fun HumbleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {

    Column(modifier = modifier) {

        OutlinedTextField(

            value = value,

            onValueChange = onValueChange,

            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium
                )
            },

            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null
                )
            },

            trailingIcon = trailingIcon,

            singleLine = singleLine,

            minLines = minLines,

            maxLines = maxLines,

            isError = isError,

            visualTransformation = visualTransformation,

            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
                capitalization = capitalization
            ),

            keyboardActions = keyboardActions,

            shape = RoundedCornerShape(14.dp),

            colors = OutlinedTextFieldDefaults.colors(

                focusedContainerColor =
                    MaterialTheme.colorScheme.surface,

                unfocusedContainerColor =
                    MaterialTheme.colorScheme.surfaceVariant,

                disabledContainerColor =
                    MaterialTheme.colorScheme.surfaceVariant,

                errorContainerColor =
                    MaterialTheme.colorScheme.surfaceVariant,

                focusedBorderColor =
                    MaterialTheme.colorScheme.primary,

                unfocusedBorderColor =
                    Color.Transparent,

                disabledBorderColor =
                    Color.Transparent,

                errorBorderColor =
                    MaterialTheme.colorScheme.error,

                focusedTextColor =
                    MaterialTheme.colorScheme.onSurface,

                unfocusedTextColor =
                    MaterialTheme.colorScheme.onSurface,

                disabledTextColor =
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),

                cursorColor =
                    MaterialTheme.colorScheme.primary,

                focusedLeadingIconColor =
                    MaterialTheme.colorScheme.primary,

                unfocusedLeadingIconColor =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                disabledLeadingIconColor =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                focusedPlaceholderColor =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                unfocusedPlaceholderColor =
                    MaterialTheme.colorScheme.onSurfaceVariant,

                disabledPlaceholderColor =
                    MaterialTheme.colorScheme.onSurfaceVariant
            ),

            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            ),

            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (singleLine) Modifier.height(54.dp)
                    else Modifier
                )
        )

        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {

            errorMessage?.let {

                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(
                        start = 4.dp,
                        top = 4.dp
                    )
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────
// HumbleButton
// ─────────────────────────────────────────────────────────────

@Composable
fun HumbleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {

    Button(
        onClick = onClick,

        enabled = enabled && !isLoading,

        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),

        shape = RoundedCornerShape(16.dp),

        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 1.dp
        ),

        contentPadding = PaddingValues(vertical = 16.dp),

        colors = ButtonDefaults.buttonColors(

            containerColor =
                MaterialTheme.colorScheme.primary,

            contentColor =
                MaterialTheme.colorScheme.onPrimary,

            disabledContainerColor =
                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),

            disabledContentColor =
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.40f)
        )

    ) {

        AnimatedContent(

            targetState = isLoading,

            transitionSpec = {
                fadeIn(tween(200))
                    .togetherWith(
                        fadeOut(tween(200))
                    )
            },

            label = "button_loading"

        ) { loading ->

            if (loading) {

                CircularProgressIndicator(

                    modifier = Modifier.size(22.dp),

                    strokeWidth = 2.5.dp,

                    color = MaterialTheme.colorScheme.onPrimary

                )

            } else {

                Text(

                    text = text,

                    style = MaterialTheme.typography.labelLarge,

                    color = MaterialTheme.colorScheme.onPrimary

                )
            }
        }
    }
}

////////////////////////////////////////////////////////////////////////////////
// SocialButton
////////////////////////////////////////////////////////////////////////////////

@Composable
fun SocialButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (text) {
        "Google"   -> Color(0xFFDB4437)
        "Facebook" -> Color(0xFF1877F2)
        else       -> Gold400
    }

    Row(
        modifier          = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(26.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )

        Text(
            text     = text,
            style    = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// OrDivider
// ─────────────────────────────────────────────────────────────

@Composable
fun OrDivider(
    label: String = "OR"
) {

    Row(

        modifier = Modifier.fillMaxWidth(),

        verticalAlignment = Alignment.CenterVertically

    ) {

        HorizontalDivider(

            modifier = Modifier.weight(1f),

            color = MaterialTheme.colorScheme.outlineVariant

        )

        Text(

            text = label,

            modifier = Modifier.padding(horizontal = 12.dp),

            style = MaterialTheme.typography.labelSmall,

            color = MaterialTheme.colorScheme.onSurfaceVariant

        )

        HorizontalDivider(

            modifier = Modifier.weight(1f),

            color = MaterialTheme.colorScheme.outlineVariant

        )
    }
}

////////////////////////////////////////////////////////////////////////////////
// PasswordStrengthBar
////////////////////////////////////////////////////////////////////////////////

@Composable
fun PasswordStrengthBar(
    strength: Int
) {

    val activeColor = when (strength) {
        1 -> MaterialTheme.colorScheme.error
        2 -> MaterialTheme.colorScheme.tertiary
        3 -> MaterialTheme.colorScheme.primary
        4 -> MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }

    val label = when (strength) {

        1 -> "Weak"

        2 -> "Fair"

        3 -> "Good"

        4 -> "Strong"

        else -> ""
    }

    AnimatedVisibility(

        visible = strength > 0,

        enter = fadeIn() + expandVertically(),

        exit = fadeOut() + shrinkVertically()

    ) {

        Column(

            modifier = Modifier.padding(top = 8.dp)

        ) {

            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.spacedBy(4.dp)

            ) {

                repeat(4) { index ->

                    val color by animateColorAsState(

                        targetValue =

                            if (index < strength)
                                activeColor
                            else
                                MaterialTheme.colorScheme.outlineVariant,

                        animationSpec = tween(300),

                        label = "strength_$index"

                    )

                    Box(

                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(color)

                    )
                }
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(

                text = label,

                style = MaterialTheme.typography.labelMedium,

                color = activeColor

            )
        }
    }
}


// ─────────────────────────────────────────────────────────────
// ErrorMessage
// ─────────────────────────────────────────────────────────────

@Composable
fun ErrorMessage(
    message: String?,
    onDismiss: () -> Unit
) {

    AnimatedVisibility(

        visible = message != null,

        enter = slideInVertically { -it } + fadeIn(),

        exit = slideOutVertically { -it } + fadeOut()

    ) {

        message?.let { msg ->

            val success = msg.startsWith("✓")

            val containerColor =
                if (success)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer

            val contentColor =
                if (success)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer

            Card(

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),

                shape = RoundedCornerShape(14.dp),

                colors = CardDefaults.cardColors(
                    containerColor = containerColor
                ),

                border = BorderStroke(
                    1.dp,
                    contentColor.copy(alpha = 0.20f)
                )

            ) {

                Row(

                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Text(

                        text = msg,

                        modifier = Modifier.weight(1f),

                        style = MaterialTheme.typography.bodyMedium,

                        color = contentColor

                    )

                    TextButton(

                        onClick = onDismiss,

                        contentPadding = PaddingValues(0.dp)

                    ) {

                        Text(

                            text = "✕",

                            color = contentColor,

                            style = MaterialTheme.typography.labelLarge

                        )

                    }
                }
            }
        }
    }
}

////////////////////////////////////////////////////////////////////////////////
// FieldLabel
////////////////////////////////////////////////////////////////////////////////

@Composable
fun FieldLabel(
    text: String
) {

    Text(

        text = text.uppercase(),

        modifier = Modifier.padding(bottom = 6.dp),

        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.sp
        ),

        color = MaterialTheme.colorScheme.onSurfaceVariant

    )
}

