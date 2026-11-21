package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.engine.AudioTtsEngine
import com.example.engine.PcmAudio
import com.example.engine.PrintShareHelper
import com.example.studio.StudioViewModel
import com.example.ui.components.StudioModuleScaffold
import com.example.ui.components.StudioSectionCard
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.coroutines.resume

// ---------------- MODULE 14: BANGLA TEXT TO SPEECH ----------------

@Composable
fun BanglaTtsScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var textInput by remember {
        mutableStateOf("আসসালামু আলাইকুম। ইউনুস কম্পিউটার স্টুডিও প্রো-তে আপনাকে স্বাগতম। এখানে পাসপোর্ট ছবি, পিডিএফ ও ডিজাইন সেবা প্রদান করা হয়।")
    }
    var selectedLocaleTag by remember { mutableStateOf("bn-BD") }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var pitch by remember { mutableFloatStateOf(1.0f) }
    var ttsReady by remember { mutableStateOf(false) }
    var banglaSupportText by remember { mutableStateOf("Initializing TTS engine…") }
    var isSpeaking by remember { mutableStateOf(false) }
    var exportedWav by remember { mutableStateOf<File?>(null) }

    val ttsHolder = remember { arrayOfNulls<TextToSpeech>(1) }

    DisposableEffect(context) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                val bnSupport = engine.isLanguageAvailable(Locale("bn", "BD"))
                banglaSupportText = when {
                    bnSupport >= TextToSpeech.LANG_AVAILABLE -> "Bangla (bn-BD) voice ready on device"
                    engine.isLanguageAvailable(Locale("bn", "IN")) >= TextToSpeech.LANG_AVAILABLE -> "Bangla (bn-IN) voice ready on device"
                    else -> "System TTS ready (install Bangla voice in Android TTS settings if silent)"
                }
            } else {
                banglaSupportText = "TTS initialization failed on this device."
            }
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { isSpeaking = true }
            override fun onDone(utteranceId: String?) { isSpeaking = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { isSpeaking = false }
        })
        ttsHolder[0] = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            ttsHolder[0] = null
        }
    }

    fun applyTtsParams(engine: TextToSpeech) {
        val locale = when (selectedLocaleTag) {
            "bn-BD" -> Locale("bn", "BD")
            "bn-IN" -> Locale("bn", "IN")
            else -> Locale.US
        }
        engine.language = locale
        engine.setSpeechRate(speechRate)
        engine.setPitch(pitch)
    }

    StudioModuleScaffold(
        title = "Bangla Text to Speech",
        subtitle = "Offline Bangla & English Speech + WAV Export",
        accentColor = Color(0xFFA855F7),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StudioSectionCard(title = "Voice Engine Status") {
                Text(banglaSupportText, style = MaterialTheme.typography.bodyMedium, color = StudioTeal)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedLocaleTag == "bn-BD", onClick = { selectedLocaleTag = "bn-BD" }, label = { Text("Bangla (BD)") })
                    FilterChip(selected = selectedLocaleTag == "bn-IN", onClick = { selectedLocaleTag = "bn-IN" }, label = { Text("Bangla (IN)") })
                    FilterChip(selected = selectedLocaleTag == "en-US", onClick = { selectedLocaleTag = "en-US" }, label = { Text("English") })
                }
            }

            StudioSectionCard(title = "Script & Voice Controls") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        textInput = "আমাদের দোকানে সুলভ মূল্যে পাসপোর্ট ছবি, এনআইডি সংশোধন এবং প্রিন্টিং সেবা পাওয়া যায়।"
                        selectedLocaleTag = "bn-BD"
                    }, modifier = Modifier.weight(1f)) { Text("Bangla Sample") }
                    OutlinedButton(onClick = {
                        textInput = "Welcome to Yunus Computer Studio Pro. Your documents and photos are ready."
                        selectedLocaleTag = "en-US"
                    }, modifier = Modifier.weight(1f)) { Text("English Sample") }
                }

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Enter Bangla or English Text") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Speech Speed: ${String.format("%.2f", speechRate)}×", style = MaterialTheme.typography.bodySmall)
                Slider(value = speechRate, onValueChange = { speechRate = it }, valueRange = 0.5f..2.0f)

                Text("Voice Pitch: ${String.format("%.2f", pitch)}×", style = MaterialTheme.typography.bodySmall)
                Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..2.0f)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val eng = ttsHolder[0]
                            if (!ttsReady || eng == null || textInput.isBlank()) {
                                scope.launch { snackbar.showSnackbar("Enter text and wait for TTS engine.") }
                                return@Button
                            }
                            applyTtsParams(eng)
                            eng.speak(textInput, TextToSpeech.QUEUE_FLUSH, Bundle(), "tts_speak_${System.currentTimeMillis()}")
                        },
                        enabled = ttsReady,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isSpeaking) "Speaking…" else "Speak")
                    }
                    OutlinedButton(
                        onClick = { ttsHolder[0]?.stop(); isSpeaking = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Stop, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Stop")
                    }
                }
            }

            StudioSectionCard(title = "Export Audio File (.WAV)") {
                Button(
                    onClick = {
                        val eng = ttsHolder[0] ?: return@Button
                        if (textInput.isBlank()) {
                            scope.launch { snackbar.showSnackbar("Enter text to synthesize.") }
                            return@Button
                        }
                        scope.launch {
                            val out = File(viewModel.repository.getExportsDir(), "bangla_tts_${System.currentTimeMillis()}.wav")
                            val ok = synthesizeTtsToFile(eng, textInput, selectedLocaleTag, speechRate, pitch, out)
                            if (ok && out.exists() && out.length() > 44) {
                                exportedWav = out
                                snackbar.showSnackbar("Synthesized WAV ready (${out.length() / 1024} KB)")
                            } else {
                                snackbar.showSnackbar("Device TTS engine could not synthesize WAV for this locale.")
                            }
                        }
                    },
                    enabled = ttsReady,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Generate & Export WAV")
                }

                exportedWav?.let { wav ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                val res = PrintShareHelper.saveFileToDownloads(context, wav, "audio/wav")
                                snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                            }
                        }, modifier = Modifier.weight(1f)) { Text("Save Downloads") }
                        OutlinedButton(onClick = {
                            PrintShareHelper.shareFile(context, wav, "audio/wav")
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Share, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Share WAV")
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private suspend fun synthesizeTtsToFile(
    tts: TextToSpeech,
    text: String,
    localeTag: String,
    rate: Float,
    pitch: Float,
    outFile: File
): Boolean = suspendCancellableCoroutine { cont ->
    val locale = when (localeTag) {
        "bn-BD" -> Locale("bn", "BD")
        "bn-IN" -> Locale("bn", "IN")
        else -> Locale.US
    }
    tts.language = locale
    tts.setSpeechRate(rate)
    tts.setPitch(pitch)
    val uid = "synth_${System.nanoTime()}"
    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) {
            if (utteranceId == uid && cont.isActive) cont.resume(true)
        }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            if (utteranceId == uid && cont.isActive) cont.resume(false)
        }
    })
    val res = tts.synthesizeToFile(text, Bundle(), outFile, uid)
    if (res != TextToSpeech.SUCCESS && cont.isActive) {
        cont.resume(false)
    }
}

