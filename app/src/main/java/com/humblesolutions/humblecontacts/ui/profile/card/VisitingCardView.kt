package com.humblesolutions.humblecontacts.ui.profile.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblesolutions.humblecontacts.R
import com.humblesolutions.humblecontacts.data.model.UserProfile
import com.humblesolutions.humblecontacts.data.model.VisitingCard

/**
 * Renders a user's digital visiting card (#64) from their [profile] and [card].
 *
 * Used both for the live preview (editor + Profile screen) and as the source for
 * the exported image, so what the user sees is exactly what they share. Set
 * [compact] for the small template-picker thumbnails (tighter spacing, fewer
 * contact rows).
 *
 * Each template maps to its own [CardLayout] — a genuinely distinct structure,
 * not the same card recoloured. The user's accent / background / font choices
 * then restyle whichever layout the template uses. Contact rows honour the
 * profile's `ShareSettings`, so a field toggled off is never drawn (matching the
 * QR/vCard exports).
 */
@Composable
fun VisitingCardView(
    profile: UserProfile,
    card: VisitingCard,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val style = card.resolveStyle()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        shadowElevation = if (compact) 0.dp else 3.dp
    ) {
        Box(Modifier.clip(RoundedCornerShape(20.dp))) {
            when (style.template.layout) {
                CardLayout.STANDARD -> StandardCard(profile, card, style, compact)
                CardLayout.EXECUTIVE -> ExecutiveCard(profile, card, style, compact)
                CardLayout.HEADER_BAND -> HeaderBandCard(profile, card, style, compact)
                CardLayout.SPLIT_PANEL -> SplitPanelCard(profile, card, style, compact)
                CardLayout.CENTERED -> CenteredCard(profile, card, style, compact)
                CardLayout.FRAMED -> FramedCard(profile, card, style, compact)
                CardLayout.MONOGRAM -> MonogramCard(profile, card, style, compact)
                CardLayout.BOLD_TYPE -> BoldTypeCard(profile, card, style, compact)
                CardLayout.SIDE_RAIL -> SideRailCard(profile, card, style, compact)
                CardLayout.MONO_TECH -> MonoTechCard(profile, card, style, compact)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Layouts — each visually distinct
// ─────────────────────────────────────────────────────────────────────────────

/** Avatar left, identity, divider, contact rows. Clean and neutral. */
@Composable
private fun StandardCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle, compact: Boolean) {
    val fill = surfacingFor(style.background, style.accent)
    val colors = paletteFor(fill.onAccent, style.accent)
    Column(Modifier.fillMaxWidth().then(fill.modifier).padding(pad(compact))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Monogram(profile.name, style.accent, fill.onAccent, sz(52.dp, compact))
            Spacer(Modifier.width(sp2(compact)))
            Column(Modifier.weight(1f)) {
                NameText(profile, style, colors, TextAlign.Start, compact)
                RoleText(profile, style, colors.secondary, TextAlign.Start)
            }
        }
        Headline(card, style, colors.primary)
        if (!compact) Bio(card, style, colors.secondary)
        GapDivider(colors.divider, compact)
        ContactList(gatedContacts(profile, card), colors, style, compact)
    }
}

/** Dark, formal, serif. Monogram beside a name over a short accent rule. */
@Composable
private fun ExecutiveCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle, compact: Boolean) {
    // Executive defaults to a solid dark fill; honour a background override too.
    val fill = surfacingFor(style.background, style.accent)
    val colors = paletteFor(fill.onAccent, style.accent)
    val ruleColor = if (fill.onAccent) Color.White.copy(alpha = 0.6f) else style.accent
    Column(Modifier.fillMaxWidth().then(fill.modifier).padding(pad(compact))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Monogram(profile.name, style.accent, fill.onAccent, sz(48.dp, compact))
            Spacer(Modifier.width(sp2(compact)))
            Column(Modifier.weight(1f)) {
                NameText(profile, style, colors, TextAlign.Start, compact, big = true)
                Spacer(Modifier.height(if (compact) 5.dp else 7.dp))
                Box(Modifier.width(if (compact) 28.dp else 40.dp).height(2.dp).background(ruleColor))
                Spacer(Modifier.height(if (compact) 5.dp else 7.dp))
                RoleText(profile, style, colors.secondary, TextAlign.Start)
            }
        }
        Headline(card, style, colors.primary)
        GapDivider(colors.divider, compact)
        ContactList(gatedContacts(profile, card), colors, style, compact)
    }
}

