package com.shakil.ytbackground

import android.webkit.JavascriptInterface

class WebAppInterface(private val listener: PlaybackListener) {

    interface PlaybackListener {
        fun onPlaybackStateChanged(isPlaying: Boolean, title: String)
    }

    @JavascriptInterface
    fun onPlaybackStateChanged(isPlaying: Boolean, title: String) {
        listener.onPlaybackStateChanged(isPlaying, title)
    }
}