// ---------------- MODULE 15: DUAL VOICE AUDIO STUDIO ----------------

private data class DialogueLine(
    val id: Long,
    val speaker: String, // "A" or "B"
    val text: String
)

@Composable
fun DualVoiceStudioScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val lines = remember {
        mutableStateListOf(
            DialogueLine(1L, "A", "স্বাগতম ইউনুস কম্পিউটার স্টুডিও প্রো-তে! আজকে আপনার কী সেবা প্রয়োজন?"),
            DialogueLine(2L, "B", "ধন্যবাদ! আমার জরুরি পাসপোর্ট ছবি এবং একটি সিভি প্রিন্ট করা দরকার।"),
            DialogueLine(3L, "A", "নিশ্চয়ই, ১০ মিনিটের মধ্যে আপনার ছবি ও সিভি প্রস্তুত হয়ে যাবে।")
        )
    }
    var nextLineId by remember { mutableIntStateOf(10) }
    var activeLineIndex by remember { mutableIntStateOf(-1) }
    var speakerALocale by remember { mutableStateOf("bn-BD") }
    var speakerAPitch by remember { mutableFloatStateOf(1.12f) }
    var speakerBLocale by remember { mutableStateOf("bn-BD") }
    var speakerBPitch by remember { mutableFloatStateOf(0.84f) }
    var gapMs by remember { mutableFloatStateOf(350f) }
    var ttsReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var mixedWavFile by remember { mutableStateOf<File?>(null) }

    val ttsHolder = remember { arrayOfNulls<TextToSpeech>(1) }
    DisposableEffect(context) {
        val eng = TextToSpeech(context) { if (it == TextToSpeech.SUCCESS) ttsReady = true }
        ttsHolder[0] = eng
        onDispose {
            eng.stop()
            eng.shutdown()
            ttsHolder[0] = null
        }
    }

    StudioModuleScaffold(
        title = "Dual Voice Audio Studio",
        subtitle = "2-Speaker Dialogue Playback & WAV Mixdown",
        accentColor = Color(0xFFD946EF),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StudioSectionCard(title = "Speaker A & Speaker B Voice Profiles") {
                Text("Speaker A Pitch: ${String.format("%.2f", speakerAPitch)}×", style = MaterialTheme.typography.bodySmall)
                Slider(value = speakerAPitch, onValueChange = { speakerAPitch = it }, valueRange = 0.6f..1.5f)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = speakerALocale == "bn-BD", onClick = { speakerALocale = "bn-BD" }, label = { Text("A: Bangla") })
                    FilterChip(selected = speakerALocale == "en-US", onClick = { speakerALocale = "en-US" }, label = { Text("A: English") })
                }

                Text("Speaker B Pitch: ${String.format("%.2f", speakerBPitch)}×", style = MaterialTheme.typography.bodySmall)
                Slider(value = speakerBPitch, onValueChange = { speakerBPitch = it }, valueRange = 0.6f..1.5f)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = speakerBLocale == "bn-BD", onClick = { speakerBLocale = "bn-BD" }, label = { Text("B: Bangla") })
                    FilterChip(selected = speakerBLocale == "en-US", onClick = { speakerBLocale = "en-US" }, label = { Text("B: English") })
                }

                Text("Turn Pause Gap: ${gapMs.toInt()} ms", style = MaterialTheme.typography.bodySmall)
                Slider(value = gapMs, onValueChange = { gapMs = it }, valueRange = 100f..1000f)
            }

            StudioSectionCard(
                title = "Dialogue Script (${lines.size} Lines)",
                trailing = {
                    OutlinedButton(onClick = {
                        val sp = if (lines.lastOrNull()?.speaker == "A") "B" else "A"
                        lines.add(DialogueLine(nextLineId++.toLong(), sp, ""))
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Add Line")
                    }
                }
            ) {
                lines.forEachIndexed { idx, line ->
                    Surface(
                        color = if (activeLineIndex == idx) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = line.speaker == "A",
                                        onClick = { lines[idx] = line.copy(speaker = "A") },
                                        label = { Text("Speaker A") }
                                    )
                                    FilterChip(
                                        selected = line.speaker == "B",
                                        onClick = { lines[idx] = line.copy(speaker = "B") },
                                        label = { Text("Speaker B") }
                                    )
                                }
                                if (lines.size > 1) {
                                    IconButton(onClick = { lines.removeAt(idx) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Remove line", tint = StudioRose)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = line.text,
                                onValueChange = { lines[idx] = line.copy(text = it) },
                                label = { Text("Line ${idx + 1} (${line.speaker})") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val eng = ttsHolder[0] ?: return@Button
                        if (!ttsReady) return@Button
                        scope.launch {
                            isPlaying = true
                            for ((idx, line) in lines.withIndex()) {
                                if (!isPlaying) break
                                if (line.text.isBlank()) continue
                                activeLineIndex = idx
                                val loc = if (line.speaker == "A") speakerALocale else speakerBLocale
                                val pt = if (line.speaker == "A") speakerAPitch else speakerBPitch
                                speakSuspend(eng, line.text, loc, 1.0f, pt)
                                kotlinx.coroutines.delay(gapMs.toLong())
                            }
                            activeLineIndex = -1
                            isPlaying = false
                        }
                    },
                    enabled = ttsReady && !isPlaying,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Play Dialogue")
                }
                OutlinedButton(
                    onClick = {
                        isPlaying = false
                        activeLineIndex = -1
                        ttsHolder[0]?.stop()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
            }

            Button(
                onClick = {
                    val eng = ttsHolder[0] ?: return@Button
                    scope.launch {
                        val tempFiles = mutableListOf<File>()
                        for ((idx, line) in lines.withIndex()) {
                            if (line.text.isBlank()) continue
                            val tmp = File(context.cacheDir, "dv_line_${idx}_${System.currentTimeMillis()}.wav")
                            val loc = if (line.speaker == "A") speakerALocale else speakerBLocale
                            val pt = if (line.speaker == "A") speakerAPitch else speakerBPitch
                            val ok = synthesizeTtsToFile(eng, line.text, loc, 1.0f, pt, tmp)
                            if (ok && tmp.exists() && tmp.length() > 44) tempFiles.add(tmp)
                        }
                        if (tempFiles.isEmpty()) {
                            snackbar.showSnackbar("Could not synthesize clips. Check TTS voice availability.")
                            return@launch
                        }
                        val out = File(viewModel.repository.getExportsDir(), "dual_voice_${System.currentTimeMillis()}.wav")
                        val res = withContext(Dispatchers.IO) { AudioTtsEngine.stitchWavFiles(tempFiles, gapMs.toInt(), out) }
                        tempFiles.forEach { it.delete() }
                        res.onSuccess {
                            mixedWavFile = it
                            snackbar.showSnackbar("Combined Dual-Voice WAV ready (${it.length() / 1024} KB)")
                        }.onFailure { snackbar.showSnackbar(it.message ?: "Mixdown failed") }
                    }
                },
                enabled = ttsReady,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Mixdown & Export Combined WAV")
            }

            mixedWavFile?.let { wav ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            val res = PrintShareHelper.saveFileToDownloads(context, wav, "audio/wav")
                            snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                        }
                    }, modifier = Modifier.weight(1f)) { Text("Save Downloads") }
                    OutlinedButton(onClick = {
                        PrintShareHelper.shareFile(context, wav, "audio/wav")
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Share Mixdown")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private suspend fun speakSuspend(
    tts: TextToSpeech,
    text: String,
    localeTag: String,
    rate: Float,
    pitch: Float
) = suspendCancellableCoroutine { cont ->
    tts.language = when (localeTag) {
        "bn-BD" -> Locale("bn", "BD")
        "bn-IN" -> Locale("bn", "IN")
        else -> Locale.US
    }
    tts.setSpeechRate(rate)
    tts.setPitch(pitch)
    val uid = "dv_${System.nanoTime()}"
    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) { if (utteranceId == uid && cont.isActive) cont.resume(Unit) }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) { if (utteranceId == uid && cont.isActive) cont.resume(Unit) }
    })
    val res = tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), uid)
    if (res != TextToSpeech.SUCCESS && cont.isActive) cont.resume(Unit)
}

