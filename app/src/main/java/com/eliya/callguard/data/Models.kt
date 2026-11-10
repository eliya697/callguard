package com.eliya.callguard.data

import kotlinx.serialization.Serializable
import java.util.Calendar

/** What to do when a filtered contact calls. */
@Serializable
enum class RuleMode {
    /** Reject immediately. Nothing rings, the call is logged as blocked. */
    BLOCK,

    /** Let the call continue silently: the caller hears ringing, the phone stays quiet. */
    IGNORE,

    /** Let it ring (optionally silently) and end it automatically after N rings. */
    HANG_UP,
}

@Serializable
data class Rule(
    val id: String,
    val name: String,
    val numbers: List<String>,
    val mode: RuleMode = RuleMode.IGNORE,
    /** Number of rings before hanging up, used only by [RuleMode.HANG_UP]. */
    val rings: Int = 3,
    /** Silence the phone while waiting to hang up, used only by [RuleMode.HANG_UP]. */
    val silent: Boolean = true,
    val enabled: Boolean = true,
    val photoUri: String? = null,
    /** When set, the rule applies only inside this time window. Null means always. */
    val schedule: Schedule? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun matches(key: String): Boolean = numbers.any { PhoneNumbers.key(it) == key }

    fun isActiveAt(time: Calendar): Boolean = schedule?.isActiveAt(time) ?: true
}

/**
 * A daily time window, e.g. 16:00 → 00:00. A window whose end is earlier than its start
 * runs past midnight (22:00 → 07:00). Equal start and end means the whole day.
 */
@Serializable
data class Schedule(
    val startMinute: Int = 16 * 60,
    val endMinute: Int = 0,
    /** Days the window *starts* on, as [Calendar.DAY_OF_WEEK] values (1 = Sunday). */
    val days: Set<Int> = ALL_DAYS,
) {
    fun isActiveAt(time: Calendar): Boolean {
        val now = time.get(Calendar.HOUR_OF_DAY) * 60 + time.get(Calendar.MINUTE)
        val today = time.get(Calendar.DAY_OF_WEEK)
        val yesterday = if (today == Calendar.SUNDAY) Calendar.SATURDAY else today - 1
        return when {
            startMinute == endMinute -> today in days
            startMinute < endMinute -> today in days && now >= startMinute && now < endMinute
            // Overnight window: the evening part belongs to today, the early-morning part to yesterday.
            now >= startMinute -> today in days
            now < endMinute -> yesterday in days
            else -> false
        }
    }

    companion object {
        val ALL_DAYS: Set<Int> = (Calendar.SUNDAY..Calendar.SATURDAY).toSet()
    }
}

@Serializable
data class CallEvent(
    val id: String,
    val ruleId: String,
    val name: String,
    val number: String,
    val mode: RuleMode,
    val rings: Int,
    val time: Long,
)

@Serializable
data class GuardState(
    val rules: List<Rule> = emptyList(),
    val events: List<CallEvent> = emptyList(),
    /** Global pause switch: when true, no call is filtered. */
    val paused: Boolean = false,
)
