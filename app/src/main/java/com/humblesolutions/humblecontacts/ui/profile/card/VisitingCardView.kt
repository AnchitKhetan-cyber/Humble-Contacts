package com.humblesolutions.humblecontacts.ui.profile.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblesolutions.humblecontacts.data.model.UserProfile
import com.humblesolutions.humblecontacts.data.model.VisitingCard
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.humblesolutions.humblecontacts.R

/**
 * Renders a user's digital visiting card (#64) from their [profile] and [card].
 *
 * Used both for the live preview (editor + Profile screen) and as the source
 * for the exported image, so what the user sees is exactly what they share.
 *
 * Display fields are read live from [profile]; only the extras + look come from
 * [card]. Contact rows honour the profile's `ShareSettings` — a field the user
 * has toggled off is not drawn (matching the QR/vCard exports).
 */
@Composable
fun VisitingCardView(
    profile: UserProfile,
    card: VisitingCard,
    modifier: Modifier = Modifier
) {
    val style = card.resolveStyle()
    val share = profile.shareSettings

    // Content sits on the accent for solid/gradient backgrounds → light text;
    // on the theme surface otherwise → normal on-surface text with accent used
    // as a highlight for the name and icons.
    val onAccent = style.background != CardBackground.SURFACE
    val textPrimary = if (onAccent) Color.White else MaterialTheme.colorScheme.onSurface
    val textSecondary = if (onAccent) Color.White.copy(alpha = 0.85f)
    else MaterialTheme.colorScheme.onSurfaceVariant
    val nameColor = if (onAccent) Color.White else style.accent
    val iconTint = if (onAccent) Color.White else style.accent
    val dividerColor = if (onAccent) Color.White.copy(alpha = 0.25f)
    else MaterialTheme.colorScheme.outlineVariant

    val bodyModifier = when (style.background) {
        CardBackground.SURFACE -> modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)

        CardBackground.SOLID -> modifier
            .clip(RoundedCornerShape(20.dp))
            .background(style.accent)

        CardBackground.GRADIENT -> modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(style.accent, style.accent.copy(alpha = 0.72f))
                )
            )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        tonalElevation = if (onAccent) 0.dp else 1.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = bodyModifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ── Header: avatar + name/role ────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardAvatar(
                    name = profile.name,
                    accent = style.accent,
                    onAccent = onAccent
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name.ifBlank { stringResource(R.string.card_your_name) },
                        color = nameColor,
                        fontFamily = style.font.family,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val roleLine = listOfNotNull(
                        profile.profession.takeIf { it.isNotBlank() },
                        profile.company.takeIf { share.shareCompany && it.isNotBlank() }
                    ).joinToString(" · ")
                    if (roleLine.isNotBlank()) {
                        Text(
                            text = roleLine,
                            color = textSecondary,
                            fontFamily = style.font.family,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (card.headline.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = card.headline,
                    color = textPrimary,
                    fontFamily = style.font.family,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (card.bio.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = card.bio,
                    color = textSecondary,
                    fontFamily = style.font.family,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(dividerColor)
            )
            Spacer(Modifier.height(14.dp))

            // ── Contact rows (privacy-gated) ──────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (share.sharePhone && profile.phone.isNotBlank()) {
                    CardContactRow(
                        icon = Icons.Outlined.Phone,
                        text = "${profile.countryCode}${profile.phone}",
                        tint = iconTint, textColor = textPrimary, font = style.font
                    )
                }
                if (share.shareEmail && profile.email.isNotBlank()) {
                    CardContactRow(
                        icon = Icons.Outlined.Email,
                        text = profile.email,
                        tint = iconTint, textColor = textPrimary, font = style.font
                    )
                }
                if (share.shareLinkedIn && profile.linkedInUrl.isNotBlank()) {
                    CardContactRow(
                        icon = Icons.Outlined.Link,
                        text = profile.linkedInUrl,
                        tint = iconTint, textColor = textPrimary, font = style.font
                    )
                }
                if (card.websiteUrl.isNotBlank()) {
                    CardContactRow(
                        icon = Icons.Outlined.Language,
                        text = card.websiteUrl,
                        tint = iconTint, textColor = textPrimary, font = style.font
                    )
                }
                if (card.portfolioUrl.isNotBlank()) {
                    CardContactRow(
                        icon = Icons.Outlined.WorkOutline,
                        text = card.portfolioUrl,
                        tint = iconTint, textColor = textPrimary, font = style.font
                    )
                }
                if (profile.address.isNotBlank()) {
                    CardContactRow(
                        icon = Icons.Outlined.Place,
                        text = profile.address,
                        tint = iconTint, textColor = textPrimary, font = style.font
                    )
                }
            }
        }
    }
}

@Composable
private fun CardAvatar(
    name: String,
    accent: Color,
    onAccent: Boolean
) {
    val initials = name
        .split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifBlank { "?" }

    val bg = if (onAccent) Color.White.copy(alpha = 0.2f) else accent.copy(alpha = 0.15f)
    val fg = if (onAccent) Color.White else accent
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun CardContactRow(
    icon: ImageVector,
    text: String,
    tint: Color,
    textColor: Color,
    font: com.humblesolutions.humblecontacts.ui.profile.card.CardFontStyle
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = textColor,
            fontFamily = font.family,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = text }
        )
    }
}
