package com.shakil.ytbackground

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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
    private var activityWakeLock: PowerManager.WakeLock? = null

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
        acquireActivityWakeLock()
        checkBatteryOptimization()
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

    @SuppressLint("BatteryLife")
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Ignore if device doesn't support intent
                }
            }
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireActivityWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        activityWakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TubeBackground::ActivityWakeLock"
        ).apply {
            acquire()
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
                injectBackgroundScript()
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

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                injectBackgroundScript()
            }
        }
    }

    private fun injectBackgroundScript() {
        val js = """
            (function() {
                if (window.__yt_bg_fix_installed) return;
                window.__yt_bg_fix_installed = true;

                // 1. Force Page Visibility API to always evaluate to visible
                try {
                    Object.defineProperties(document, {
                        'hidden': { get: function() { return false; }, configurable: true },
                        'visibilityState': { get: function() { return 'visible'; }, configurable: true },
                        'webkitHidden': { get: function() { return false; }, configurable: true },
                        'webkitVisibilityState': { get: function() { return 'visible'; }, configurable: true }
                    });
                } catch(e) {}

                // 2. Prevent YouTube from attaching visibilitychange listeners
                try {
                    var origAddEventListener = EventTarget.prototype.addEventListener;
                    EventTarget.prototype.addEventListener = function(type, listener, options) {
                        if (type === 'visibilitychange' || type === 'webkitvisibilitychange') {
                            return; // Silently drop visibility listeners!
                        }
                        return origAddEventListener.call(this, type, listener, options);
                    };
                } catch(e) {}

                // 3. Block visibility events from bubbling
                var blockEvent = function(e) {
                    e.stopImmediatePropagation();
                };
                ['visibilitychange', 'webkitvisibilitychange', 'blur', 'pagehide'].forEach(function(ev) {
                    window.addEventListener(ev, blockEvent, true);
                    document.addEventListener(ev, blockEvent, true);
                });

                // 4. Auto-resume when video is paused by system/screen lock (not user)
                window.__userPaused = false;

                document.addEventListener('pause', function(e) {
                    if (e.target && e.target.tagName === 'VIDEO') {
                        var video = e.target;
                        if (!window.__userPaused && !video.ended) {
                            setTimeout(function() {
                                if (!window.__userPaused && video.paused) {
                                    video.play();
                                }
                            }, 100);
                        }
                    }
                }, true);

                // 5. Periodic monitor: keep video playing and dismiss "Still watching?" dialog
                setInterval(function() {
                    try {
                        var video = document.querySelector('video');
                        if (video) {
                            var isPlaying = !video.paused && !video.ended && video.readyState > 2;
                            var title = document.title ? document.title.replace(' - YouTube', '').trim() : 'YouTube';
                            if (window.AndroidInterface) {
                                window.AndroidInterface.onPlaybackStateChanged(isPlaying, title);
                            }

                            // If paused without user consent while still supposed to play:
                            if (!window.__userPaused && video.paused && !video.ended && video.currentTime > 0) {
                                video.play();
                            }
                        }

                        // Auto-dismiss "Video paused. Continue watching?" prompt
                        var confirmBtn = document.querySelector('yt-confirm-dialog-renderer #confirm-button button') ||
                                         document.querySelector('.yt-spec-button-shape-next--filled');
                        if (confirmBtn && confirmBtn.offsetParent !== null) {
                            confirmBtn.click();
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
                            window.__userPaused = false;
                            video.play();
                        } else {
                            window.__userPaused = true;
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
                    window.__userPaused = true;
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
        // DO NOT pause WebView!
        injectBackgroundScript()
    }

    override fun onStop() {
        super.onStop()
        injectBackgroundScript()
        if (isVideoPlaying) {
            startOrUpdateService(true)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(serviceBroadcastReceiver)
        } catch (e: Exception) {}
        if (activityWakeLock?.isHeld == true) {
            activityWakeLock?.release()
        }
        if (!isVideoPlaying) {
            stopService(Intent(this, BackgroundAudioService::class.java))
        }
        super.onDestroy()
    }
}
