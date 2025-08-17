package org.nunocky.kodama

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class AudioFileInputStreamTest {

    @Before
    fun setUp() {
    }

    @Test
    fun testFileAudioInputStream() {
        val context = InstrumentationRegistry.getInstrumentation().context

        // アセット sample_voice01.wavのパスを取得
        val assetManager = context.assets
        val voiceAssest = assetManager.open("sample_voice01.wav")

        val inputStream = FileStreamVoiceActivityDetection(voiceAssest)

        var lastSpeech = false
        inputStream.addListener(object : AudioInputStream.Listener() {
            override fun onFrame(bytes: ByteArray, isSpeech: Boolean) {
                if (isSpeech != lastSpeech) {
                    Log.d("TEST", "onFrame: isSpeech=$isSpeech")
                    lastSpeech = isSpeech
                }
            }
        })

        // Execute start method
        inputStream.start()
    }
}