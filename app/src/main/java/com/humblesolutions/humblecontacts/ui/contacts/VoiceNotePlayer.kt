package com.humblesolutions.humblecontacts.ui.contacts

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay

/**
 * A compact play/pause player for a contact's voice note (#voice-notes). Streams
 * the recording from its Storage [audioUrl]; [durationMs] gives the total length
 * so the timeline shows before the media prepares. Releases its [MediaPlayer]
 * when it leaves composition.
 */
@Composable
fun VoiceNotePlayer(
    audioUrl: String,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val player = remember { MediaPlayer() }
    var prepared by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableStateOf(0) }

    // Prepare the stream once, and clean up on dispose.
    DisposableEffect(audioUrl) {
        try {
            player.setDataSource(audioUrl)
            player.setOnPreparedListener { prepared = true }
            player.setOnCompletionListener {
                playing = false
                positionMs = 0
                player.seekTo(0)
            }
            player.setOnErrorListener { _, _, _ -> failed = true; true }
            player.prepareAsync()
        } catch (e: Exception) {
            failed = true
        }
        onDispose {
            try { player.release() } catch (_: Exception) {}
        }
    }

    // While playing, follow the playback position.
    LaunchedEffect(playing) {
        while (playing) {
            positionMs = try { player.currentPosition } catch (e: Exception) { positionMs }
            delay(200)
        }
    }

    val total = if (durationMs > 0) durationMs.toInt() else player.let { if (prepared) it.duration else 0 }
    val progress = if (total > 0) (positionMs.toFloat() / total).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (failed) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primary
                    )
                    .clickable(enabled = prepared && !failed) {
                        if (playing) {
                            player.pause(); playing = false
                        } else {
                            player.start(); playing = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    failed -> Text("!", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 18.sp)
                    !prepared -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    else -> Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            if (failed) {
                Text(
                    "Couldn't load voice note",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = formatMs(if (playing || positionMs > 0) positionMs else total),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
