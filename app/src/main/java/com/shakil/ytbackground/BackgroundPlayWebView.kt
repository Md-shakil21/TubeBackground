package com.shakil.ytbackground

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.webkit.WebView

class BackgroundPlayWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    override fun onWindowVisibilityChanged(visibility: Int) {
        // Android calls onWindowVisibilityChanged(View.GONE) when screen locks or turns off.
        // Chromium's internal engine pauses HTML5 video/audio when it receives View.GONE.
        // By always passing View.VISIBLE, Chromium never pauses playback!
        super.onWindowVisibilityChanged(View.VISIBLE)
    }

    override fun dispatchWindowVisibilityChanged(visibility: Int) {
        super.dispatchWindowVisibilityChanged(View.VISIBLE)
    }
}
