package com.humblesolutions.humblecontacts.ui.profile

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.humblecontacts.R
import com.humblesolutions.humblecontacts.ui.auth.HumbleInputField
import com.humblesolutions.humblecontacts.ui.profile.card.CardAccentSwatches
import com.humblesolutions.humblecontacts.ui.profile.card.CardBackground
import com.humblesolutions.humblecontacts.ui.profile.card.CardFontStyle
import com.humblesolutions.humblecontacts.ui.profile.card.CardTemplate
import com.humblesolutions.humblecontacts.ui.profile.card.VisitingCardView
import com.humblesolutions.humblecontacts.ui.profile.card.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitingCardScreen(
    onBack: () -> Unit,
    viewModel: VisitingCardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Moves focus between the card's text fields on "Next" and dismisses the
    // keyboard on "Done".
    val focusManager = LocalFocusManager.current

    // Dismiss the keyboard before leaving so it doesn't collapse on top of (and
    // visually swallow) the screen's slide-out transition.
    val exitScreen = {
        focusManager.clearFocus(force = true)
        onBack()
    }

    // One-shot feedback for save results.
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            Toast.makeText(context, context.getString(R.string.card_saved), Toast.LENGTH_SHORT).show()
            viewModel.consumeSaveSuccess()
            // Return to the previous screen once the card is saved.
            exitScreen()
        }
    }
    LaunchedEffect(state.saveError) {
        if (state.saveError) {
            Toast.makeText(context, context.getString(R.string.card_save_error), Toast.LENGTH_LONG).show()
            viewModel.consumeSaveError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.card_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = exitScreen) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.card_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            state.loadError || state.profile == null -> {
                Box(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.card_load_error),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.load() }) {
                            Text(stringResource(R.string.card_retry))
                        }
                    }
                }
            }

            else -> {
                val profile = state.profile!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        // Tap on empty space clears focus and dismisses the keyboard.
                        // Interactive children consume their own taps, so this only
                        // fires for taps outside a field/chip/button.
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(12.dp))

                    // ── Live preview ─────────────────────────────────────────
                    // animateContentSize smooths height changes as fields/layouts
                    // change; Crossfade fades between templates on switch.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Crossfade(
                            targetState = state.card.template.ifBlank { CardTemplate.DEFAULT.id },
                            animationSpec = tween(280),
                            label = "cardTemplate"
                        ) { _ ->
                            VisitingCardView(
                                profile = profile,
                                card = state.card,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Template ─────────────────────────────────────────────
                    // Each option is a live thumbnail of the template's own
                    // default look (with the user's real data), so the design is
                    // visible before choosing — not just a name.
                    SectionLabel(stringResource(R.string.card_template))
                    val selectedTemplateId = state.card.template.ifBlank { CardTemplate.DEFAULT.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CardTemplate.entries.forEach { tpl ->
                            TemplateThumbnail(
                                template = tpl,
                                profile = profile,
                                selected = selectedTemplateId == tpl.id,
                                onClick = { viewModel.onTemplateChange(tpl.id) }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Accent colour ────────────────────────────────────────
                    SectionLabel(stringResource(R.string.card_accent))
                    val selectedAccent = state.card.accentColor.ifBlank {
                        CardTemplate.fromId(state.card.template).defaultAccentHex
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CardAccentSwatches.forEach { hex ->
                            AccentSwatch(
                                hex = hex,
                                selected = selectedAccent.equals(hex, ignoreCase = true),
                                onClick = { viewModel.onAccentChange(hex) }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Background ───────────────────────────────────────────
                    SectionLabel(stringResource(R.string.card_background))
                    val selectedBg = if (state.card.background.isBlank())
                        CardTemplate.fromId(state.card.template).defaultBackground
                    else CardBackground.fromId(state.card.background)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BgOption(R.string.card_bg_surface, CardBackground.SURFACE, selectedBg, viewModel)
                        BgOption(R.string.card_bg_solid, CardBackground.SOLID, selectedBg, viewModel)
                        BgOption(R.string.card_bg_gradient, CardBackground.GRADIENT, selectedBg, viewModel)
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Font ─────────────────────────────────────────────────
                    SectionLabel(stringResource(R.string.card_font))
                    val selectedFont = if (state.card.fontStyle.isBlank())
                        CardTemplate.fromId(state.card.template).defaultFont
                    else CardFontStyle.fromId(state.card.fontStyle)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CardFontStyle.entries.forEach { f ->
                            SelectableChip(
                                label = f.label,
                                selected = selectedFont == f,
                                onClick = { viewModel.onFontChange(f.id) }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Details ──────────────────────────────────────────────
                    SectionLabel(stringResource(R.string.card_details))
                    HumbleInputField(
                        value = state.card.headline,
                        onValueChange = viewModel::onHeadlineChange,
                        placeholder = stringResource(R.string.card_headline_hint),
                        leadingIcon = Icons.Outlined.WorkOutline,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Next) }
                    )
                    Spacer(Modifier.height(12.dp))
                    HumbleInputField(
                        value = state.card.bio,
                        onValueChange = viewModel::onBioChange,
                        placeholder = stringResource(R.string.card_bio_hint),
                        leadingIcon = Icons.Outlined.WorkOutline,
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        imeAction = ImeAction.Default
                    )
                    Spacer(Modifier.height(12.dp))
                    HumbleInputField(
                        value = state.card.websiteUrl,
                        onValueChange = viewModel::onWebsiteChange,
                        placeholder = stringResource(R.string.card_website_hint),
                        leadingIcon = Icons.Outlined.Language,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Next) }
                    )
                    Spacer(Modifier.height(12.dp))
                    HumbleInputField(
                        value = state.card.portfolioUrl,
                        onValueChange = viewModel::onPortfolioChange,
                        placeholder = stringResource(R.string.card_portfolio_hint),
                        leadingIcon = Icons.Outlined.WorkOutline,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                        onImeAction = { focusManager.clearFocus() }
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Save ─────────────────────────────────────────────────
                    Button(
                        onClick = { viewModel.save() },
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.card_save), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun TemplateThumbnail(
    template: CardTemplate,
    profile: com.humblesolutions.humblecontacts.data.model.UserProfile,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Show the template's OWN default look (blank overrides) with the user's data.
    val previewCard = remember(template, profile.visitingCard) {
        profile.visitingCard.copy(
            template = template.id,
            accentColor = "",
            background = "",
            fontStyle = ""
        )
    }
    // Animated selection feedback.
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        label = "thumbBorder"
    )
    val borderW by animateDpAsState(if (selected) 2.dp else 1.dp, label = "thumbBorderW")
    val scale by animateFloatAsState(
        if (selected) 1f else 0.955f, animationSpec = tween(220), label = "thumbScale"
    )
    Column(
        modifier = Modifier.width(168.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = borderW,
                    color = borderColor,
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(onClick = onClick)
                .padding(6.dp)
        ) {
            VisitingCardView(
                profile = profile,
                card = previewCard,
                modifier = Modifier.fillMaxWidth(),
                compact = true
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = template.label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun BgOption(
    labelRes: Int,
    value: CardBackground,
    selected: CardBackground,
    viewModel: VisitingCardViewModel
) {
    SelectableChip(
        label = stringResource(labelRes),
        selected = selected == value,
        onClick = { viewModel.onBackgroundChange(value.id) }
    )
}

@Composable
private fun AccentSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    val color = parseHexColor(hex, MaterialTheme.colorScheme.primary)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = stringResource(R.string.card_accent_selected),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
