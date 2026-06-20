package com.humblesolutions.humblecontacts.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.humblesolutions.humblecontacts.ui.theme.DancingScript
import com.humblesolutions.humblecontacts.ui.theme.Nunito

// ─────────────────────────────────────────────────────────────────────────────
// Animation timeline (ms from launch):
//   100  → started = true (UI loaded)
//   100  → Rings begin fading in (600ms fade-in gate)
//   200  → Particle dots fade in (staggered to 600ms)
//   500  → App icon pops in (spring overshoot)
//   950  → "Humble" slides up
//   1030 → "CONTACTS" slides up
//   1500 → Gold separator line sweeps left → right
//   2000 → Bottom rule grows
//   2100 → Tagline fades in
//   2400 → "A Humble Solutions Product" rises in
//   2900 → Navigate to next screen
// ─────────────────────────────────────────────────────────────────────────────


// ─────────────────────────────────────────────────────────────────────────────
// SplashColors — theme-aware colour system
// ─────────────────────────────────────────────────────────────────────────────

private data class SplashColors(
    val background:  Color,
    val surface:     Color,
    val primary:     Color,
    val primaryText: Color,
    val accent:      Color,
    val ring:        Color,
    val rule:        Color,
)

private val DarkSplashColors = SplashColors(
    background  = Color(0xFF0F1523),
    surface     = Color(0xFF1A2440),
    primary     = Color(0xFF3B5A9A),
    primaryText = Color(0xFF8FA8CC),
    accent      = Color(0xFFD4A017),
    ring        = Color(0xFF3B5A9A).copy(alpha = 0.20f),
    rule        = Color(0xFF3B5A9A).copy(alpha = 0.35f),
)

private val LightSplashColors = SplashColors(
    background  = Color(0xFFF5F7FC),
    surface     = Color(0xFFEEF2FA),
    primary     = Color(0xFF3B5A9A),
    primaryText = Color(0xFF2C4480),
    accent      = Color(0xFFD4A017),
    ring        = Color(0xFF3B5A9A).copy(alpha = 0.10f),
    rule        = Color(0xFF3B5A9A).copy(alpha = 0.20f),
)


// ─────────────────────────────────────────────────────────────────────────────
// Easing curves
// ─────────────────────────────────────────────────────────────────────────────

private val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)


// ─────────────────────────────────────────────────────────────────────────────
// Root composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimatedSplashScreen(
    viewModel: SplashViewModel = viewModel(),
    onNavigate: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit){

        kotlinx.coroutines.delay(3000)

        onNavigate()

    }

    val isDark  = isSystemInDarkTheme()
    val colors  = if (isDark) DarkSplashColors else LightSplashColors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        AmbientRings(colors = colors, started = uiState.started)
        ParticleDots(colors = colors)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier.fillMaxSize()
        ) {
            AppIconAnimated(started = uiState.started, colors = colors)
            Spacer(Modifier.height(18.dp))
            WordmarkAnimated(started = uiState.started, colors = colors)
            Spacer(Modifier.height(16.dp))
            GoldSeparatorLine(started = uiState.started, colors = colors)
            Spacer(Modifier.height(10.dp))
            TaglineAnimated(started = uiState.started, colors = colors)
        }

        HumbleSolutionsLine(
            started  = uiState.started,
            colors   = colors,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp)
        )
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// 1 — App Icon
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppIconAnimated(started: Boolean, colors: SplashColors) {

    val scale by animateFloatAsState(
        targetValue   = if (started) 1f else 0.4f,
        animationSpec = tween(550, delayMillis = 500, easing = EaseOutBack),
        label         = "iconScale"
    )
    val alpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(400, delayMillis = 500),
        label         = "iconAlpha"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
            .size(84.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface),
        contentAlignment = Alignment.Center
    ) {
        NetworkIconDrawing(colors)
    }
}

@Composable
private fun NetworkIconDrawing(colors: SplashColors) {
    Box(modifier = Modifier.size(48.dp)) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(colors.primaryText.copy(alpha = 0.9f))
        )
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(13.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 50f, topEnd = 50f))
                .background(colors.primaryText.copy(alpha = 0.9f))
        )
        Box(
            modifier = Modifier
                .size(9.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.9f))
        )
        Box(
            modifier = Modifier
                .size(9.dp)
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.9f))
        )
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// 2 — Wordmark
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WordmarkAnimated(started: Boolean, colors: SplashColors) {

    val humbleOffsetY by animateFloatAsState(
        targetValue   = if (started) 0f else 22f,
        animationSpec = tween(600, delayMillis = 950, easing = EaseOutExpo),
        label         = "humbleY"
    )
    val humbleAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(500, delayMillis = 950),
        label         = "humbleAlpha"
    )
    val contactsOffsetY by animateFloatAsState(
        targetValue   = if (started) 0f else 22f,
        animationSpec = tween(600, delayMillis = 1030, easing = EaseOutExpo),
        label         = "contactsY"
    )
    val contactsAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(500, delayMillis = 1030),
        label         = "contactsAlpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text          = "Humble",
            fontFamily    = DancingScript,
            fontWeight    = FontWeight.Bold,
            fontSize      = 36.sp,
            color         = colors.accent,
            letterSpacing = 1.sp,
            modifier      = Modifier
                .alpha(humbleAlpha)
                .offset(y = humbleOffsetY.dp)
        )
        Text(
            text          = "CONTACTS",
            fontFamily    = Nunito,
            fontWeight    = FontWeight.ExtraBold,
            fontSize      = 32.sp,
            color         = colors.primaryText,
            letterSpacing = 5.sp,
            modifier      = Modifier
                .alpha(contactsAlpha)
                .offset(y = contactsOffsetY.dp)
        )
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// 3 — Gold separator line
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GoldSeparatorLine(started: Boolean, colors: SplashColors) {

    val progress by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(700, delayMillis = 1500, easing = EaseOutExpo),
        label         = "lineProgress"
    )
    val lineAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(50, delayMillis = 1500),
        label         = "lineAlpha"
    )

    Box(
        modifier = Modifier
            .alpha(lineAlpha)
            .width(140.dp)
            .height(1.5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(colors.accent)
                .align(Alignment.CenterStart)
        )
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// 4 — Tagline
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TaglineAnimated(started: Boolean, colors: SplashColors) {

    val alpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(600, delayMillis = 2100),
        label         = "taglineAlpha"
    )

    Text(
        text          = "YOUR NETWORK. YOUR PRIVACY.",
        fontFamily    = Nunito,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 12.sp,
        letterSpacing = 2.sp,
        color         = colors.primaryText.copy(alpha = 0.5f),
        textAlign     = TextAlign.Center,
        modifier      = Modifier.alpha(alpha)
    )
}