// ---------------- MODULE 16: AUDIO EDITOR ----------------

@Composable
fun AudioEditorScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var rawAudio by remember { mutableStateOf<PcmAudio?>(null) }
    var processedAudio by remember { mutableStateOf<PcmAudio?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    var gain by remember { mutableFloatStateOf(1.0f) }
    var fadeIn by remember { mutableFloatStateOf(0f) }
    var fadeOut by remember { mutableFloatStateOf(0f) }
    var echo by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var reverse by remember { mutableStateOf(false) }
    var normalize by remember { mutableStateOf(false) }

    val activeTrackHolder = remember { arrayOfNulls<AudioTrack>(1) }
    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                activeTrackHolder[0]?.stop()
                activeTrackHolder[0]?.release()
            }
        }
    }

    fun startRecording() {
        scope.launch {
            isRecording = true
            val sampleRate = 22050
            val bufSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
            val recorder = try {
                AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize)
            } catch (e: SecurityException) {
                isRecording = false
                snackbar.showSnackbar("Microphone permission required.")
                return@launch
            }
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                isRecording = false
                recorder.release()
                snackbar.showSnackbar("Microphone unavailable on this device.")
                return@launch
            }

            val bytesOut = ByteArrayOutputStream()
            withContext(Dispatchers.IO) {
                val buf = ByteArray(bufSize)
                recorder.startRecording()
                while (isActive && isRecording) {
                    val read = recorder.read(buf, 0, buf.size)
                    if (read > 0) bytesOut.write(buf, 0, read)
                }
                runCatching { recorder.stop() }
                recorder.release()
            }

            val raw = bytesOut.toByteArray()
            if (raw.size >= 2) {
                val samples = ShortArray(raw.size / 2)
                ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
                rawAudio = PcmAudio(samples, sampleRate)
                trimStart = 0f; trimEnd = 1f
                snackbar.showSnackbar("Recorded ${String.format("%.1f", rawAudio!!.durationSeconds)}s audio.")
            }
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording()
        else scope.launch { snackbar.showSnackbar("Microphone permission denied.") }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) { AudioTtsEngine.decodeAudioUri(context, uri) }
                res.onSuccess {
                    rawAudio = it
                    trimStart = 0f; trimEnd = 1f
                    snackbar.showSnackbar("Loaded ${String.format("%.1f", it.durationSeconds)}s @ ${it.sampleRate} Hz")
                }.onFailure { snackbar.showSnackbar(it.message ?: "Failed to decode audio") }
            }
        }
    }

    LaunchedEffect(rawAudio, trimStart, trimEnd, gain, fadeIn, fadeOut, normalize, echo, speed, reverse) {
        val src = rawAudio ?: return@LaunchedEffect
        processedAudio = withContext(Dispatchers.Default) {
            AudioTtsEngine.processAudio(src, trimStart, trimEnd, gain, fadeIn, fadeOut, normalize, echo, speed, reverse)
        }
    }

    val waveformBars = remember(processedAudio) {
        processedAudio?.let { AudioTtsEngine.computeWaveformBars(it.samples, 68) } ?: emptyList()
    }

    StudioModuleScaffold(
        title = "Audio Editor",
        subtitle = "Mic Record, Trim, Gain, Fade, Echo, Speed & WAV",
        accentColor = Color(0xFF06B6D4),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (isRecording) {
                            isRecording = false
                        } else {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            if (granted) startRecording() else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = if (isRecording) ButtonDefaults.buttonColors(containerColor = StudioRose) else ButtonDefaults.buttonColors(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isRecording) "Stop Rec" else "Record Mic")
                }
                OutlinedButton(
                    onClick = { audioPicker.launch(arrayOf("audio/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.AudioFile, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Import Audio")
                }
            }

            StudioSectionCard(
                title = "Waveform Preview",
                trailing = {
                    processedAudio?.let {
                        Text(
                            "${String.format("%.2f", it.durationSeconds)}s",
                            fontFamily = MonoFontFamily,
                            color = StudioTeal
                        )
                    }
                }
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                ) {
                    if (waveformBars.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Record microphone or import MP3/WAV/M4A", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val barW = size.width / waveformBars.size
                            val midY = size.height / 2f
                            waveformBars.forEachIndexed { idx, amp ->
                                val x = idx * barW + barW / 2f
                                val halfH = (amp * size.height * 0.45f).coerceAtLeast(3f)
                                drawLine(
                                    color = Color(0xFF06B6D4),
                                    start = Offset(x, midY - halfH),
                                    end = Offset(x, midY + halfH),
                                    strokeWidth = (barW * 0.62f).coerceAtLeast(2f)
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val aud = processedAudio ?: return@Button
                            runCatching {
                                activeTrackHolder[0]?.stop()
                                activeTrackHolder[0]?.release()
                            }
                            if (isPlaying) {
                                isPlaying = false
                                return@Button
                            }
                            isPlaying = true
                            val track = AudioTtsEngine.createAudioTrackPlayer(aud)
                            activeTrackHolder[0] = track
                            track.play()
                            scope.launch {
                                kotlinx.coroutines.delay((aud.durationSeconds * 1000).toLong().coerceAtLeast(200))
                                isPlaying = false
                            }
                        },
                        enabled = processedAudio != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPlaying) "Stop" else "Play Processed")
                    }
                    OutlinedButton(
                        onClick = {
                            val aud = processedAudio ?: return@OutlinedButton
                            scope.launch {
                                val out = File(viewModel.repository.getExportsDir(), "edited_audio_${System.currentTimeMillis()}.wav")
                                withContext(Dispatchers.IO) { AudioTtsEngine.writeWav(aud, out) }
                                val res = PrintShareHelper.saveFileToDownloads(context, out, "audio/wav")
                                snackbar.showSnackbar(res.getOrElse { it.message ?: "Saved" })
                                PrintShareHelper.shareFile(context, out, "audio/wav")
                            }
                        },
                        enabled = processedAudio != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Export WAV")
                    }
                }
            }

            StudioSectionCard(title = "Trim & DSP Effects") {
                Text("Trim Start: ${(trimStart * 100).toInt()}%  |  Trim End: ${(trimEnd * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(value = trimStart, onValueChange = { if (it < trimEnd - 0.05f) trimStart = it }, valueRange = 0f..0.95f)
                Slider(value = trimEnd, onValueChange = { if (it > trimStart + 0.05f) trimEnd = it }, valueRange = 0.05f..1f)

                Text("Gain / Volume: ${String.format("%.2f", gain)}×", style = MaterialTheme.typography.bodySmall)
                Slider(value = gain, onValueChange = { gain = it }, valueRange = 0.25f..2.5f)

                Text("Speed: ${String.format("%.2f", speed)}×", style = MaterialTheme.typography.bodySmall)
                Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.5f..2.0f)

                Text("Fade In: ${String.format("%.1f", fadeIn)}s  |  Fade Out: ${String.format("%.1f", fadeOut)}s", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Slider(value = fadeIn, onValueChange = { fadeIn = it }, valueRange = 0f..2.5f, modifier = Modifier.weight(1f))
                    Slider(value = fadeOut, onValueChange = { fadeOut = it }, valueRange = 0f..2.5f, modifier = Modifier.weight(1f))
                }

                Text("Studio Echo: ${(echo * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(value = echo, onValueChange = { echo = it }, valueRange = 0f..0.65f)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Normalize Peak Volume")
                    Switch(checked = normalize, onCheckedChange = { normalize = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Reverse Audio")
                    Switch(checked = reverse, onCheckedChange = { reverse = it })
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
