/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
* limitations under the License.
 */
package com.example.exoplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.VideoSize
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import com.example.exoplayer.databinding.ActivityPlayerBinding
import kotlinx.coroutines.*


/**
 * A fullscreen activity to play audio or video streams.
 * Adds a basic analytics component that reports the following playback events:

    - A single event when playback begins.
        Assumption: This means anytime playback starts during the duration
    - An event sent every second that includes the current player position and information about the selected track(s).
    - An event when the bitrate changes that includes information about the previous and new bitrate.

 Test Videos:
        https://gist.github.com/jsturgis/3b19447b304616f18657
 */
class PlayerActivity : AppCompatActivity() {

    private val scope = MainScope() // could also use an other scope such as viewModelScope if available
    var job: Job? = null

    companion object {
        const val TAG = "PlayerActivity"
    }
    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L
    var exoLogPlayer: ExoPlayer? = null


    private val playbackStateListener: ComcastPlayer.Listener = playbackStateListener()

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }
    private fun initializePlayer() {
        exoLogPlayer = ExoPlayer.Builder(this)
            .build()
            .also { comcastPlayer ->
                viewBinding.videoView.player = comcastPlayer

                val mediaItem = MediaItem.fromUri(getString(R.string.media_url_tears_mp4))
                comcastPlayer.setMediaItem(mediaItem)

                comcastPlayer.playWhenReady = playWhenReady
                comcastPlayer.seekTo(currentItem, playbackPosition)
                comcastPlayer.addListener(playbackStateListener)

                comcastPlayer.prepare()
                startLoggerTimer(mediaItem.mediaMetadata)//
            }
    }
    private fun playbackStateListener() = object : ComcastPlayer.Listener {

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            Log.d(TAG, "onRenderedFirstFrame asif")

        }
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            Log.d(TAG, "onVideoSizeChanged asif videoSize $videoSize")

        }


        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            if(stateString.contains("STATE_READY")){
                Log.d(TAG, "changed state to streaming in progress...")
            }
            Log.d(TAG, "changed state to $stateString")
            //Log.d(TAG, "changed state to playbackPosition $playbackPosition")
            Log.d(TAG, "minVideoBitrate ${exoLogPlayer?.trackSelectionParameters?.minVideoBitrate}")
            Log.d(TAG, "maxVideoBitrate ${exoLogPlayer?.trackSelectionParameters?.maxVideoBitrate}")
            //Log.d(TAG, "changed state to minVideoBitrate ${exoPlayerLog?.trackSelectionParameters?.bit}")



        }
    }

    public override fun onStart() {
        super.onStart()
        //Multiple window support
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }
    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        //TODO: Document this
        if ((Util.SDK_INT <= 23 || exoLogPlayer == null)) {
            initializePlayer()
        }
    }
    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, viewBinding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        stopLoggerTimer()
        exoLogPlayer?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()
        }
        exoLogPlayer = null
    }

    private fun startLoggerTimer(mediaMetadata: MediaMetadata) {
        stopLoggerTimer()
        //Start coroutine in its scope to log events
        job = scope.launch {

            while(job?.isActive == true) {
                Log.d(TAG, "currentPosition ${exoLogPlayer?.currentPosition}")
                Log.d(TAG, "trackNumber ${mediaMetadata.artist} ${mediaMetadata.displayTitle}")
                delay(1000)
            }
        }
    }

    private fun stopLoggerTimer() {
        job?.cancel()
        job = null
    }
}