package com.example.media

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.VocalSeparationMode
import com.example.model.VoiceEffectPreset
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@UnstableApi
class VoiceEffectsAudioProcessor(
    @Volatile var voiceEffect: VoiceEffectPreset = VoiceEffectPreset.NONE,
    @Volatile var vocalMode: VocalSeparationMode = VocalSeparationMode.ORIGINAL,
    @Volatile var noiseReduction: Boolean = false
) : BaseAudioProcessor() {

    private var phase = 0.0
    private var delayBuffer = FloatArray(16000)
    private var delayWriteIdx = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        val delaySamples = (inputAudioFormat.sampleRate * inputAudioFormat.channelCount * 0.16f)
            .toInt()
            .coerceAtLeast(1024)
        if (delayBuffer.size != delaySamples) {
            delayBuffer = FloatArray(delaySamples)
        }
        delayWriteIdx = 0
        return inputAudioFormat
    }

    override fun onFlush() {
        delayBuffer.fill(0f)
        delayWriteIdx = 0
        phase = 0.0
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val channels = inputAudioFormat.channelCount.coerceAtLeast(1)
        val sampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8000)
        val outBuffer = replaceOutputBuffer(remaining)

        val currentEffect = voiceEffect
        val currentVocal = vocalMode
        val reduceNoise = noiseReduction

        while (inputBuffer.remaining() >= 2 * channels) {
            if (channels == 2) {
                val leftRaw = inputBuffer.short / 32768f
                val rightRaw = inputBuffer.short / 32768f

                var l = leftRaw
                var r = rightRaw

                when (currentVocal) {
                    VocalSeparationMode.VOCAL_ONLY -> {
                        // Mid-channel extraction isolates center-panned vocals
                        val mid = (leftRaw + rightRaw) * 0.6f
                        l = mid
                        r = mid
                    }
                    VocalSeparationMode.INSTRUMENTAL_ONLY -> {
                        // Side-channel cancellation attenuates center vocals
                        val side = (leftRaw - rightRaw) * 0.85f
                        l = side
                        r = -side
                    }
                    VocalSeparationMode.ORIGINAL -> {}
                }

                l = processSample(l, sampleRate, currentEffect, reduceNoise, advancePhase = true)
                r = processSample(r, sampleRate, currentEffect, reduceNoise, advancePhase = false)

                outBuffer.putShort((l.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
                outBuffer.putShort((r.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            } else {
                for (c in 0 until channels) {
                    val sample = inputBuffer.short / 32768f
                    val processed = processSample(
                        sample = sample,
                        sampleRate = sampleRate,
                        effect = currentEffect,
                        reduceNoise = reduceNoise,
                        advancePhase = (c == 0)
                    )
                    outBuffer.putShort((processed.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
                }
            }
        }

        // Copy any leftover bytes safely
        while (inputBuffer.hasRemaining()) {
            outBuffer.put(inputBuffer.get())
        }
        outBuffer.flip()
    }

    private fun processSample(
        sample: Float,
        sampleRate: Int,
        effect: VoiceEffectPreset,
        reduceNoise: Boolean,
        advancePhase: Boolean
    ): Float {
        var s = sample
        if (reduceNoise && abs(s) < 0.015f) {
            s *= 0.18f
        }

        when (effect) {
            VoiceEffectPreset.ROBOT -> {
                val mod = sin(phase).toFloat()
                if (advancePhase) {
                    phase += (2.0 * PI * 65.0) / sampleRate
                    if (phase > 2.0 * PI) phase -= 2.0 * PI
                }
                s = s * (0.45f + 0.55f * mod)
            }
            VoiceEffectPreset.ECHO -> {
                val delayed = delayBuffer[delayWriteIdx]
                val wet = (s + delayed * 0.45f).coerceIn(-1f, 1f)
                delayBuffer[delayWriteIdx] = wet
                delayWriteIdx = (delayWriteIdx + 1) % delayBuffer.size
                s = wet
            }
            VoiceEffectPreset.STUDIO -> {
                val delayed = delayBuffer[delayWriteIdx]
                val warm = (s * 1.12f + delayed * 0.16f).coerceIn(-1f, 1f)
                delayBuffer[delayWriteIdx] = s
                delayWriteIdx = (delayWriteIdx + 1) % delayBuffer.size
                s = warm
            }
            VoiceEffectPreset.MEGAPHONE -> {
                val boosted = (s * 1.55f).coerceIn(-0.85f, 0.85f)
                val delayed = delayBuffer[delayWriteIdx]
                delayBuffer[delayWriteIdx] = boosted * 0.5f
                delayWriteIdx = (delayWriteIdx + 1) % delayBuffer.size
                s = (boosted - delayed * 0.25f).coerceIn(-1f, 1f)
            }
            else -> {}
        }
        return s
    }
}
