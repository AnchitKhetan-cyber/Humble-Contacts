package com.humblesolutions.humblecontacts.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.io.File

/**
 * Thin wrapper around [MediaRecorder] for recording a voice note (#voice-notes)
 * to an `.m4a` (AAC) file in the app cache. One instance records one clip.
 *
 * Usage: [start], then [stop] which returns the recorded [File] (or null on
 * failure) and exposes [durationMs]. Always [release] when done.
 */
class VoiceNoteRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    /** Recorded length so far / of the finished clip, in milliseconds. */
    var durationMs: Long = 0L
        private set

    /** Starts recording. Returns true if recording began. */
    fun start(): Boolean {
        return try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setAudioEncodingBitRate(96_000)
            rec.setAudioSamplingRate(44_100)
            rec.setOutputFile(file.absolutePath)
            rec.prepare()
            rec.start()

            recorder = rec
            startedAt = SystemClock.elapsedRealtime()
            durationMs = 0L
            true
        } catch (e: Exception) {
            Log.e("VoiceNoteRecorder", "start failed", e)
            release()
            false
        }
    }

    /** Stops recording and returns the finished file, or null on failure. */
    fun stop(): File? {
        val rec = recorder ?: return null
        return try {
            durationMs = SystemClock.elapsedRealtime() - startedAt
            rec.stop()
            rec.release()
            recorder = null
            outputFile
        } catch (e: Exception) {
            // stop() throws if stopped almost immediately (no valid data).
            Log.e("VoiceNoteRecorder", "stop failed", e)
            release()
            outputFile?.delete()
            null
        }
    }

    /** Releases the recorder without keeping the file (cancel). */
    fun release() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }
}
