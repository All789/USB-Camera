package com.example.usb

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Monitors USB Audio Class (UAC) HDMI capture audio through Android's AudioRecord/AudioDeviceInfo
 * without opening a conflicting UsbDeviceConnection on usbfs while libuvc/libusb streams video.
 */
class UsbAudioMonitor(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val tag = "UsbAudioMonitor"

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var audioJob: Job? = null

    private val _isAudioActive = MutableStateFlow(false)
    val isAudioActive: StateFlow<Boolean> = _isAudioActive.asStateFlow()

    private val _audioPeakLevel = MutableStateFlow(0f)
    val audioPeakLevel: StateFlow<Float> = _audioPeakLevel.asStateFlow()

    private var isMuted = false
    private var volume = 0.85f

    @SuppressLint("MissingPermission")
    fun startAudio() {
        stopAudio()

        val hasRecordPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasRecordPermission) {
            Log.d(tag, "RECORD_AUDIO not granted; skipping USB audio loopback (video unaffected)")
            return
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val usbInputDevice = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .firstOrNull {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }

        if (usbInputDevice == null) {
            Log.d(tag, "No USB audio input device registered by Android AudioManager")
            return
        }

        try {
            val sampleRate = 48000
            val inChannel = AudioFormat.CHANNEL_IN_STEREO
            val outChannel = AudioFormat.CHANNEL_OUT_STEREO
            val encoding = AudioFormat.ENCODING_PCM_16BIT

            val recMin = AudioRecord.getMinBufferSize(sampleRate, inChannel, encoding).coerceAtLeast(4096)
            val playMin = AudioTrack.getMinBufferSize(sampleRate, outChannel, encoding).coerceAtLeast(4096)

            val recorder = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(inChannel)
                        .build()
                )
                .setBufferSizeInBytes(recMin * 2)
                .build()

            recorder.preferredDevice = usbInputDevice

            val player = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(outChannel)
                        .build()
                )
                .setBufferSizeInBytes(playMin * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (recorder.state != AudioRecord.STATE_INITIALIZED || player.state != AudioTrack.STATE_INITIALIZED) {
                recorder.release()
                player.release()
                Log.w(tag, "AudioRecord/AudioTrack failed to initialize")
                return
            }

            audioRecord = recorder
            audioTrack = player
            updateVolume()

            recorder.startRecording()
            player.play()
            _isAudioActive.value = true

            audioJob = coroutineScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(recMin)
                while (isActive && _isAudioActive.value) {
                    try {
                        val readBytes = recorder.read(buffer, 0, buffer.size)
                        if (readBytes > 0) {
                            if (!isMuted) {
                                player.write(buffer, 0, readBytes)
                            }
                            var maxSample = 0
                            var i = 0
                            while (i < readBytes - 1) {
                                val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort().toInt()
                                val abs = kotlin.math.abs(sample)
                                if (abs > maxSample) maxSample = abs
                                i += 2
                            }
                            _audioPeakLevel.value = (maxSample / 32768f).coerceIn(0f, 1f)
                        } else {
                            delay(20)
                        }
                    } catch (t: Throwable) {
                        if (t is kotlinx.coroutines.CancellationException) throw t
                        Log.w(tag, "USB audio loopback warning: ${t.message}")
                        delay(50)
                    }
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "Could not start USB audio monitor: ${e.message}")
            stopAudio()
        }
    }

    fun setMuted(muted: Boolean) {
        this.isMuted = muted
        updateVolume()
    }

    fun setVolume(vol: Float) {
        this.volume = vol.coerceIn(0f, 1f)
        updateVolume()
    }

    private fun updateVolume() {
        val effectiveVol = if (isMuted) 0f else volume
        try {
            audioTrack?.setVolume(effectiveVol)
        } catch (e: Exception) {
            Log.w(tag, "Could not set volume: ${e.message}")
        }
    }

    fun stopAudio() {
        _isAudioActive.value = false
        _audioPeakLevel.value = 0f
        audioJob?.cancel()
        audioJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Throwable) {
        }
        audioRecord = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Throwable) {
        }
        audioTrack = null
    }
}