/** A centred accent header band (monogram + name) over a light body. */
@Composable
private fun HeaderBandCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle, compact: Boolean) {
    val bandFill = surfacingFor(
        if (style.background == CardBackground.GRADIENT) CardBackground.GRADIENT else CardBackground.SOLID,
        style.accent
    )
    val bandColors = paletteFor(true, style.accent)
    val bodyColors = paletteFor(false, style.accent)
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier.fillMaxWidth().then(bandFill.modifier)
                .padding(horizontal = pad(compact), vertical = if (compact) 14.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Monogram(profile.name, style.accent, onAccent = true, size = sz(52.dp, compact))
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            NameText(profile, style, bandColors, TextAlign.Center, compact)
            RoleText(profile, style, Color.White.copy(alpha = 0.85f), TextAlign.Center)
        }
        Column(Modifier.padding(pad(compact))) {
            Headline(card, style, bodyColors.primary)
            if (!compact) Bio(card, style, bodyColors.secondary)
            if (compact) Spacer(Modifier.height(2.dp))
            ContactList(gatedContacts(profile, card), bodyColors, style, compact)
        }
    }
}

/** A coloured left panel with a big monogram; details on the right. */
@Composable
private fun SplitPanelCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle, compact: Boolean) {
    val panelFill = surfacingFor(
        if (style.background == CardBackground.GRADIENT) CardBackground.GRADIENT else CardBackground.SOLID,
        style.accent
    )
    val colors = paletteFor(false, style.accent)
    Row(Modifier.fillMaxWidth().heightIn(min = if (compact) 120.dp else 150.dp)
        .background(MaterialTheme.colorScheme.surface)) {
        Box(
            Modifier.width(if (compact) 84.dp else 116.dp).fillMaxHeight().then(panelFill.modifier),
            contentAlignment = Alignment.Center
        ) {
            Monogram(profile.name, style.accent, onAccent = true, size = sz(56.dp, compact), transparentOnAccent = true)
        }
        Column(Modifier.weight(1f).padding(pad(compact))) {
            NameText(profile, style, colors, TextAlign.Start, compact)
            RoleText(profile, style, colors.secondary, TextAlign.Start)
            Headline(card, style, colors.primary)
            GapDivider(colors.divider, compact)
            ContactList(gatedContacts(profile, card), colors, style, compact)
        }
    }
}

