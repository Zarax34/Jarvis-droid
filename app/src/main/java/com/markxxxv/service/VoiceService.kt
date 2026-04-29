package com.markxxxv.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.IBinder
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

class VoiceService : Service() {
    
    private val binder = VoiceBinder()
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    
    private var audioCallback: ((ByteArray) -> Unit)? = null
    
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    inner class VoiceBinder : Binder() {
        fun getService(): VoiceService = this@VoiceService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        initializeAudioRecord()
    }
    
    private fun initializeAudioRecord() {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 2
        )
    }
    
    fun startListening(onAudio: (ByteArray) -> Unit) {
        if (audioRecord == null) {
            initializeAudioRecord()
        }
        
        audioCallback = onAudio
        _isListening.value = true
        
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            recordAudio()
        }
    }
    
    private suspend fun recordAudio() {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val buffer = ShortArray(bufferSize)
        val outputStream = ByteArrayOutputStream()
        
        audioRecord?.startRecording()
        
        while (_isListening.value && isActive) {
            val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0
            
            if (bytesRead > 0) {
                val audioData = shortArrayToByteArray(buffer, bytesRead)
                outputStream.write(audioData)
                
                // Send chunks for real-time processing
                if (outputStream.size() >= 1024) {
                    audioCallback?.invoke(outputStream.toByteArray())
                    outputStream.reset()
                }
            }
            
            delay(10)
        }
        
        audioRecord?.stop()
        
        // Send remaining data
        if (outputStream.size() > 0) {
            audioCallback?.invoke(outputStream.toByteArray())
        }
        outputStream.close()
    }
    
    fun stopListening() {
        _isListening.value = false
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.stop()
    }
    
    private fun shortArrayToByteArray(shorts: ShortArray, length: Int): ByteArray {
        val bytes = ByteArray(length * 2)
        for (i in 0 until length) {
            bytes[i * 2] = (shorts[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = (shorts[i].toInt() shr 8 and 0xFF).toByte()
        }
        return bytes
    }
    
    override fun onDestroy() {
        stopListening()
        audioRecord?.release()
        audioRecord = null
        super.onDestroy()
    }
}