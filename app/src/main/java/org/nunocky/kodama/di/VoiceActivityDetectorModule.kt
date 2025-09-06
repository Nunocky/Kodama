package org.nunocky.kodama.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.nunocky.kodama.vad.MicStreamVoiceActivityDetector
import org.nunocky.kodama.vad.VoiceActivityDetector

@Module
@InstallIn(SingletonComponent::class)
abstract class VoiceActivityDetectorModule {
    @Binds
    abstract fun bindMicStreamVoiceActivityDetection(
        impl: MicStreamVoiceActivityDetector
    ): VoiceActivityDetector
}