// ─────────────────────────────────────────────────────────────────────────────
// 5 — Brand line
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HumbleSolutionsLine(
    started:  Boolean,
    colors:   SplashColors,
    modifier: Modifier = Modifier
) {
    val ruleProgress by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(600, delayMillis = 2000, easing = EaseOutExpo),
        label         = "ruleProgress"
    )
    val textOffsetY by animateFloatAsState(
        targetValue   = if (started) 0f else 14f,
        animationSpec = tween(500, delayMillis = 2400, easing = EaseOutExpo),
        label         = "hsTextY"
    )
    val textAlpha by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(400, delayMillis = 2400),
        label         = "hsTextAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = modifier
    ) {
        Box(
            modifier = Modifier
                .width(110.dp * ruleProgress)
                .height(0.75.dp)
                .background(colors.rule)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier         = Modifier.height(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text          = "A HUMBLE SOLUTIONS PRODUCT",
                fontFamily    = Nunito,
                fontWeight    = FontWeight.SemiBold,
                fontSize      = 11.sp,
                letterSpacing = 1.sp,
                color         = colors.primaryText.copy(alpha = 0.4f),
                textAlign     = TextAlign.Center,
                modifier      = Modifier
                    .alpha(textAlpha)
                    .offset(y = textOffsetY.dp)
            )
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Ambient rings
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AmbientRings(colors: SplashColors, started: Boolean) {

    val ringsVisible by animateFloatAsState(
        targetValue   = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label         = "ringsVisible"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "rings")

    @Composable
    fun ring(delayMs: Int): Pair<Float, Float> {
        val scale by infiniteTransition.animateFloat(
            initialValue  = 0f,
            targetValue   = 1.6f,
            animationSpec = infiniteRepeatable(
                animation  = tween(2800, delayMillis = delayMs, easing = EaseOutExpo),
                repeatMode = RepeatMode.Restart
            ),
            label = "rS$delayMs"
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue  = 0f,
            targetValue   = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2800
                    0f   at 0    using LinearEasing
                    0.8f at 450  using EaseOutExpo
                    1f   at 1200 using EaseOutExpo
                    0f   at 2800 using EaseOutExpo
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "rA$delayMs"
        )
        return scale to alpha
    }

    val (s1, a1) = ring(0)
    val (s2, a2) = ring(1200)

    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        listOf(s1 to a1, s2 to a2).forEach { (scale, alpha) ->
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .scale(scale)
                    .alpha(alpha * ringsVisible)
                    .drawBehind {
                        drawCircle(
                            color  = colors.ring,
                            radius = size.minDimension / 2f,
                            style  = Stroke(width = 1.dp.toPx())
                        )
                    }
            )
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Particle dots
// ─────────────────────────────────────────────────────────────────────────────

private data class ParticleConfig(
    val xFraction: Float,
    val yFraction: Float,
    val sizeDp:    Dp,
    val alpha:     Float,
    val delayMs:   Int
)

private val particles = listOf(
    ParticleConfig(0.08f, 0.12f, 3.dp, 0.15f, 200),
    ParticleConfig(0.82f, 0.19f, 4.dp, 0.12f, 350),
    ParticleConfig(0.06f, 0.68f, 3.dp, 0.10f, 280),
    ParticleConfig(0.87f, 0.75f, 5.dp, 0.12f, 450),
    ParticleConfig(0.91f, 0.35f, 2.dp, 0.08f, 550),
    ParticleConfig(0.04f, 0.58f, 2.dp, 0.08f, 600),
)

@Composable
private fun ParticleDots(colors: SplashColors) {
    particles.forEach { p ->
        val alpha by animateFloatAsState(
            targetValue   = p.alpha,
            animationSpec = tween(400, delayMillis = p.delayMs),
            label         = "particle"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.TopStart)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            color  = colors.primary.copy(alpha = alpha),
                            radius = p.sizeDp.toPx() / 2f,
                            center = Offset(
                                size.width  * p.xFraction,
                                size.height * p.yFraction
                            )
                        )
                    }
            )
        }
    }
}