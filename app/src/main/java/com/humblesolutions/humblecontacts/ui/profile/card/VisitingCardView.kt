package com.humblesolutions.humblecontacts.ui.profile.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblesolutions.humblecontacts.R
import com.humblesolutions.humblecontacts.data.model.UserProfile
import com.humblesolutions.humblecontacts.data.model.VisitingCard

/**
 * Renders a user's digital visiting card (#64) from their [profile] and [card].
 *
 * Used both for the live preview (editor + Profile screen) and as the source for
 * the exported image, so what the user sees is exactly what they share.
 *
 * The card's *look* comes from the template's [CardLayout] plus the user's
 * accent / background / font choices; the *content* is read live from [profile]
 * (only the extras come from [card]). Contact rows honour the profile's
 * `ShareSettings` — a field the user has toggled off is never drawn, matching the
 * QR/vCard exports.
 */
@Composable
fun VisitingCardView(
    profile: UserProfile,
    card: VisitingCard,
    modifier: Modifier = Modifier
) {
    val style = card.resolveStyle()

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = 3.dp
    ) {
        Box(Modifier.clip(RoundedCornerShape(20.dp))) {
            when (style.template.layout) {
                CardLayout.STANDARD -> StandardCard(profile, card, style)
                CardLayout.CENTERED -> CenteredCard(profile, card, style)
                CardLayout.HEADER_BAND -> HeaderBandCard(profile, card, style)
                CardLayout.SIDE_STRIPE -> SideStripeCard(profile, card, style)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Layouts
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StandardCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle) {
    val fill = surfacingFor(style.background, style.accent)
    val colors = paletteFor(fill.onAccent, style.accent)
    Column(
        Modifier
            .fillMaxWidth()
            .then(fill.modifier)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardAvatar(profile.name, style.accent, fill.onAccent, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                NameText(profile, style, colors, TextAlign.Start)
                RoleText(profile, style, colors.secondary, TextAlign.Start)
            }
        }
        Headline(card, style, colors.primary)
        Bio(card, style, colors.secondary)
        Spacer(Modifier.height(14.dp))
        Divider(colors.divider)
        Spacer(Modifier.height(14.dp))
        ContactList(profile, card, colors, style)
    }
}

@Composable
private fun CenteredCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle) {
    val fill = surfacingFor(style.background, style.accent)
    val colors = paletteFor(fill.onAccent, style.accent)
    Column(
        Modifier
            .fillMaxWidth()
            .then(fill.modifier)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CardAvatar(profile.name, style.accent, fill.onAccent, size = 68.dp)
        Spacer(Modifier.height(12.dp))
        NameText(profile, style, colors, TextAlign.Center)
        RoleText(profile, style, colors.secondary, TextAlign.Center)
        HeadlineCentered(card, style, colors.primary)
        BioCentered(card, style, colors.secondary)
        Spacer(Modifier.height(14.dp))
        Divider(colors.divider)
        Spacer(Modifier.height(14.dp))
        ContactList(profile, card, colors, style)
    }
}

@Composable
private fun HeaderBandCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle) {
    // The band always uses the accent (solid, or gradient when chosen); the body
    // sits on the theme surface for legibility.
    val bandFill = surfacingFor(
        if (style.background == CardBackground.GRADIENT) CardBackground.GRADIENT else CardBackground.SOLID,
        style.accent
    )
    val bodyColors = paletteFor(onAccent = false, accent = style.accent)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Band
        Row(
            Modifier
                .fillMaxWidth()
                .then(bandFill.modifier)
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardAvatar(profile.name, style.accent, onAccent = true, size = 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                NameText(profile, style, paletteFor(true, style.accent), TextAlign.Start)
                RoleText(profile, style, Color.White.copy(alpha = 0.85f), TextAlign.Start)
            }
        }
        // Body
        Column(Modifier.padding(20.dp)) {
            Headline(card, style, bodyColors.primary)
            Bio(card, style, bodyColors.secondary)
            if (card.headline.isNotBlank() || card.bio.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Divider(bodyColors.divider)
                Spacer(Modifier.height(14.dp))
            }
            ContactList(profile, card, bodyColors, style)
        }
    }
}

@Composable
private fun SideStripeCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle) {
    val stripeFill = surfacingFor(
        if (style.background == CardBackground.GRADIENT) CardBackground.GRADIENT else CardBackground.SOLID,
        style.accent
    )
    val colors = paletteFor(onAccent = false, accent = style.accent)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .width(10.dp)
                .fillMaxHeight()
                .then(stripeFill.modifier)
        )
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardAvatar(profile.name, style.accent, onAccent = false, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    NameText(profile, style, colors, TextAlign.Start)
                    RoleText(profile, style, colors.secondary, TextAlign.Start)
                }
            }
            Headline(card, style, colors.primary)
            Bio(card, style, colors.secondary)
            Spacer(Modifier.height(14.dp))
            Divider(colors.divider)
            Spacer(Modifier.height(14.dp))
            ContactList(profile, card, colors, style)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared building blocks
