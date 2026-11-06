package com.calmloop.sounds.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.calmloop.sounds.MainActivity
import com.calmloop.sounds.R
import com.calmloop.sounds.audio.SoundCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Ține sunetele pornite și cu ecranul stins, cu notificare de control. */
class PlaybackService : Service() {

    companion object {
        @Volatile var isRunning = false
            private set
        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 42
        private const val ACTION_TOGGLE = "com.calmloop.sounds.TOGGLE"
        private const val ACTION_STOP = "com.calmloop.sounds.STOP"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground(build(PlaybackController.state.value))
        when (intent?.action) {
            ACTION_TOGGLE -> PlaybackController.togglePause()
            ACTION_STOP -> PlaybackController.stopAll()
        }
        if (!started) {
            started = true
            scope.launch {
                PlaybackController.state.collect { s ->
                    if (!s.isActive) {
                        ServiceCompat.stopForeground(this@PlaybackService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIFICATION_ID, build(s))
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    private fun goForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun servicePending(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this,
            code,
            Intent(this, PlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun build(s: PlaybackState): Notification {
        val names = s.sounds.keys.mapNotNull { SoundCatalog.byId(it)?.let { d -> getString(d.name) } }.toMutableList()
        s.track?.let { names.add(it.name) }
        val text = if (names.isEmpty()) getString(R.string.app_name) else names.joinToString(" · ")
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(if (s.paused) R.string.notif_paused else R.string.notif_playing))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(!s.paused)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                0,
                getString(if (s.paused) R.string.action_resume else R.string.action_pause),
                servicePending(ACTION_TOGGLE, 1),
            )
            .addAction(0, getString(R.string.action_stop), servicePending(ACTION_STOP, 2))
            .build()
    }
}
