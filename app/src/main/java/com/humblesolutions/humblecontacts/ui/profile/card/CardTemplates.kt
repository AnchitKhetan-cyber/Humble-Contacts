package com.humblesolutions.humblecontacts.ui.profile.card

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.humblesolutions.humblecontacts.data.model.VisitingCard
import com.humblesolutions.humblecontacts.ui.theme.Navy600

/**
 * Visiting-card look presets and the small, curated set of customisation
 * options (#64). The [VisitingCard] model stores each choice as a plain string
 * (so Firestore stays simple and forward-compatible); the `from*` resolvers map
 * those strings back to typed values, always falling back to a sensible default
 * so an unknown/blank/legacy value never breaks rendering.
 */

/**
 * The structural layout a template draws with. This is the template's intrinsic
 * identity (unlike accent/background/font, which the user can freely tweak), so
 * the ten presets read as genuinely different professional cards rather than the
 * same card recoloured.
 */
enum class CardLayout {
    /** Avatar + identity on the left, contact rows below a divider. */
    STANDARD,
    /** Everything centre-aligned around a prominent avatar. */
    CENTERED,
    /** A coloured header band (name/role in it) over a light body. */
    HEADER_BAND,
    /** A vertical accent stripe down the leading edge. */
    SIDE_STRIPE
}

/** The preset templates. `id` is what's persisted in `VisitingCard.template`. */
enum class CardTemplate(
    val id: String,
    val label: String,
    val layout: CardLayout,
    val defaultAccentHex: String,
    val defaultBackground: CardBackground,
    val defaultFont: CardFontStyle
) {
    MINIMAL("minimal", "Minimal", CardLayout.STANDARD, "#3B5A9A", CardBackground.SURFACE, CardFontStyle.SANS),
    EXECUTIVE("executive", "Executive", CardLayout.STANDARD, "#1A2D5A", CardBackground.SOLID, CardFontStyle.SERIF),
    CLASSIC("classic", "Classic", CardLayout.HEADER_BAND, "#1A2D5A", CardBackground.SURFACE, CardFontStyle.SERIF),
    CORPORATE("corporate", "Corporate", CardLayout.HEADER_BAND, "#0F4C81", CardBackground.SURFACE, CardFontStyle.SANS),
    GRADIENT("gradient", "Gradient", CardLayout.CENTERED, "#3B5A9A", CardBackground.GRADIENT, CardFontStyle.SANS),
    ELEGANT("elegant", "Elegant", CardLayout.CENTERED, "#A87C0D", CardBackground.SURFACE, CardFontStyle.SERIF),
    MONOGRAM("monogram", "Monogram", CardLayout.CENTERED, "#B00020", CardBackground.SURFACE, CardFontStyle.SANS),
    BOLD("bold", "Bold", CardLayout.STANDARD, "#263238", CardBackground.SOLID, CardFontStyle.SANS),
    SIDEBAR("sidebar", "Sidebar", CardLayout.SIDE_STRIPE, "#00695C", CardBackground.SURFACE, CardFontStyle.SANS),
    MODERN("modern", "Modern", CardLayout.STANDARD, "#6A1B9A", CardBackground.SURFACE, CardFontStyle.MONO);

    companion object {
        val DEFAULT = MINIMAL
        fun fromId(id: String?): CardTemplate =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** How the card body is filled. */
enum class CardBackground(val id: String) {
    /** Card surface colour (theme-aware). */
    SURFACE("surface"),
    /** A solid fill of the accent colour. */
    SOLID("solid"),
    /** A subtle gradient from the accent colour. */
    GRADIENT("gradient");

    companion object {
        val DEFAULT = SURFACE
        fun fromId(id: String?): CardBackground =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/** The curated font choices. */
enum class CardFontStyle(val id: String, val label: String, val family: FontFamily) {
    SANS("sans", "Sans", FontFamily.SansSerif),
    SERIF("serif", "Serif", FontFamily.Serif),
    MONO("mono", "Mono", FontFamily.Monospace);

    companion object {
        val DEFAULT = SANS
        fun fromId(id: String?): CardFontStyle =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * The preset accent swatches offered in the editor. Drawn from the brand ramps
 * plus a couple of complementary hues so cards can look distinct while staying
 * on-brand.
 */
val CardAccentSwatches: List<String> = listOf(
    "#3B5A9A", // Navy600 (primary)
    "#1A2D5A", // Navy900
    "#0F4C81", // Corporate blue
    "#263238", // Charcoal
    "#00695C", // Teal
    "#2E7D32", // Success green
    "#A87C0D", // Gold600
    "#D4A017", // Gold400
    "#6A1B9A", // Purple
    "#B00020"  // Crimson
)

/**
 * Parses a `#RRGGBB` / `#AARRGGBB` hex string to a [Color], returning [fallback]
 * for a blank or malformed value so a bad stored value never crashes rendering.
 */
fun parseHexColor(hex: String, fallback: Color): Color {
    val cleaned = hex.trim()
    if (cleaned.isEmpty()) return fallback
    return try {
        Color(android.graphics.Color.parseColor(cleaned))
    } catch (e: IllegalArgumentException) {
        fallback
    }
}

/**
 * The fully-resolved, ready-to-render look for a card: the stored strings merged
 * with the template's defaults and parsed into typed values.
 */
data class ResolvedCardStyle(
    val template: CardTemplate,
    val accent: Color,
    val background: CardBackground,
    val font: CardFontStyle
)

/** Resolves a [VisitingCard]'s stored look strings against its template defaults. */
fun VisitingCard.resolveStyle(): ResolvedCardStyle {
    val tpl = CardTemplate.fromId(template)
    // Each choice falls back to the template's default when the user hasn't
    // overridden it (blank stored value).
    val accentHex = accentColor.ifBlank { tpl.defaultAccentHex }
    val bg = if (background.isBlank()) tpl.defaultBackground else CardBackground.fromId(background)
    val font = if (fontStyle.isBlank()) tpl.defaultFont else CardFontStyle.fromId(fontStyle)
    return ResolvedCardStyle(
        template = tpl,
        accent = parseHexColor(accentHex, Navy600),
        background = bg,
        font = font
    )
}
