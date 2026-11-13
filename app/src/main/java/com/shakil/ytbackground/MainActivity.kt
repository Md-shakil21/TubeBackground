package com.shakil.ytbackground

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shakil.ytbackground.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), WebAppInterface.PlaybackListener {

    private lateinit var binding: ActivityMainBinding
    private var isVideoPlaying: Boolean = false
    private var currentVideoTitle: String = "YouTube Video"

    private val userAgentString =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val serviceBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BackgroundAudioService.BROADCAST_PLAY_PAUSE -> togglePlayPause()
                BackgroundAudioService.BROADCAST_STOP -> stopPlayback()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestRequiredPermissions()
        setupNavigationControls()
        setupWebView()
        setupBackNavigation()
        registerServiceReceiver()

        // Load YouTube Mobile
        binding.webView.loadUrl("https://m.youtube.com")
    }

    private fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun setupNavigationControls() {
        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            }
        }

        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            }
        }

        binding.btnHome.setOnClickListener {
            binding.webView.loadUrl("https://m.youtube.com")
        }

        binding.btnRefresh.setOnClickListener {
            binding.webView.reload()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = binding.webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.userAgentString = userAgentString
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        binding.webView.addJavascriptInterface(WebAppInterface(this), "AndroidInterface")

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = newProgress
                } else {
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank() && !title.contains("m.youtube.com")) {
                    val cleanTitle = title.replace(" - YouTube", "").trim()
                    binding.tvStatus.text = cleanTitle
                    currentVideoTitle = cleanTitle
                }
            }
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false
                }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                injectBackgroundScript()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectBackgroundScript()
            }
        }
    }

    private fun injectBackgroundScript() {
        val js = """
            (function() {
                if (window.__yt_bg_injected) return;
                window.__yt_bg_injected = true;

                // Keep visibility always active
                Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                Object.defineProperty(document, 'webkitVisibilityState', { get: function() { return 'visible'; }, configurable: true });
                Object.defineProperty(document, 'webkitHidden', { get: function() { return false; }, configurable: true });

                var suppressEvent = function(e) {
                    e.stopImmediatePropagation();
                };
                ['visibilitychange', 'webkitvisibilitychange', 'blur', 'pagehide'].forEach(function(ev) {
                    window.addEventListener(ev, suppressEvent, true);
                    document.addEventListener(ev, suppressEvent, true);
                });

                // Detect video playback status
                setInterval(function() {
                    try {
                        var video = document.querySelector('video');
                        if (video && window.AndroidInterface) {
                            var isPlaying = !video.paused && !video.ended && video.readyState > 2;
                            var title = document.title ? document.title.replace(' - YouTube', '').trim() : 'YouTube';
                            window.AndroidInterface.onPlaybackStateChanged(isPlaying, title);
                        }
                    } catch(e) {}
                }, 1000);
            })();
        """.trimIndent()

        binding.webView.evaluateJavascript(js, null)
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean, title: String) {
        runOnUiThread {
            isVideoPlaying = isPlaying
            if (title.isNotBlank() && title != "YouTube") {
                currentVideoTitle = title
                binding.tvStatus.text = title
            }

            // Sync with background foreground service
            if (isPlaying) {
                startOrUpdateService(true)
            } else if (BackgroundAudioService.isServiceRunning) {
                startOrUpdateService(false)
            }
        }
    }

    private fun startOrUpdateService(isPlaying: Boolean) {
        val intent = Intent(this, BackgroundAudioService::class.java).apply {
            action = if (BackgroundAudioService.isServiceRunning) {
                BackgroundAudioService.ACTION_UPDATE
            } else {
                BackgroundAudioService.ACTION_START
            }
            putExtra(BackgroundAudioService.EXTRA_TITLE, currentVideoTitle)
            putExtra(BackgroundAudioService.EXTRA_IS_PLAYING, isPlaying)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }

    private fun togglePlayPause() {
        runOnUiThread {
            val js = """
                (function() {
                    var video = document.querySelector('video');
                    if (video) {
                        if (video.paused) {
                            video.play();
                        } else {
                            video.pause();
                        }
                    }
                })();
            """.trimIndent()
            binding.webView.evaluateJavascript(js, null)
        }
    }

    private fun stopPlayback() {
        runOnUiThread {
            val js = """
                (function() {
                    var video = document.querySelector('video');
                    if (video) {
                        video.pause();
                    }
                })();
            """.trimIndent()
            binding.webView.evaluateJavascript(js, null)
            stopService(Intent(this, BackgroundAudioService::class.java))
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    // Minimize app instead of killing so music keeps playing
                    moveTaskToBack(true)
                }
            }
        })
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerServiceReceiver() {
        val filter = IntentFilter().apply {
            addAction(BackgroundAudioService.BROADCAST_PLAY_PAUSE)
            addAction(BackgroundAudioService.BROADCAST_STOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceBroadcastReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        // DO NOT call binding.webView.onPause() here!
        // Leaving WebView active ensures audio continues playing when screen turns off.
    }

    override fun onStop() {
        super.onStop()
        // Ensure background service is running if video was playing
        if (isVideoPlaying) {
            startOrUpdateService(true)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(serviceBroadcastReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
        if (!isVideoPlaying) {
            stopService(Intent(this, BackgroundAudioService::class.java))
        }
        super.onDestroy()
    }
}
