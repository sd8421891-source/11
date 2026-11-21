package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundCue {
    SHOT_RIFLE,
    SHOT_SMG,
    SHOT_SHOTGUN,
    SHOT_SNIPER,
    HIT_MARKER,
    HEADSHOT,
    RELOAD,
    GLOO_WALL,
    EXPLOSION,
    HEAL,
    SKILL,
    BOOYAH
}

class SoundFxManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050
    @Volatile
    var enabled: Boolean = true

    private val cachedSamples: Map<SoundCue, ShortArray> by lazy {
        mapOf(
            SoundCue.SHOT_RIFLE to synthNoiseBurst(durationMs = 85, baseFreq = 190f, decay = 22f, amp = 0.45f),
            SoundCue.SHOT_SMG to synthNoiseBurst(durationMs = 50, baseFreq = 260f, decay = 30f, amp = 0.36f),
            SoundCue.SHOT_SHOTGUN to synthNoiseBurst(durationMs = 150, baseFreq = 110f, decay = 14f, amp = 0.65f),
            SoundCue.SHOT_SNIPER to synthNoiseBurst(durationMs = 210, baseFreq = 95f, decay = 11f, amp = 0.72f),
            SoundCue.HIT_MARKER to synthTone(durationMs = 40, startFreq = 920f, endFreq = 620f, amp = 0.35f),
            SoundCue.HEADSHOT to synthTone(durationMs = 110, startFreq = 1480f, endFreq = 1920f, amp = 0.55f),
            SoundCue.RELOAD to synthTone(durationMs = 70, startFreq = 420f, endFreq = 680f, amp = 0.30f),
            SoundCue.GLOO_WALL to synthTone(durationMs = 130, startFreq = 240f, endFreq = 520f, amp = 0.42f),
            SoundCue.EXPLOSION to synthNoiseBurst(durationMs = 340, baseFreq = 65f, decay = 6f, amp = 0.75f),
            SoundCue.HEAL to synthTone(durationMs = 220, startFreq = 520f, endFreq = 880f, amp = 0.36f),
            SoundCue.SKILL to synthTone(durationMs = 190, startFreq = 380f, endFreq = 960f, amp = 0.42f),
            SoundCue.BOOYAH to synthFanfare()
        )
    }

    fun play(cue: SoundCue) {
        if (!enabled) return
        scope.launch {
            val pcm = cachedSamples[cue] ?: return@launch
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, pcm.size)
                track.play()
                val sleepMs = (pcm.size * 1000L) / sampleRate + 35L
                kotlinx.coroutines.delay(sleepMs)
                track.stop()
                track.release()
            } catch (_: Throwable) {
                // Ignore audio hardware limitations in headless environments
            }
        }
    }

    fun release() {
        scope.cancel()
    }

    private fun synthNoiseBurst(durationMs: Int, baseFreq: Float, decay: Float, amp: Float): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        val rng = Random(durationMs * 31 + baseFreq.toInt())
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            val env = exp(-decay * t)
            val punch = sin(2.0 * PI * baseFreq * (1.0 - t * 2.5).coerceAtLeast(0.3) * t).toFloat()
            val noise = rng.nextFloat() * 2f - 1f
            val sample = (noise * 0.65f + punch * 0.5f) * env * amp
            out[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthTone(durationMs: Int, startFreq: Float, endFreq: Float, amp: Float): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val frac = i.toFloat() / count
            val freq = startFreq + (endFreq - startFreq) * frac
            phase += 2.0 * PI * freq / sampleRate
            val env = (1f - frac) * (1f - frac * 0.4f)
            val sample = sin(phase).toFloat() * env * amp
            out[i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthFanfare(): ShortArray {
        val notes = listOf(523.25f, 659.25f, 783.99f, 1046.5f)
        val noteSamples = (sampleRate * 120) / 1000
        val out = ShortArray(notes.size * noteSamples)
        notes.forEachIndexed { idx, freq ->
            var phase = 0.0
            for (i in 0 until noteSamples) {
                val frac = i.toFloat() / noteSamples
                phase += 2.0 * PI * freq / sampleRate
                val env = (1f - frac * 0.6f)
                val sample = sin(phase).toFloat() * env * 0.42f
                out[idx * noteSamples + i] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }
}
