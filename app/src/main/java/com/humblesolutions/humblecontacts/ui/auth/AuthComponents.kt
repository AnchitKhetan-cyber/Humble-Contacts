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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblesolutions.humblecontacts.ui.theme.DarkBackground
import com.humblesolutions.humblecontacts.ui.theme.DarkSurface
import com.humblesolutions.humblecontacts.ui.theme.DarkSurfaceVariant
import com.humblesolutions.humblecontacts.ui.theme.Error
import com.humblesolutions.humblecontacts.ui.theme.Gold200
import com.humblesolutions.humblecontacts.ui.theme.Gold400
import com.humblesolutions.humblecontacts.ui.theme.Navy200
import com.humblesolutions.humblecontacts.ui.theme.Navy50
import com.humblesolutions.humblecontacts.ui.theme.Navy600
import com.humblesolutions.humblecontacts.ui.theme.Navy900
import com.humblesolutions.humblecontacts.ui.theme.Success
import com.humblesolutions.humblecontacts.ui.theme.SurfaceVariant
import com.humblesolutions.humblecontacts.ui.theme.TextDarkMuted
import com.humblesolutions.humblecontacts.ui.theme.TextDisabled
import com.humblesolutions.humblecontacts.ui.theme.TextOnGold
import com.humblesolutions.humblecontacts.ui.theme.TextOnNavy
import com.humblesolutions.humblecontacts.ui.theme.TextSecondary
import com.humblesolutions.humblecontacts.ui.theme.Warning

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private val isDarkScheme: @Composable () -> Boolean = {
    MaterialTheme.colorScheme.background == DarkBackground
}

// ─────────────────────────────────────────────────────────────────────────────
// HumbleTextField
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HumbleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    val dark = isDarkScheme()
    val focusedBorder = if (dark) Gold200   else Navy600
    val inputBg       = if (dark) DarkSurfaceVariant else SurfaceVariant
    val placeholderColor = if (dark) TextDarkMuted else TextDisabled
    val iconColor        = if (dark) TextDarkMuted else TextSecondary

    Column(modifier = modifier) {
        OutlinedTextField(
            value             = value,
            onValueChange     = onValueChange,
            placeholder       = {
                Text(
                    text  = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = placeholderColor
                )
            },
            leadingIcon = {
                Icon(
                    imageVector        = leadingIcon,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp),
                    tint               = iconColor
                )
            },
            trailingIcon         = trailingIcon,
            singleLine           = true,
            isError              = isError,
            visualTransformation = visualTransformation,
            keyboardOptions      = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction    = imeAction
            ),
            keyboardActions = keyboardActions,
            shape           = RoundedCornerShape(14.dp),
            colors          = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = inputBg,
                focusedBorderColor      = focusedBorder,
                unfocusedBorderColor    = Color.Transparent,
                errorBorderColor        = Error,
                errorContainerColor     = inputBg,
                cursorColor             = focusedBorder,
                focusedLeadingIconColor = focusedBorder,
                unfocusedLeadingIconColor = iconColor,
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )

        // Inline field error
        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            errorMessage?.let {
                Text(
                    text     = it,
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Error,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HumbleButton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HumbleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val dark      = isDarkScheme()
    val bgColor   = if (dark) Gold400  else Navy600
    val textColor = if (dark) TextOnGold else TextOnNavy

    Button(
        onClick        = onClick,
        enabled        = enabled && !isLoading,
        shape          = RoundedCornerShape(16.dp),
        colors         = ButtonDefaults.buttonColors(
            containerColor         = bgColor,
            contentColor           = textColor,
            disabledContainerColor = bgColor.copy(alpha = 0.45f),
            disabledContentColor   = textColor.copy(alpha = 0.45f),
        ),
        elevation      = ButtonDefaults.buttonElevation(
            defaultElevation  = 4.dp,
            pressedElevation  = 1.dp,
        ),
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier       = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        AnimatedContent(
            targetState  = isLoading,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label        = "btn_content"
        ) { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(22.dp),
                    color       = textColor,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text  = text,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                    color = textColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SocialButton
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SocialButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val dark   = isDarkScheme()

    val bg     =
        if (dark)
            DarkSurfaceVariant
        else
            SurfaceVariant


    val border =
        if (dark)
            Navy200.copy(.3f)
        else
            Navy200.copy(.5f)



    OutlinedButton(

        onClick = onClick,

        shape =
            RoundedCornerShape(
                14.dp
            ),

        colors =
            ButtonDefaults
                .outlinedButtonColors(
                    containerColor = bg
                ),

        border =
            BorderStroke(
                1.dp,
                border
            ),

        contentPadding =
            PaddingValues(
                vertical = 12.dp,
                horizontal = 8.dp
            ),

        modifier =
            modifier.height(
                50.dp
            )

    ) {

        icon()

        Spacer(
            Modifier.width(8.dp)
        )

        Text(

            text = text,

            style =
                MaterialTheme
                    .typography
                    .bodyMedium
                    .copy(
                        fontWeight =
                            FontWeight.Bold
                    ),

            color =
                when(text) {

                    "Google" ->
                        Color(0xFFDB4437)

                    "Facebook" ->
                        Color(0xFF1877F2)

                    else ->
                        MaterialTheme
                            .colorScheme
                            .onSurface
                }

        )

    }

}

// ─────────────────────────────────────────────────────────────────────────────
// OrDivider
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OrDivider(label: String = "OR") {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
        Text(
            text     = label,
            modifier = Modifier.padding(horizontal = 12.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.outline
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PasswordStrengthBar — uses your existing color tokens
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PasswordStrengthBar(strength: Int) {
    // 0=none  1=Weak(red)  2=Fair(gold)  3=Good(gold-light)  4=Strong(green)
    val barColors = listOf(
        Color.Transparent,
        Error,           // Weak
        Gold400,         // Fair
        Gold200,         // Good
        Success,         // Strong
    )
    val labels = listOf("", "Weak", "Fair", "Good", "Strong")

    AnimatedVisibility(
        visible = strength > 0,
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically()
    ) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..4) {
                    val targetColor = if (i <= strength) barColors[strength]
                    else MaterialTheme.colorScheme.outline.copy(.15f)
                    val animColor by animateColorAsState(
                        targetValue  = targetColor,
                        animationSpec = tween(300),
                        label        = "strength_bar_$i"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(animColor)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text  = labels.getOrElse(strength) { "" },
                style = MaterialTheme.typography.labelSmall,
                color = barColors.getOrElse(strength) { Color.Transparent }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ErrorMessage banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ErrorMessage(message: String?, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = message != null,
        enter   = slideInVertically { -it } + fadeIn(),
        exit    = slideOutVertically { -it } + fadeOut()
    ) {
        message?.let { msg ->
            val isSuccess = msg.startsWith("✓")
            val bg        = if (isSuccess) Success.copy(.10f) else Error.copy(.08f)
            val border    = if (isSuccess) Success.copy(.30f) else Error.copy(.25f)
            val tint      = if (isSuccess) Success              else Error

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = bg),
                border   = BorderStroke(1.dp, border)
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = msg,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = tint,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick        = onDismiss,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("✕", color = tint, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FieldLabel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FieldLabel(text: String) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
        color    = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}