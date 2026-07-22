package com.haveit.app.notification

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.haveit.app.HaveItApplication
import com.haveit.app.data.local.entity.CheckInEntity
import com.haveit.app.data.local.entity.HabitEntity
import com.haveit.app.widget.HabitWidget
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that produces the actual "alarm" experience: a looping alarm ringtone plus
 * continuous vibration, fronted by a full-screen-intent notification that launches [AlarmActivity].
 *
 * Sound and vibration are owned here (not on the notification channel) because a channel plays its
 * sound/vibration only once per post — that is the single blip we're trying to get away from. Owning
 * a [MediaPlayer] and driving the [Vibrator] directly lets the alarm ring until the user acts, the
 * screen is turned off, or the safety cap elapses, and stop the instant they hit 완료.
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var screenOffReceiver: BroadcastReceiver? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val autoStop = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stopAlarm() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopAlarm()
            ACTION_DONE -> markDone(intent.getStringExtra(EXTRA_HABIT_ID))
            else -> startAlarm(intent)
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(intent: Intent?) {
        val habitId = intent?.getStringExtra(EXTRA_HABIT_ID) ?: run { stopSelfSafely(); return }
        val name = intent.getStringExtra(EXTRA_HABIT_NAME).orEmpty()
        val icon = intent.getStringExtra(EXTRA_HABIT_ICON).orEmpty()
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 0)
        val canSnooze = intent.getBooleanExtra(EXTRA_CAN_SNOOZE, false)
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI)

        HaveItNotifications.ensureChannel(this)
        val notification = buildNotification(habitId, name, icon, snoozeMinutes, canSnooze)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startSound(soundUri)
        startVibration()
        registerScreenOff()
        autoStop.removeCallbacks(autoStopRunnable)
        autoStop.postDelayed(autoStopRunnable, RING_TIMEOUT_MS)
    }

    /** 완료: silence immediately, then record the check-in and clear any pending snooze. */
    private fun markDone(habitId: String?) {
        stopSoundAndVibration()
        val app = applicationContext as? HaveItApplication
        if (habitId == null || app == null) {
            stopSelfSafely()
            return
        }
        scope.launch {
            try {
                val epochDay = LocalDate.now().toEpochDay()
                val existing = app.container.checkInRepository.getForHabitOnDay(habitId, epochDay)
                app.container.checkInRepository.upsert(
                    CheckInEntity(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        habitId = habitId,
                        epochDay = epochDay,
                        completed = true,
                    ),
                )
                ReminderScheduler(this@AlarmService).cancelSnooze(habitId)
                HabitWidget.refresh(this@AlarmService)
            } finally {
                stopSelfSafely()
            }
        }
    }

    /** 미루기/닫기, screen off, or safety cap: just silence. A pending snooze alarm (if any) re-rings later. */
    private fun stopAlarm() {
        stopSoundAndVibration()
        stopSelfSafely()
    }

    /**
     * Uses the picked [soundUri], or the device's own default alarm sound if the user never picked
     * one — the sound is whatever's configured on this system, not something bundled with the app.
     */
    private fun startSound(soundUri: String?) {
        try {
            val uri = soundUri?.let(Uri::parse)
                ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                isLooping = true
                setWakeMode(this@AlarmService, PowerManager.PARTIAL_WAKE_LOCK)
                setOnPreparedListener { start() }
                prepareAsync()
            }
        } catch (e: Exception) {
            // No alarm sound available/playable — vibration and the notification still fire.
            mediaPlayer = null
        }
    }

    private fun startVibration() {
        val v = systemVibrator() ?: return
        // repeat index 0 → the on/off pattern loops until cancel(), i.e. a continuous alarm buzz.
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 600, 500), 0)
        v.vibrate(effect)
        vibrator = v
    }

    /** Ring until the screen goes off: pressing the power button silences the alarm like a real one. */
    private fun registerScreenOff() {
        if (screenOffReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) stopAlarm()
            }
        }
        // ACTION_SCREEN_OFF is a protected system broadcast, so it can only be registered at runtime.
        registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        screenOffReceiver = receiver
    }

    private fun unregisterScreenOff() {
        screenOffReceiver?.let { runCatching { unregisterReceiver(it) } }
        screenOffReceiver = null
    }

    private fun stopSoundAndVibration() {
        autoStop.removeCallbacks(autoStopRunnable)
        unregisterScreenOff()
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
            } catch (_: IllegalStateException) {
                // Player not in a stoppable state; releasing below is enough.
            }
            player.release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun stopSelfSafely() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSoundAndVibration()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(
        habitId: String,
        name: String,
        icon: String,
        snoozeMinutes: Int,
        canSnooze: Boolean,
    ): android.app.Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_HABIT_NAME, name)
            putExtra(EXTRA_HABIT_ICON, icon)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            putExtra(EXTRA_CAN_SNOOZE, canSnooze)
        }
        val fullScreenPending = PendingIntent.getActivity(
            this,
            REQUEST_FULL_SCREEN,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val donePending = commandPendingIntent(ACTION_DONE, REQUEST_DONE, habitId)
        val stopPending = commandPendingIntent(ACTION_STOP, REQUEST_STOP, habitId)
        val secondaryLabel = if (canSnooze) "${snoozeMinutes}분 뒤 다시" else "닫기"

        return NotificationCompat.Builder(this, HaveItNotifications.REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(listOf(icon, name).filter { it.isNotBlank() }.joinToString(" "))
            .setContentText("지금 해볼까요?")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)
            .addAction(android.R.drawable.checkbox_on_background, "완료", donePending)
            .addAction(0, secondaryLabel, stopPending)
            .build()
    }

    private fun commandPendingIntent(action: String, requestCode: Int, habitId: String): PendingIntent {
        val intent = Intent(this, AlarmService::class.java).apply {
            this.action = action
            putExtra(EXTRA_HABIT_ID, habitId)
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun systemVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    companion object {
        const val ACTION_START = "com.haveit.app.ALARM_START"
        const val ACTION_STOP = "com.haveit.app.ALARM_STOP"
        const val ACTION_DONE = "com.haveit.app.ALARM_DONE"

        const val EXTRA_HABIT_ID = "alarm_habit_id"
        const val EXTRA_HABIT_NAME = "alarm_habit_name"
        const val EXTRA_HABIT_ICON = "alarm_habit_icon"
        const val EXTRA_SNOOZE_MINUTES = "alarm_snooze_minutes"
        const val EXTRA_CAN_SNOOZE = "alarm_can_snooze"
        const val EXTRA_SOUND_URI = "alarm_sound_uri"

        private const val NOTIFICATION_ID = 424242
        private const val REQUEST_FULL_SCREEN = 1
        private const val REQUEST_DONE = 2
        private const val REQUEST_STOP = 3

        /**
         * Safety cap only. The alarm normally rings until the user acts or the screen is turned off;
         * if it's left completely unattended with the screen still on, it goes quiet after this long so
         * it can't ring forever and drain the battery. A pending snooze re-rings later.
         */
        private const val RING_TIMEOUT_MS = 10 * 60_000L

        fun start(context: Context, habit: HabitEntity, soundUri: String?) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_HABIT_ID, habit.id)
                putExtra(EXTRA_HABIT_NAME, habit.name)
                putExtra(EXTRA_HABIT_ICON, habit.icon)
                putExtra(EXTRA_SNOOZE_MINUTES, habit.reminderSnoozeMinutes)
                putExtra(EXTRA_CAN_SNOOZE, habit.reminderSnoozeMaxCount > 0)
                putExtra(EXTRA_SOUND_URI, soundUri)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException (a subclass, API 31+): this alarm was
                // scheduled inexact (no exact-alarm permission) and fired while the app had no
                // foreground-service-start exemption. Nothing to recover here.
            }
        }
    }
}
