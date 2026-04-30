package com.humblesolutions.humblecontacts.ui.introduction

// ─────────────────────────────────────────────────────────────────────────────
// IntroductionScreen.kt — Humble Contacts
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.humblesolutions.humblecontacts.ui.intro.IntroEvent
import com.humblesolutions.humblecontacts.ui.intro.IntroViewModel
import com.humblesolutions.humblecontacts.ui.intro.introPages
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// ── Brand palette ─────────────────────────────────────────────────────────────

private val Gold     = Color(0xFFF5D554)
private val GoldDeep = Color(0xFFD4A017)
private val GoldSoft = Color(0xFFFBEFB0)
private val Blue     = Color(0xFF3B5AC6)
private val BlueDeep = Color(0xFF2A45A0)
private val BlueMid  = Color(0xFF5578E0)
private val BluePale = Color(0xFFDDE3F8)

private val SurfLight = Color(0xFFF4F6FF)
private val SurfDark  = Color(0xFF0B0F1E)
private val TxtLight  = Color(0xFF0F172A)
private val TxtDark   = Color(0xFFEEF2FF)
private val SubLight  = Color(0xFF64748B)
private val SubDark   = Color(0xFF8899BB)

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
fun IntroductionScreen(
    // ↓ FIX: parameter is named onNavigateToLogin, matching AppNavGraph
    onNavigateToLogin: () -> Unit = {},
    viewModel: IntroViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = IntroViewModel.Factory()
    )
) {
    val uiState   by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark     = isSystemInDarkTheme()
    val pagerState = rememberPagerState(pageCount = { introPages.size })
    val scope      = rememberCoroutineScope()

    // ↓ FIX: was calling onNavigateToAuth() (undefined). Now calls onNavigateToLogin()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is IntroEvent.NavigateToAuth) onNavigateToLogin()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) SurfDark else SurfLight)
    ) {

        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            PageContent(pageIndex = pageIndex, isDark = isDark)
        }

        // Skip button
        AnimatedVisibility(
            visible  = !uiState.isLastPage,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 20.dp, top = 8.dp)
        ) {
            TextButton(onClick = { viewModel.skip() }) {
                Text(
                    text  = "Skip",
                    color = if (isDark) SubDark else SubLight,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PageIndicator(
                total   = introPages.size,
                current = pagerState.currentPage,
                isDark  = isDark
            )
            Button(
                onClick = {
                    if (uiState.isLastPage) {
                        viewModel.onCtaClick()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                pagerState.currentPage + 1,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness    = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                },
                shape     = RoundedCornerShape(50),
                colors    = ButtonDefaults.buttonColors(
                    containerColor = Blue,
                    contentColor   = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(8.dp, 2.dp),
                modifier  = Modifier
                    .fillMaxWidth(0.78f)
                    .height(56.dp)
            ) {
                Text(
                    text          = introPages[pagerState.currentPage].ctaLabel,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 16.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ── Single page layout ────────────────────────────────────────────────────────

@Composable
private fun PageContent(pageIndex: Int, isDark: Boolean) {
    val page = introPages[pageIndex]

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(pageIndex) { entered = false; delay(60); entered = true }

    val headAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(400, easing = FastOutSlowInEasing), label = "ha")
    val headSlide by animateDpAsState(if (entered) 0.dp else 28.dp, tween(420, easing = FastOutSlowInEasing), label = "hs")
    val bodyAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(440, delayMillis = 110, easing = FastOutSlowInEasing), label = "ba")
    val bodySlide by animateDpAsState(if (entered) 0.dp else 22.dp, tween(440, delayMillis = 110, easing = FastOutSlowInEasing), label = "bs")

    Column(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier         = Modifier.fillMaxWidth().weight(0.52f),
            contentAlignment = Alignment.Center
        ) {
            IllustrationBackground(pageIndex = pageIndex, isDark = isDark)
            IllustrationArt(pageIndex = pageIndex, isDark = isDark)
        }

        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .weight(0.48f)
                .padding(start = 32.dp, end = 32.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .offset(y = bodySlide)
                    .alpha(bodyAlpha)
                    .clip(RoundedCornerShape(50))
                    .background(if (isDark) Color(0xFF2A200A) else GoldSoft)
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Text(
                    text          = page.subline,
                    color         = GoldDeep,
                    fontWeight    = FontWeight.SemiBold,
                    fontSize      = 12.sp,
                    letterSpacing = 0.2.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text          = page.headline,
                color         = if (isDark) TxtDark else TxtLight,
                fontWeight    = FontWeight.ExtraBold,
                fontSize      = 28.sp,
                lineHeight    = 36.sp,
                letterSpacing = (-0.3).sp,
                textAlign     = TextAlign.Center,
                modifier      = Modifier.offset(y = headSlide).alpha(headAlpha)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text      = page.bodyText,
                color     = if (isDark) SubDark else SubLight,
                fontSize  = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.offset(y = bodySlide).alpha(bodyAlpha)
            )
        }
    }
}

// ── Decorative background ─────────────────────────────────────────────────────

@Composable
private fun IllustrationBackground(pageIndex: Int, isDark: Boolean) {
    val accent = when (pageIndex) { 0 -> Blue; 1 -> BlueDeep; else -> BlueMid }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color  = accent.copy(alpha = if (isDark) 0.24f else 0.11f),
            radius = size.width * 0.68f,
            center = Offset(size.width / 2f, size.height * 0.32f)
        )
        drawCircle(
            color  = Gold.copy(alpha = if (isDark) 0.18f else 0.10f),
            radius = size.width * 0.30f,
            center = Offset(size.width * 0.82f, size.height * 0.10f),
            style  = Stroke(width = 2.dp.toPx())
        )
        listOf(
            Offset(size.width * 0.08f, size.height * 0.78f),
            Offset(size.width * 0.14f, size.height * 0.88f),
            Offset(size.width * 0.92f, size.height * 0.80f)
        ).forEach {
            drawCircle(color = Gold.copy(alpha = if (isDark) 0.22f else 0.14f), radius = 4.dp.toPx(), center = it)
        }
    }
}

// ── Canvas illustration ───────────────────────────────────────────────────────

@Composable
private fun IllustrationArt(pageIndex: Int, isDark: Boolean) {
    val inf = rememberInfiniteTransition(label = "art")
    val orbit by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(18_000, easing = LinearEasing)), label = "orbit")
    val floatY by inf.animateFloat(-9f, 9f, infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(pageIndex) { entered = false; delay(40); entered = true }

    val entAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(500), label = "ea")
    val entScale by animateFloatAsState(if (entered) 1f else 0.82f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "es")

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.80f)
            .aspectRatio(1f)
            .scale(entScale)
            .alpha(entAlpha)
            .offset(y = floatY.dp)
    ) {
        when (pageIndex) {
            0    -> drawNetworkPage(isDark, orbit)
            1    -> drawCapturePage(isDark, orbit)
            else -> drawInsightsPage(isDark, orbit)
        }
    }
}

