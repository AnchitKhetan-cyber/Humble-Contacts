package com.humblesolutions.humblecontacts.ui.theme

import androidx.compose.ui.graphics.Color



// ui/theme/Color.kt
// Humble Contacts — Brand Color System
// Material Design 3 · Generated from Humble Coders / Humble Solutions logos
// Primary: Humble Navy · Secondary: Humble Gold


// ─── Navy Ramp (Primary) ───────────────────────────────────────────────────

val Navy50  = Color(0xFFEEF2FA)   // Card surfaces, input backgrounds
val Navy100 = Color(0xFFC5D1E8)   // Dividers, skeleton loaders
val Navy200 = Color(0xFF8FA8CC)   // Dark mode primary, icon fills
val Navy400 = Color(0xFF5C7DB5)   // Hover states, progress indicators
val Navy600 = Color(0xFF3B5A9A)   // PRIMARY — buttons, links, active nav
val Navy800 = Color(0xFF2C4480)   // Pressed state, strong text on light bg
val Navy900 = Color(0xFF1A2D5A)   // Dark mode nav bar, darkest headings

// ─── Gold Ramp (Secondary / Accent) ───────────────────────────────────────

val Gold50  = Color(0xFFFDF6E3)   // Tag backgrounds, info chip surfaces
val Gold100 = Color(0xFFF5DFA0)   // Subtle highlights, selected row tint
val Gold200 = Color(0xFFE8C054)   // Dark mode secondary
val Gold400 = Color(0xFFD4A017)   // SECONDARY — FAB, AI badge, accent chip
val Gold600 = Color(0xFFA87C0D)   // Pressed gold, icon on gold surface
val Gold800 = Color(0xFF7A5C0A)   // Text on gold 50 / gold 100
val Gold900 = Color(0xFF4A3405)   // Dark mode onSecondary

// ─── Neutral / Surface ────────────────────────────────────────────────────

val White          = Color(0xFFFFFFFF)
val AppBackground  = Color(0xFFF5F7FC)   // Light mode page bg (very slightly tinted navy)
val SurfaceLight   = Color(0xFFFFFFFF)   // Card / sheet background
val SurfaceVariant = Color(0xFFEEF2FA)   // Input field bg, bottom sheet scrim

val DarkBackground     = Color(0xFF0F1523)   // Dark mode page bg
val DarkSurface        = Color(0xFF1A2440)   // Dark mode card
val DarkSurfaceVariant = Color(0xFF243058)   // Dark mode elevated card / bottom sheet

// ─── Semantic Utility Colors ──────────────────────────────────────────────

val Success     = Color(0xFF2E7D32)
val SuccessBg   = Color(0xFFE8F5E9)
val Warning     = Color(0xFFF57F17)
val WarningBg   = Color(0xFFFFF8E1)
val Error       = Color(0xFFB00020)
val ErrorBg     = Color(0xFFFFDAD6)
val ErrorDark   = Color(0xFFFFB4AB)

// ─── Text Colors ──────────────────────────────────────────────────────────

val TextPrimary   = Color(0xFF1C1C1E)   // Main body text (light mode)
val TextSecondary = Color(0xFF636366)   // Hints, placeholders, metadata
val TextDisabled  = Color(0xFFAEAEB2)   // Disabled state text
val TextOnNavy    = Color(0xFFFFFFFF)   // Text drawn on top of Navy600
val TextOnGold    = Color(0xFF2C2000)   // Text drawn on top of Gold400
val TextDark      = Color(0xFFECF0FB)   // Primary text in dark mode
val TextDarkMuted = Color(0xFF8FA8CC)   // Secondary text in dark mode