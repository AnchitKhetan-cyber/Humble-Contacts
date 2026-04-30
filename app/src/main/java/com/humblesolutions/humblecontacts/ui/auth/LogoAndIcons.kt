package com.humblesolutions.humblecontacts.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.humblesolutions.humblecontacts.ui.theme.*
import com.humblesolutions.humblecontacts.R

// Dancing Script fallback — swap with loaded FontFamily if you add the asset
val DancingScript = FontFamily.Cursive

@Composable
fun HumbleContactsLogo() {

    // Pulsing animation for the outer ring
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    // Resolve colors from MaterialTheme so they respond to dark/light automatically
    val iconBg       = MaterialTheme.colorScheme.surfaceVariant
    val iconHead     = MaterialTheme.colorScheme.primary        // Navy600 / Navy200
    val iconAccent   = MaterialTheme.colorScheme.secondary      // Gold400 / Gold200
    val ringColor    = MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val wordmarkColor = MaterialTheme.colorScheme.onBackground

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Box(contentAlignment = Alignment.Center) {

            // Pulsing gold ring
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        width = 1.5.dp,
                        color = ringColor,
                        shape = RoundedCornerShape(26.dp)
                    )
            )

            // Icon container
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(40.dp)) {
                    val w = size.width
                    val h = size.height

                    // Main person — head
                    drawCircle(
                        color  = iconHead,
                        radius = w * 0.18f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.28f)
                    )
                    // Main person — body arc
                    drawArc(
                        color      = iconHead,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter  = false,
                        topLeft    = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.52f),
                        size       = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.36f),
                        style      = Stroke(width = w * 0.09f)
                    )
                    // Left gold accent dot
                    drawCircle(
                        color  = iconAccent,
                        radius = w * 0.09f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.48f)
                    )
                    // Right gold accent dot
                    drawCircle(
                        color  = iconAccent,
                        radius = w * 0.09f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.48f)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // "Humble" script wordmark — Gold secondary
        Text(
            text       = "Humble",
            fontFamily = DancingScript,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.secondary  // Gold400 light / Gold200 dark
        )

        // "CONTACTS" caps — onBackground
        Text(
            text          = "CONTACTS",
            fontSize      = 13.sp,
            fontWeight    = FontWeight.ExtraBold,
            letterSpacing = 3.sp,
            color         = wordmarkColor
        )

        Spacer(Modifier.height(4.dp))

        // Gold underline
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
        )

        Spacer(Modifier.height(6.dp))

        // Tagline
        Text(
            text          = "YOUR NETWORK. YOUR PRIVACY.",
            fontSize      = 9.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
            color         = subtitleColor
        )
    }
}


@Composable
fun GoogleIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = R.drawable.google_logo),
        contentDescription = "Google Sign In",
        modifier = modifier.size(18.dp),
        tint = Color.Unspecified // Keeps original Google colors
    )
}
// ─── Facebook Icon ────────────────────────────────────────────────────────────

@Composable
fun FacebookIcon(
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(R.drawable.facebook_logo),
        contentDescription = "Facebook Sign In",
        modifier = modifier.size(18.dp),
        tint = Color.Unspecified
    )
}