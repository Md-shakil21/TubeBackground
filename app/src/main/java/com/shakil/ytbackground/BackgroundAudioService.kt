package com.shakil.ytbackground

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle

class BackgroundAudioService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentTitle: String = "YouTube Video"
    private var isCurrentlyPlaying: Boolean = true

    companion object {
        const val CHANNEL_ID = "yt_background_channel_v2"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.shakil.ytbackground.ACTION_START"
        const val ACTION_UPDATE = "com.shakil.ytbackground.ACTION_UPDATE"
        const val ACTION_TOGGLE_PLAY = "com.shakil.ytbackground.ACTION_TOGGLE_PLAY"
        const val ACTION_STOP = "com.shakil.ytbackground.ACTION_STOP"

        const val BROADCAST_PLAY_PAUSE = "com.shakil.ytbackground.BROADCAST_PLAY_PAUSE"
        const val BROADCAST_STOP = "com.shakil.ytbackground.BROADCAST_STOP"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        var isServiceRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        setupMediaSession()
        requestAudioFocus()
        acquireWakeLock()
        isServiceRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "YouTube Video"
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                currentTitle = title
                isCurrentlyPlaying = isPlaying
                updatePlaybackState()
                startForegroundWithNotification()
            }
            ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE)
                if (!title.isNullOrBlank()) {
                    currentTitle = title
                }
                isCurrentlyPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, isCurrentlyPlaying)
                updatePlaybackState()
                updateNotification()
            }
            ACTION_TOGGLE_PLAY -> {
                sendBroadcast(Intent(BROADCAST_PLAY_PAUSE).setPackage(packageName))
                isCurrentlyPlaying = !isCurrentlyPlaying
                updatePlaybackState()
                updateNotification()
            }
            ACTION_STOP -> {
                sendBroadcast(Intent(BROADCAST_STOP).setPackage(packageName))
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "TubeBackgroundSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    sendBroadcast(Intent(BROADCAST_PLAY_PAUSE).setPackage(packageName))
                    isCurrentlyPlaying = true
                    updatePlaybackState()
                    updateNotification()
                }

                override fun onPause() {
                    sendBroadcast(Intent(BROADCAST_PLAY_PAUSE).setPackage(packageName))
                    isCurrentlyPlaying = false
                    updatePlaybackState()
                    updateNotification()
                }

                override fun onStop() {
                    sendBroadcast(Intent(BROADCAST_STOP).setPackage(packageName))
                    stopSelf()
                }
            })
        }
        updatePlaybackState()
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { /* Keep playing */ }
                .build()
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun updatePlaybackState() {
        val state = if (isCurrentlyPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
        )
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePlayIntent = Intent(this, BackgroundAudioService::class.java).apply {
            action = ACTION_TOGGLE_PLAY
        }
        val togglePlayPendingIntent = PendingIntent.getService(
            this,
            1,
            togglePlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BackgroundAudioService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isCurrentlyPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isCurrentlyPlaying) getString(R.string.action_pause) else getString(R.string.action_play)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(currentTitle)
            .setContentText(if (isCurrentlyPlaying) getString(R.string.playback_active) else getString(R.string.playback_paused))
            .setContentIntent(openPendingIntent)
            .setOngoing(isCurrentlyPlaying)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseIcon, playPauseTitle, togglePlayPendingIntent)
            .addAction(R.drawable.ic_stop, getString(R.string.action_stop), stopPendingIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1)
            )

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TubeBackground::AudioWakeLock"
        ).apply {
            acquire()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    override fun onDestroy() {
        isServiceRunning = false
        mediaSession?.release()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
