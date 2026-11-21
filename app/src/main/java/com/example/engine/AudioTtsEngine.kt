package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PcmAudio(
    val samples: ShortArray,
    val sampleRate: Int
) {
    val durationSeconds: Float get() = if (sampleRate > 0) samples.size.toFloat() / sampleRate else 0f
}

object AudioTtsEngine {

    fun writeWav(audio: PcmAudio, outFile: File): File {
        val dataSize = audio.samples.size * 2
        val byteRate = audio.sampleRate * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(1) // Mono
            putInt(audio.sampleRate)
            putInt(byteRate)
            putShort(2) // Block align
            putShort(16) // Bits per sample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()

        FileOutputStream(outFile).use { fos ->
            fos.write(header)
            val buf = ByteBuffer.allocate(audio.samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in audio.samples) buf.putShort(s)
            fos.write(buf.array())
        }
        return outFile
    }

    fun readWav(file: File): PcmAudio? {
        return runCatching {
            val bytes = file.readBytes()
            if (bytes.size < 44) return null
            val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            var pos = 12
            var sampleRate = 22050
            var channels = 1
            var bitsPerSample = 16
            var dataStart = -1
            var dataLen = 0

            while (pos + 8 <= bytes.size) {
                val id = String(bytes, pos, 4, Charsets.US_ASCII)
                val chunkSize = bb.getInt(pos + 4)
                pos += 8
                if (id == "fmt " && pos + 16 <= bytes.size) {
                    channels = bb.getShort(pos + 2).toInt().coerceAtLeast(1)
                    sampleRate = bb.getInt(pos + 4).coerceAtLeast(8000)
                    bitsPerSample = bb.getShort(pos + 14).toInt().coerceAtLeast(8)
                } else if (id == "data") {
                    dataStart = pos
                    dataLen = min(chunkSize, bytes.size - pos)
                    break
                }
                pos += chunkSize + (chunkSize % 2)
            }
            if (dataStart < 0 || dataLen <= 0) return null

            if (bitsPerSample == 16) {
                val frameCount = dataLen / (2 * channels)
                val mono = ShortArray(frameCount)
                val dataBuf = ByteBuffer.wrap(bytes, dataStart, dataLen).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until frameCount) {
                    var sum = 0
                    for (c in 0 until channels) sum += dataBuf.short.toInt()
                    mono[i] = (sum / channels).toShort()
                }
                PcmAudio(mono, sampleRate)
            } else {
                null
            }
        }.getOrNull()
    }

    fun decodeAudioUri(context: Context, uri: Uri, maxSeconds: Int = 90): Result<PcmAudio> = runCatching {
        val extractor = MediaExtractor()
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Cannot open audio file")
        pfd.use {
            extractor.setDataSource(it.fileDescriptor)
        }

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val f = extractor.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = f
                break
            }
        }
        require(trackIndex >= 0 && format != null) { "No audio track found in selected file." }
        extractor.selectTrack(trackIndex)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        var sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
        var channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val pcmOut = ByteArrayOutputStream()
        val maxBytes = sampleRate * channels * 2 * maxSeconds
        val info = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false

        try {
            while (!sawOutputEos && pcmOut.size() < maxBytes) {
                if (!sawInputEos) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        val chunk = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(chunk)
                        pcmOut.write(chunk)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEos = true
                    }
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val outFmt = codec.outputFormat
                    if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) channels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        val raw = pcmOut.toByteArray()
        val frames = raw.size / (2 * channels.coerceAtLeast(1))
        val mono = ShortArray(frames)
        val bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frames) {
            var sum = 0
            for (c in 0 until channels.coerceAtLeast(1)) sum += bb.short.toInt()
            mono[i] = (sum / channels.coerceAtLeast(1)).toShort()
        }
        PcmAudio(mono, sampleRate)
    }

    fun stitchWavFiles(files: List<File>, gapMs: Int, outFile: File): Result<File> = runCatching {
        val loaded = files.mapNotNull { readWav(it) }
        require(loaded.isNotEmpty()) { "No synthesized audio clips found to mix." }
        val targetRate = loaded.first().sampleRate
        val gapSamples = ((gapMs / 1000f) * targetRate).roundToInt().coerceAtLeast(0)
        val resampled = loaded.map { clip ->
            if (clip.sampleRate == targetRate) clip.samples
            else resample(clip.samples, clip.sampleRate, targetRate)
        }
        val totalLen = resampled.sumOf { it.size } + gapSamples * (resampled.size - 1).coerceAtLeast(0)
        val merged = ShortArray(totalLen)
        var pos = 0
        resampled.forEachIndexed { idx, arr ->
            System.arraycopy(arr, 0, merged, pos, arr.size)
            pos += arr.size
            if (idx < resampled.lastIndex) pos += gapSamples
        }
        writeWav(PcmAudio(merged, targetRate), outFile)
    }

    private fun resample(src: ShortArray, fromRate: Int, toRate: Int): ShortArray {
        if (src.isEmpty() || fromRate == toRate) return src
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val outLen = (src.size / ratio).roundToInt().coerceAtLeast(1)
        return ShortArray(outLen) { i ->
            val pos = i * ratio
            val idx0 = pos.toInt().coerceIn(0, src.lastIndex)
            val idx1 = (idx0 + 1).coerceIn(0, src.lastIndex)
            val frac = pos - idx0
            ((src[idx0] * (1.0 - frac)) + (src[idx1] * frac)).roundToInt().toShort()
        }
    }

    fun processAudio(
        source: PcmAudio,
        trimStartRatio: Float = 0f,
        trimEndRatio: Float = 1f,
        gain: Float = 1f,
        fadeInSec: Float = 0f,
        fadeOutSec: Float = 0f,
        normalize: Boolean = false,
        echoAmount: Float = 0f,
        speed: Float = 1f,
        reverse: Boolean = false
    ): PcmAudio {
        if (source.samples.isEmpty()) return source
        val sIdx = (trimStartRatio.coerceIn(0f, 0.98f) * source.samples.size).roundToInt()
        val eIdx = (trimEndRatio.coerceIn(0.02f, 1f) * source.samples.size).roundToInt().coerceAtLeast(sIdx + 1)
        var work = source.samples.copyOfRange(sIdx, eIdx.coerceAtMost(source.samples.size))

        if (reverse) {
            work.reverse()
        }

        if (abs(speed - 1f) > 0.03f) {
            val spd = speed.coerceIn(0.5f, 2.0f)
            val newLen = (work.size / spd).roundToInt().coerceAtLeast(1)
            work = ShortArray(newLen) { i ->
                val p = i * spd
                val i0 = p.toInt().coerceIn(0, work.lastIndex)
                val i1 = (i0 + 1).coerceIn(0, work.lastIndex)
                val frac = p - i0
                ((work[i0] * (1f - frac)) + (work[i1] * frac)).roundToInt().toShort()
            }
        }

        // Apply gain + fades
        val fadeInSamples = (fadeInSec * source.sampleRate).roundToInt().coerceAtLeast(0)
        val fadeOutSamples = (fadeOutSec * source.sampleRate).roundToInt().coerceAtLeast(0)
        val floatBuf = FloatArray(work.size)
        for (i in work.indices) {
            var env = gain
            if (fadeInSamples > 0 && i < fadeInSamples) {
                env *= (i.toFloat() / fadeInSamples)
            }
            if (fadeOutSamples > 0 && i >= work.size - fadeOutSamples) {
                env *= ((work.size - 1 - i).toFloat() / fadeOutSamples).coerceAtLeast(0f)
            }
            floatBuf[i] = work[i] * env
        }

        if (echoAmount > 0.02f) {
            val delaySamples = (0.22f * source.sampleRate).roundToInt()
            val decay = echoAmount.coerceIn(0f, 0.7f)
            for (i in delaySamples until floatBuf.size) {
                floatBuf[i] += floatBuf[i - delaySamples] * decay
            }
        }

        if (normalize) {
            var peak = 1f
            for (v in floatBuf) peak = max(peak, abs(v))
            val normScale = 30500f / peak
            for (i in floatBuf.indices) floatBuf[i] *= normScale
        }

        val out = ShortArray(floatBuf.size) { i ->
            floatBuf[i].roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return PcmAudio(out, source.sampleRate)
    }

    fun computeWaveformBars(samples: ShortArray, barCount: Int = 72): List<Float> {
        if (samples.isEmpty()) return List(barCount) { 0.05f }
        val step = max(1, samples.size / barCount)
        return List(barCount) { b ->
            val start = b * step
            val end = min(samples.size, start + step)
            var maxAbs = 0
            for (i in start until end) {
                val a = abs(samples[i].toInt())
                if (a > maxAbs) maxAbs = a
            }
            (maxAbs / 32768f).coerceIn(0.04f, 1f)
        }
    }

    fun createAudioTrackPlayer(audio: PcmAudio): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(
            audio.sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val byteLen = audio.samples.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(audio.sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuf, byteLen))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(audio.samples, 0, audio.samples.size)
        return track
    }
}
