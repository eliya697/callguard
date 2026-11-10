package com.eliya.callguard.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import com.eliya.callguard.data.PhoneNumbers
import com.eliya.callguard.data.RuleMode
import com.eliya.callguard.guardApp

/**
 * Android binds to this service for every incoming call (once the app holds the
 * call screening role) and waits up to 5 seconds for our decision before ringing.
 */
class ScreeningService : CallScreeningService() {

    override fun onScreenCall(details: Call.Details) {
        val allow = CallResponse.Builder().build()

        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(details, allow)
            return
        }

        val repository = guardApp.repository
        val number = details.handle?.schemeSpecificPart
        val rule = number?.let(repository::findRule)
        if (number == null || rule == null) {
            respondToCall(details, allow)
            return
        }

        val response = when (rule.mode) {
            RuleMode.BLOCK -> CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                // We post our own, clearer notification instead of the system one.
                .setSkipNotification(true)
                .build()

            RuleMode.IGNORE -> CallResponse.Builder()
                .setSilenceCall(true)
                .build()

            RuleMode.HANG_UP -> CallResponse.Builder()
                .setSilenceCall(rule.silent)
                .build()
        }
        respondToCall(details, response)

        when (rule.mode) {
            RuleMode.BLOCK -> Notifications.showBlocked(this, rule.name, number)
            RuleMode.HANG_UP -> HangUpService.schedule(
                context = this,
                delayMillis = rule.rings * PhoneNumbers.SECONDS_PER_RING * 1000L,
                callerName = rule.name,
            )
            RuleMode.IGNORE -> Unit
        }
        repository.recordEvent(rule, number)
    }
}