/** Everything centre-aligned around a prominent avatar. */
@Composable
private fun CenteredCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle, compact: Boolean) {
    val fill = surfacingFor(style.background, style.accent)
    val colors = paletteFor(fill.onAccent, style.accent)
    Column(
        Modifier.fillMaxWidth().then(fill.modifier).padding(if (compact) 16.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Monogram(profile.name, style.accent, fill.onAccent, sz(68.dp, compact))
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        NameText(profile, style, colors, TextAlign.Center, compact)
        RoleText(profile, style, colors.secondary, TextAlign.Center)
        Headline(card, style, colors.primary, centered = true)
        if (!compact) Bio(card, style, colors.secondary, centered = true)
        GapDivider(colors.divider, compact)
        ContactList(gatedContacts(profile, card), colors, style, compact, centered = true)
    }
}

/** Centred, uppercase letter-spaced name framed by two hairline rules. Elegant. */
@Composable
private fun FramedCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle, compact: Boolean) {
    val fill = surfacingFor(style.background, style.accent)
    val colors = paletteFor(fill.onAccent, style.accent)
    val line = if (fill.onAccent) Color.White.copy(alpha = 0.55f) else style.accent
    Column(
        Modifier.fillMaxWidth().then(fill.modifier).padding(if (compact) 18.dp else 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Monogram(profile.name, style.accent, fill.onAccent, sz(52.dp, compact), ringed = true)
        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
        Box(Modifier.width(if (compact) 40.dp else 56.dp).height(1.dp).background(line))
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Text(
            text = profile.name.ifBlank { stringResource(R.string.card_your_name) }.uppercase(),
            color = if (fill.onAccent) Color.White else style.accent,
            fontFamily = style.font.family,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 14.sp else 18.sp,
            letterSpacing = if (compact) 1.5.sp else 2.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Box(Modifier.width(if (compact) 40.dp else 56.dp).height(1.dp).background(line))
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        RoleText(profile, style, colors.secondary, TextAlign.Center)
        if (!compact) Bio(card, style, colors.secondary, centered = true)
        Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
        ContactList(gatedContacts(profile, card), colors, style, compact, centered = true)
    }
}

/** A dominant monogram badge on top, name and details beneath. */
@Composable
private fun MonogramCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle, compact: Boolean) {
    val fill = surfacingFor(style.background, style.accent)
    val colors = paletteFor(fill.onAccent, style.accent)
    Column(
        Modifier.fillMaxWidth().then(fill.modifier).padding(if (compact) 16.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Monogram(
            profile.name, style.accent, fill.onAccent,
            size = sz(84.dp, compact), ringed = true
        )
        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
        NameText(profile, style, colors, TextAlign.Center, compact, big = !compact)
        RoleText(profile, style, colors.secondary, TextAlign.Center)
        GapDivider(colors.divider, compact)
        ContactList(gatedContacts(profile, card), colors, style, compact, centered = true)
    }
}

/** Oversized name typography; a minimal contact block. */
@Composable
private fun BoldTypeCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle, compact: Boolean) {
    val fill = surfacingFor(style.background, style.accent)
    val colors = paletteFor(fill.onAccent, style.accent)
    Column(Modifier.fillMaxWidth().then(fill.modifier).padding(pad(compact))) {
        Monogram(profile.name, style.accent, fill.onAccent, sz(44.dp, compact))
        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        Text(
            text = profile.name.ifBlank { stringResource(R.string.card_your_name) },
            color = colors.name,
            fontFamily = style.font.family,
            fontWeight = FontWeight.Black,
            fontSize = if (compact) 22.sp else 32.sp,
            lineHeight = if (compact) 24.sp else 34.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        RoleText(profile, style, colors.secondary, TextAlign.Start)
        Headline(card, style, colors.primary)
        GapDivider(colors.divider, compact)
        ContactList(gatedContacts(profile, card), colors, style, compact)
    }
}

/** A wide accent rail (monogram in it) down the leading edge. */
@Composable
private fun SideRailCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle, compact: Boolean) {
    val railFill = surfacingFor(
        if (style.background == CardBackground.GRADIENT) CardBackground.GRADIENT else CardBackground.SOLID,
        style.accent
    )
    val colors = paletteFor(false, style.accent)
    Row(Modifier.fillMaxWidth().heightIn(min = if (compact) 120.dp else 150.dp)
        .background(MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier.width(if (compact) 46.dp else 60.dp).fillMaxHeight().then(railFill.modifier)
                .padding(vertical = pad(compact)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Monogram(profile.name, style.accent, onAccent = true, size = sz(38.dp, compact), transparentOnAccent = true)
        }
        Column(Modifier.weight(1f).padding(pad(compact))) {
            NameText(profile, style, colors, TextAlign.Start, compact)
            RoleText(profile, style, colors.secondary, TextAlign.Start)
            Headline(card, style, colors.primary)
            GapDivider(colors.divider, compact)
            ContactList(gatedContacts(profile, card), colors, style, compact)
        }
    }
}

/** Monospace, an accent corner tab, key-value contact rows. Techy. */
@Composable
private fun MonoTechCard(profile: UserProfile, card: VisitingCard, style: ResolvedCardStyle, compact: Boolean) {
    val fill = surfacingFor(style.background, style.accent)
    val colors = paletteFor(fill.onAccent, style.accent)
    val labelColor = if (fill.onAccent) Color.White.copy(alpha = 0.7f) else style.accent
    Column(Modifier.fillMaxWidth().then(fill.modifier)) {
        // Accent corner tab.
        Box(
            Modifier.padding(start = pad(compact), top = pad(compact))
                .width(if (compact) 26.dp else 34.dp).height(if (compact) 5.dp else 6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (fill.onAccent) Color.White else style.accent)
        )
        Column(Modifier.padding(pad(compact))) {
            NameText(profile, style, colors, TextAlign.Start, compact)
            RoleText(profile, style, colors.secondary, TextAlign.Start)
            Headline(card, style, colors.primary)
            GapDivider(colors.divider, compact)
            val items = gatedContacts(profile, card).let { if (compact) it.take(3) else it }
            Column(verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)) {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.label.padEnd(5),
                            color = labelColor,
                            fontFamily = style.font.family,
                            fontSize = if (compact) 11.sp else 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = item.text,
                            color = colors.primary,
                            fontFamily = style.font.family,
                            fontSize = if (compact) 11.sp else 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).semantics { contentDescription = "${item.label}: ${item.text}" }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Shared building blocks
// ─────────────────────────────────────────────────────────────────────────────

private data class ContactItem(val icon: ImageVector, val label: String, val text: String)

/** Ordered, privacy-gated contact set shared by every layout. */
private fun gatedContacts(profile: UserProfile, card: VisitingCard): List<ContactItem> {
    val share = profile.shareSettings
    return buildList {
        if (share.sharePhone && profile.phone.isNotBlank())
            add(ContactItem(Icons.Outlined.Phone, "tel", "${profile.countryCode}${profile.phone}"))
        if (share.shareEmail && profile.email.isNotBlank())
            add(ContactItem(Icons.Outlined.Email, "mail", profile.email))
        if (share.shareLinkedIn && profile.linkedInUrl.isNotBlank())
            add(ContactItem(Icons.Outlined.Link, "in", profile.linkedInUrl))
        if (card.websiteUrl.isNotBlank())
            add(ContactItem(Icons.Outlined.Language, "web", card.websiteUrl))
        if (card.portfolioUrl.isNotBlank())
            add(ContactItem(Icons.Outlined.WorkOutline, "work", card.portfolioUrl))
        if (profile.address.isNotBlank())
            add(ContactItem(Icons.Outlined.Place, "loc", profile.address))
    }
}

private data class CardPalette(
    val name: Color,
    val primary: Color,
    val secondary: Color,
    val icon: Color,
    val divider: Color
)

@Composable
private fun paletteFor(onAccent: Boolean, accent: Color): CardPalette =
    if (onAccent) CardPalette(
        name = Color.White,
        primary = Color.White,
        secondary = Color.White.copy(alpha = 0.85f),
        icon = Color.White,
        divider = Color.White.copy(alpha = 0.25f)
    ) else CardPalette(
        name = accent,
        primary = MaterialTheme.colorScheme.onSurface,
        secondary = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = accent,
        divider = MaterialTheme.colorScheme.outlineVariant
    )

private class Surfacing(val modifier: Modifier, val onAccent: Boolean)

@Composable
private fun surfacingFor(background: CardBackground, accent: Color): Surfacing = when (background) {
    CardBackground.SURFACE -> Surfacing(Modifier.background(MaterialTheme.colorScheme.surface), false)
    CardBackground.SOLID -> Surfacing(Modifier.background(accent), true)
    CardBackground.GRADIENT -> Surfacing(
        Modifier.background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.72f)))), true
    )
}