// ── Page illustrations (unchanged from original) ──────────────────────────────

private fun DrawScope.drawNetworkPage(isDark: Boolean, orbit: Float) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    listOf(0.46f to 1.2f, 0.33f to 1.0f, 0.20f to 0.7f).forEach { (frac, sw) ->
        drawCircle(color = Blue.copy(alpha = if (isDark) 0.20f else 0.10f), radius = size.minDimension * frac, center = Offset(cx, cy),
            style = Stroke(width = sw.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
    }
    val orbitR = size.minDimension * 0.44f
    repeat(6) { i ->
        val angle = Math.toRadians((orbit + i * 60.0))
        val nx = cx + orbitR * cos(angle).toFloat()
        val ny = cy + orbitR * sin(angle).toFloat()
        drawLine(color = Blue.copy(alpha = 0.20f), start = Offset(cx, cy), end = Offset(nx, ny), strokeWidth = 1.dp.toPx())
        drawCircle(color = (if (i % 2 == 0) Gold else BlueMid).copy(alpha = 0.22f), radius = 14.dp.toPx(), center = Offset(nx, ny))
        drawCircle(color = if (i % 2 == 0) Gold else BlueMid, radius = 9.dp.toPx(), center = Offset(nx, ny))
        drawCircle(color = Color.White.copy(alpha = 0.65f), radius = 3.5.dp.toPx(), center = Offset(nx - 2.dp.toPx(), ny - 2.dp.toPx()))
    }
    val hubR = size.minDimension * 0.19f
    drawCircle(brush = Brush.radialGradient(listOf(BlueMid, BlueDeep), center = Offset(cx, cy - hubR * 0.1f), radius = hubR), radius = hubR, center = Offset(cx, cy))
    drawCircle(color = Blue.copy(alpha = 0.38f), radius = hubR + 5.dp.toPx(), center = Offset(cx, cy), style = Stroke(width = 1.8.dp.toPx()))
    val lockCx = cx; val lockCy = cy - 3.dp.toPx()
    drawArc(color = Color.White.copy(alpha = 0.90f), startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(lockCx - 8.dp.toPx(), lockCy - 10.dp.toPx()), size = Size(16.dp.toPx(), 12.dp.toPx()),
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
    drawRoundRect(color = Color.White.copy(alpha = 0.90f), topLeft = Offset(lockCx - 7.dp.toPx(), lockCy), size = Size(14.dp.toPx(), 11.dp.toPx()), cornerRadius = CornerRadius(2.5.dp.toPx()))
    drawCircle(color = BlueDeep, radius = 2.5.dp.toPx(), center = Offset(lockCx, lockCy + 5.dp.toPx()))
}

private fun DrawScope.drawCapturePage(isDark: Boolean, orbit: Float) {
    val cx = size.width / 2f; val cy = size.height / 2f
    val cW = size.width * 0.78f; val cH = size.height * 0.46f
    val cL = cx - cW / 2f; val cT = cy - cH / 2f - 8.dp.toPx()
    drawRoundRect(color = BlueDeep.copy(alpha = if (isDark) 0.55f else 0.18f), topLeft = Offset(cL + 8f, cT + 12f), size = Size(cW, cH), cornerRadius = CornerRadius(18.dp.toPx()))
    drawRoundRect(brush = Brush.linearGradient(listOf(BlueMid, BlueDeep), start = Offset(cL, cT), end = Offset(cL + cW, cT + cH)), topLeft = Offset(cL, cT), size = Size(cW, cH), cornerRadius = CornerRadius(18.dp.toPx()))
    drawRoundRect(brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent), start = Offset(cL, cT), end = Offset(cL + cW * 0.6f, cT + cH)), topLeft = Offset(cL, cT), size = Size(cW, cH), cornerRadius = CornerRadius(18.dp.toPx()))
    drawRoundRect(color = Gold, topLeft = Offset(cL + 14.dp.toPx(), cT + cH * 0.72f), size = Size(cW * 0.36f, 4.5.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()))
    drawRoundRect(color = Gold.copy(alpha = 0.42f), topLeft = Offset(cL + 14.dp.toPx(), cT + cH * 0.84f), size = Size(cW * 0.20f, 3.5.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx()))
    val avCx = cL + cH * 0.36f; val avCy = cT + cH * 0.40f; val avR = cH * 0.23f
    drawCircle(color = Gold.copy(alpha = 0.25f), radius = avR + 5.dp.toPx(), center = Offset(avCx, avCy))
    drawCircle(color = Gold, radius = avR, center = Offset(avCx, avCy))
    drawCircle(color = BlueDeep.copy(alpha = 0.55f), radius = avR * 0.36f, center = Offset(avCx, avCy - avR * 0.14f))
    drawArc(color = BlueDeep.copy(alpha = 0.55f), startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(avCx - avR * 0.52f, avCy + avR * 0.08f), size = Size(avR * 1.04f, avR * 0.82f))
    val chipCx = cL + cW - 18.dp.toPx(); val chipCy = cT + 18.dp.toPx()
    repeat(3) { i -> drawArc(color = Gold.copy(alpha = 0.4f + i * 0.18f), startAngle = -55f, sweepAngle = 110f, useCenter = false, topLeft = Offset(chipCx - (5 + i * 4).dp.toPx(), chipCy - (5 + i * 4).dp.toPx()), size = Size((10 + i * 8).dp.toPx(), (10 + i * 8).dp.toPx()), style = Stroke(width = 1.5.dp.toPx())) }
    val scanCx = cx; val scanCy = cT + cH + 26.dp.toPx(); val scanR = 26.dp.toPx()
    rotate(orbit * 0.55f, pivot = Offset(scanCx, scanCy)) {
        listOf(Offset(scanCx - scanR, scanCy - scanR), Offset(scanCx + scanR, scanCy - scanR), Offset(scanCx + scanR, scanCy + scanR), Offset(scanCx - scanR, scanCy + scanR)).forEach { c ->
            val arm = scanR * 0.42f; val dx = if (c.x < scanCx) arm else -arm; val dy = if (c.y < scanCy) arm else -arm
            drawLine(Gold, c, Offset(c.x - dx, c.y), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Gold, c, Offset(c.x, c.y - dy), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(color = Gold.copy(alpha = 0.30f), radius = 5.dp.toPx(), center = Offset(scanCx, scanCy))
        drawCircle(color = Gold, radius = 2.8.dp.toPx(), center = Offset(scanCx, scanCy))
    }
}

private fun DrawScope.drawInsightsPage(isDark: Boolean, orbit: Float) {
    val cx = size.width / 2f; val cy = size.height / 2f
    drawCircle(brush = Brush.radialGradient(listOf(Blue.copy(alpha = if (isDark) 0.28f else 0.12f), Color.Transparent), center = Offset(cx, cy), radius = size.minDimension * 0.50f), radius = size.minDimension * 0.50f, center = Offset(cx, cy))
    val bars = listOf(0.36f, 0.60f, 0.44f, 0.86f, 0.52f, 0.70f)
    val barW = size.width * 0.072f; val barGap = size.width * 0.036f; val chartH = size.height * 0.40f
    val baseY = cy + chartH * 0.50f; val totalW = bars.size * (barW + barGap) - barGap; val startX = cx - totalW / 2f
    drawLine(color = SubDark.copy(alpha = 0.28f), start = Offset(startX - 4f, baseY), end = Offset(startX + totalW + 4f, baseY), strokeWidth = 1.dp.toPx())
    val tops = bars.mapIndexed { i, frac ->
        val bH = chartH * frac; val bX = startX + i * (barW + barGap); val top = baseY - bH
        drawRoundRect(color = Blue.copy(alpha = if (isDark) 0.20f else 0.09f), topLeft = Offset(bX - 2.5f, top - 2.5f), size = Size(barW + 5f, bH + 2.5f), cornerRadius = CornerRadius(7.dp.toPx()))
        drawRoundRect(brush = Brush.verticalGradient(listOf(Gold, Blue), startY = top, endY = baseY), topLeft = Offset(bX, top), size = Size(barW, bH), cornerRadius = CornerRadius(6.dp.toPx()))
        Offset(bX + barW / 2f, top)
    }
    for (i in 0 until tops.lastIndex) drawLine(Gold, tops[i], tops[i + 1], strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
    tops.forEach { pt ->
        drawCircle(color = Gold.copy(alpha = 0.30f), radius = 5.5.dp.toPx(), center = pt)
        drawCircle(color = Gold, radius = 3.8.dp.toPx(), center = pt)
        drawCircle(color = Color.White, radius = 1.8.dp.toPx(), center = pt)
    }
    val badgeCx = cx; val badgeCy = cy - chartH * 0.80f
    rotate(orbit * 0.22f, pivot = Offset(badgeCx, badgeCy)) {
        drawCircle(brush = Brush.radialGradient(listOf(BlueMid, BlueDeep), center = Offset(badgeCx, badgeCy), radius = 24.dp.toPx()), radius = 24.dp.toPx(), center = Offset(badgeCx, badgeCy))
        drawCircle(color = Gold.copy(alpha = 0.40f), radius = 28.dp.toPx(), center = Offset(badgeCx, badgeCy), style = Stroke(width = 1.5.dp.toPx()))
        repeat(3) { i -> drawArc(color = Color.White.copy(alpha = 0.45f + i * 0.15f), startAngle = -50f, sweepAngle = 100f, useCenter = false, topLeft = Offset(badgeCx - (5 + i * 5).dp.toPx(), badgeCy - (5 + i * 5).dp.toPx()), size = Size((10 + i * 10).dp.toPx(), (10 + i * 10).dp.toPx()), style = Stroke(width = 1.5.dp.toPx())) }
    }
    val bellCx = cx + size.width * 0.28f; val bellCy = cy - chartH * 0.58f
    rotate(-(orbit * 0.14f), pivot = Offset(bellCx, bellCy)) {
        drawCircle(color = Gold.copy(alpha = 0.20f), radius = 18.dp.toPx(), center = Offset(bellCx, bellCy))
        drawCircle(color = Gold, radius = 11.dp.toPx(), center = Offset(bellCx, bellCy))
        drawCircle(color = BlueDeep, radius = 4.dp.toPx(), center = Offset(bellCx, bellCy))
    }
}

// ── Page indicator ────────────────────────────────────────────────────────────

@Composable
private fun PageIndicator(total: Int, current: Int, isDark: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        repeat(total) { i ->
            val active = i == current
            val w by animateDpAsState(if (active) 28.dp else 8.dp, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "dw")
            Box(
                Modifier
                    .height(8.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(when { active -> Blue; isDark -> Color(0xFF1E2D5A); else -> BluePale })
            )
        }
    }
}

