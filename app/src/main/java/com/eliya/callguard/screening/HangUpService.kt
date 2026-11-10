package com.eliya.callguard.screening

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Short-lived foreground service that waits for the configured number of rings and then
 * ends the call, but only if it is still ringing (never an answered call).
 *
 * A foreground service is used because the app process may be frozen once the screening
 * service is unbound; a plain timer would then never fire.
 */
class HangUpService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var deadline = 0L
    private var ringingSeen = false

    private val tick = object : Runnable {
        override fun run() {
            val ringing = isRinging(this@HangUpService)
            if (ringing) ringingSeen = true

            when {
                // The call was answered, or the caller gave up: nothing left to do.
                ringingSeen && !ringing -> stopSelf()
                SystemClock.elapsedRealtime() >= deadline -> {
                    if (ringing) endRingingCall(this@HangUpService)
                    stopSelf()
                }
                else -> handler.postDelayed(this, TICK_MS)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val delay = intent?.getLongExtra(EXTRA_DELAY, 0L) ?: 0L
        val name = intent?.getStringExtra(EXTRA_NAME).orEmpty()

        ServiceCompat.startForeground(
            this,
            Notifications.HANGUP_NOTIFICATION_ID,
            Notifications.hangUpPending(this, name),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE else 0,
        )

        deadline = SystemClock.elapsedRealtime() + delay
        ringingSeen = false
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, TICK_MS)
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int) {
        // Android 14+ limit for short services (about 3 minutes). Never reached in practice.
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    companion object {
        private const val TAG = "HangUpService"
        private const val EXTRA_DELAY = "delay"
        private const val EXTRA_NAME = "name"
        private const val TICK_MS = 500L

        fun schedule(context: Context, delayMillis: Long, callerName: String) {
            if (!hasCallControl(context)) {
                Log.w(TAG, "Missing phone permissions, cannot hang up automatically")
                return
            }
            val intent = Intent(context, HangUpService::class.java)
                .putExtra(EXTRA_DELAY, delayMillis)
                .putExtra(EXTRA_NAME, callerName)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // Fallback: an in-process timer. Works as long as the process stays awake.
                Log.w(TAG, "Foreground service not allowed, using in-process timer", e)
                val app = context.applicationContext
                Handler(Looper.getMainLooper()).postDelayed(
                    { if (isRinging(app)) endRingingCall(app) },
                    delayMillis,
                )
            }
        }

        fun hasCallControl(context: Context): Boolean =
            granted(context, Manifest.permission.ANSWER_PHONE_CALLS) &&
                granted(context, Manifest.permission.READ_PHONE_STATE)

        private fun granted(context: Context, permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        @SuppressLint("MissingPermission")
        private fun isRinging(context: Context): Boolean {
            if (!hasCallControl(context)) return false
            return try {
                @Suppress("DEPRECATION") // Still the simplest synchronous check of the call state.
                context.getSystemService(TelephonyManager::class.java)?.callState == TelephonyManager.CALL_STATE_RINGING
            } catch (e: SecurityException) {
                false
            }
        }

        @SuppressLint("MissingPermission")
        private fun endRingingCall(context: Context) {
            try {
                // Deprecated for new dialer apps, but still the supported path for
                // apps holding ANSWER_PHONE_CALLS. Rejects the ringing call.
                @Suppress("DEPRECATION")
                context.getSystemService(TelecomManager::class.java)?.endCall()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to end call", e)
            }
        }
    }
}