// Spacing helpers so every layout scales the same way in compact thumbnails.
private fun pad(compact: Boolean): Dp = if (compact) 14.dp else 20.dp
private fun sp2(compact: Boolean): Dp = if (compact) 10.dp else 14.dp
private fun sz(full: Dp, compact: Boolean): Dp = if (compact) full * 0.76f else full

@Composable
private fun NameText(
    profile: UserProfile,
    style: ResolvedCardStyle,
    colors: CardPalette,
    align: TextAlign,
    compact: Boolean = false,
    big: Boolean = false
) {
    val base = if (big) (if (compact) 17.sp else 24.sp) else (if (compact) 15.sp else 20.sp)
    Text(
        text = profile.name.ifBlank { stringResource(R.string.card_your_name) },
        color = colors.name,
        fontFamily = style.font.family,
        fontWeight = FontWeight.Bold,
        fontSize = base,
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
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun Headline(card: VisitingCard, style: ResolvedCardStyle, color: Color, centered: Boolean = false) {
    if (card.headline.isNotBlank()) {
        Text(
            text = card.headline,
            color = color,
            fontFamily = style.font.family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
    }
}

@Composable
private fun Bio(card: VisitingCard, style: ResolvedCardStyle, color: Color, centered: Boolean = false) {
    if (card.bio.isNotBlank()) {
        Text(
            text = card.bio,
            color = color,
            fontFamily = style.font.family,
            fontSize = 13.sp,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
}

/** A divider with symmetric spacing above and below. */
@Composable
private fun GapDivider(color: Color, compact: Boolean) {
    val g = if (compact) 10.dp else 14.dp
    Spacer(Modifier.height(g))
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
    Spacer(Modifier.height(g))
}

@Composable
private fun ContactList(
    items: List<ContactItem>,
    colors: CardPalette,
    style: ResolvedCardStyle,
    compact: Boolean = false,
    centered: Boolean = false
) {
    val shown = if (compact) items.take(3) else items
    Column(
        verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        shown.forEach { item -> ContactRow(item.icon, item.text, colors, style, centered) }
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    text: String,
    colors: CardPalette,
    style: ResolvedCardStyle,
    centered: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start,
        modifier = if (centered) Modifier else Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = colors.icon, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            color = colors.primary,
            fontFamily = style.font.family,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = (if (centered) Modifier else Modifier.weight(1f))
                .semantics { contentDescription = text }
        )
    }
}

/** Circular monogram of the user's initials. Optionally with an accent ring. */
@Composable
private fun Monogram(
    name: String,
    accent: Color,
    onAccent: Boolean,
    size: Dp,
    ringed: Boolean = false,
    transparentOnAccent: Boolean = false
) {
    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
        .take(2).joinToString("").uppercase().ifBlank { "?" }
    val bg = when {
        transparentOnAccent -> Color.White.copy(alpha = 0.18f)
        onAccent -> Color.White.copy(alpha = 0.2f)
        else -> accent.copy(alpha = 0.15f)
    }
    val fg = if (onAccent) Color.White else accent
    val ring = if (onAccent) Color.White.copy(alpha = 0.6f) else accent
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (ringed) Modifier.border(2.dp, ring, CircleShape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp
        )
    }
}
