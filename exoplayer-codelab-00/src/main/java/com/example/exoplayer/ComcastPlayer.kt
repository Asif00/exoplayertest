package com.example.exoplayer

import androidx.media3.common.Player

/**
 * Generic ComcastPlayer interface to use for ALL Comcast needs therefor must encompass high
 * level functions only and details must be delegated to specific implementations
 */
interface ComcastPlayer {
    interface Listener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int)
    }
}