// ─────────────────────────────────────────────────────────────────────────────

/** Resolved text/icon colours for a region. */
private data class CardPalette(
    val name: Color,
    val primary: Color,
    val secondary: Color,
    val icon: Color,
    val divider: Color
)

@Composable
private fun paletteFor(onAccent: Boolean, accent: Color): CardPalette =
    if (onAccent) {
        CardPalette(
            name = Color.White,
            primary = Color.White,
            secondary = Color.White.copy(alpha = 0.85f),
            icon = Color.White,
            divider = Color.White.copy(alpha = 0.25f)
        )
    } else {
        CardPalette(
            name = accent,
            primary = MaterialTheme.colorScheme.onSurface,
            secondary = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = accent,
            divider = MaterialTheme.colorScheme.outlineVariant
        )
    }

/** A region fill (solid/gradient/surface) plus whether content sits on the accent. */
private class Surfacing(val modifier: Modifier, val onAccent: Boolean)

@Composable
private fun surfacingFor(background: CardBackground, accent: Color): Surfacing = when (background) {
    CardBackground.SURFACE -> Surfacing(
        Modifier.background(MaterialTheme.colorScheme.surface), onAccent = false
    )
    CardBackground.SOLID -> Surfacing(
        Modifier.background(accent), onAccent = true
    )
    CardBackground.GRADIENT -> Surfacing(
        Modifier.background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.72f)))),
        onAccent = true
    )
}

@Composable
private fun NameText(profile: UserProfile, style: ResolvedCardStyle, colors: CardPalette, align: TextAlign) {
    Text(
        text = profile.name.ifBlank { stringResource(R.string.card_your_name) },
        color = colors.name,
        fontFamily = style.font.family,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RoleText(profile: UserProfile, style: ResolvedCardStyle, color: Color, align: TextAlign) {
    val roleLine = listOfNotNull(
        profile.profession.takeIf { it.isNotBlank() },
        profile.company.takeIf { profile.shareSettings.shareCompany && it.isNotBlank() }
    ).joinToString(" · ")
    if (roleLine.isNotBlank()) {
        Text(
            text = roleLine,
            color = color,
            fontFamily = style.font.family,
            fontSize = 13.sp,
            textAlign = align,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ColumnScope.Headline(card: VisitingCard, style: ResolvedCardStyle, color: Color) {
    if (card.headline.isNotBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = card.headline,
            color = color,
            fontFamily = style.font.family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ColumnScope.HeadlineCentered(card: VisitingCard, style: ResolvedCardStyle, color: Color) {
    if (card.headline.isNotBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = card.headline,
            color = color,
            fontFamily = style.font.family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ColumnScope.Bio(card: VisitingCard, style: ResolvedCardStyle, color: Color) {
    if (card.bio.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = card.bio,
            color = color,
            fontFamily = style.font.family,
            fontSize = 13.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ColumnScope.BioCentered(card: VisitingCard, style: ResolvedCardStyle, color: Color) {
    if (card.bio.isNotBlank()) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = card.bio,
            color = color,
            fontFamily = style.font.family,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun Divider(color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

@Composable
private fun ContactList(
    profile: UserProfile,
    card: VisitingCard,
    colors: CardPalette,
    style: ResolvedCardStyle
) {
    val share = profile.shareSettings
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (share.sharePhone && profile.phone.isNotBlank()) {
            ContactRow(Icons.Outlined.Phone, "${profile.countryCode}${profile.phone}", colors, style)
        }
        if (share.shareEmail && profile.email.isNotBlank()) {
            ContactRow(Icons.Outlined.Email, profile.email, colors, style)
        }
        if (share.shareLinkedIn && profile.linkedInUrl.isNotBlank()) {
            ContactRow(Icons.Outlined.Link, profile.linkedInUrl, colors, style)
        }
        if (card.websiteUrl.isNotBlank()) {
            ContactRow(Icons.Outlined.Language, card.websiteUrl, colors, style)
        }
        if (card.portfolioUrl.isNotBlank()) {
            ContactRow(Icons.Outlined.WorkOutline, card.portfolioUrl, colors, style)
        }
        if (profile.address.isNotBlank()) {
            ContactRow(Icons.Outlined.Place, profile.address, colors, style)
        }
    }
}

@Composable
private fun ContactRow(icon: ImageVector, text: String, colors: CardPalette, style: ResolvedCardStyle) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.icon, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = colors.primary,
            fontFamily = style.font.family,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { contentDescription = text }
        )
    }
}

@Composable
private fun CardAvatar(name: String, accent: Color, onAccent: Boolean, size: androidx.compose.ui.unit.Dp) {
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
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.34f).sp
        )
    }
}
