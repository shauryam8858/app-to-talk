package com.example.echo

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.util.Locale

/**
 * Echo: shows back exactly what you type (or say). No AI, no network.
 * Uses only the plain Android SDK, so there are no libraries to download.
 */
class MainActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var output: TextView
    private lateinit var status: TextView
    private lateinit var micButton: Button

    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeak: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        input = findViewById(R.id.inputText)
        output = findViewById(R.id.outputText)
        status = findViewById(R.id.statusText)
        micButton = findViewById(R.id.micButton)

        findViewById<Button>(R.id.echoButton).setOnClickListener { echo() }
        findViewById<Button>(R.id.clearButton).setOnClickListener { clearAll() }
        findViewById<Button>(R.id.copyButton).setOnClickListener { copyOutput() }
        findViewById<Button>(R.id.speakButton).setOnClickListener { speakOutput() }
        micButton.setOnClickListener { onMicClicked() }
    }

    override fun onStop() {
        super.onStop()
        if (listening) finishListening()
        tts?.stop()
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------- text echo

    private fun echo() {
        val text = input.text.toString() // exact copy, nothing changed
        if (text.isEmpty()) {
            output.text = ""
            showStatus(R.string.nothing_to_echo)
        } else {
            output.text = text
            status.text = ""
        }
    }

    private fun clearAll() {
        if (listening) finishListening()
        tts?.stop()
        input.setText("")
        output.text = ""
        status.text = ""
    }

    private fun copyOutput() {
        val text = output.text.toString()
        if (text.isEmpty()) {
            showStatus(R.string.nothing_to_copy)
            return
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Echo", text))
        showStatus(R.string.copied)
    }

    private fun showStatus(resId: Int) {
        status.setText(resId)
    }

    // ---------------------------------------------------------- voice (offline)

    private fun onMicClicked() {
        if (listening) {
            finishListening()
            status.text = ""
            return
        }
        // Before Android 13 there is no way to force on-device recognition, so
        // the app refuses rather than risk silently using an online service.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            showStatus(R.string.mic_needs_android13)
            return
        }
        startOfflineListening()
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private fun startOfflineListening() {
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            showStatus(R.string.mic_no_offline)
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC)
            return
        }

        val r = SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onResults(results: Bundle?) {
                val heard = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                finishListening()
                if (heard.isNullOrEmpty()) {
                    showStatus(R.string.heard_nothing)
                } else {
                    input.setText(heard) // exactly as returned by the recognizer
                    output.text = heard
                    status.text = ""
                }
            }

            override fun onError(error: Int) {
                finishListening()
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> showStatus(R.string.heard_nothing)
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> showStatus(R.string.mic_language_missing)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> showStatus(R.string.mic_denied)
                    else -> status.text = getString(R.string.mic_error, error)
                }
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        listening = true
        micButton.setText(R.string.mic_stop)
        showStatus(R.string.listening)
        r.startListening(intent)
    }

    private fun finishListening() {
        recognizer?.destroy()
        recognizer = null
        listening = false
        micButton.setText(R.string.mic)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_MIC) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMicClicked()
            } else {
                showStatus(R.string.mic_denied)
            }
        }
    }

    // ------------------------------------------------------- text-to-speech

    private fun speakOutput() {
        val text = output.text.toString()
        if (text.isEmpty()) {
            showStatus(R.string.nothing_to_speak)
            return
        }
        if (tts == null) {
            pendingSpeak = text
            tts = TextToSpeech(this) { code ->
                if (code == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    pendingSpeak?.let { speakNow(it) }
                } else {
                    tts = null
                    showStatus(R.string.tts_unavailable)
                }
                pendingSpeak = null
            }
        } else if (ttsReady) {
            speakNow(text)
        }
    }

    private fun speakNow(text: String) {
        val engine = tts ?: return
        // Devanagari characters -> Hindi voice, otherwise the phone's language.
        val hasHindi = text.any { it in '\u0900'..'\u097F' }
        val locale = if (hasHindi) Locale("hi", "IN") else Locale.getDefault()

        // Only voices that work without internet and are already installed.
        val voice = engine.voices?.firstOrNull {
            !it.isNetworkConnectionRequired &&
                it.locale.language == locale.language &&
                !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)
        }
        if (voice == null) {
            showStatus(R.string.tts_no_offline_voice)
            return
        }
        engine.voice = voice
        status.text = ""
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "echo")
    }

    companion object {
        private const val REQ_MIC = 1001
    }
}
