package com.eliya.callguard.data

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar
import java.util.UUID

/**
 * Single source of truth for rules and the activity log.
 *
 * State is kept in memory so the screening service can decide instantly (Android gives it
 * only 5 seconds to respond), and is persisted to a small JSON file on every change.
 */
class GuardRepository(context: Context, private val scope: CoroutineScope) {

    private val file = AtomicFile(File(context.filesDir, "guard.json"))
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val writeLock = Mutex()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<GuardState> = _state.asStateFlow()

    /** The active rule for [number], or null if the call should go through untouched. */
    fun findRule(number: String): Rule? {
        val key = PhoneNumbers.key(number) ?: return null
        val current = _state.value
        if (current.paused) return null
        val now = Calendar.getInstance()
        return current.rules.firstOrNull { it.enabled && it.matches(key) && it.isActiveAt(now) }
    }

    fun upsertRule(rule: Rule) = mutate { s ->
        val exists = s.rules.any { it.id == rule.id }
        s.copy(rules = if (exists) s.rules.map { if (it.id == rule.id) rule else it } else s.rules + rule)
    }

    fun deleteRule(id: String) = mutate { s -> s.copy(rules = s.rules.filterNot { it.id == id }) }

    fun setRuleEnabled(id: String, enabled: Boolean) = mutate { s ->
        s.copy(rules = s.rules.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun setPaused(paused: Boolean) = mutate { it.copy(paused = paused) }

    fun recordEvent(rule: Rule, number: String) = mutate { s ->
        val event = CallEvent(
            id = UUID.randomUUID().toString(),
            ruleId = rule.id,
            name = rule.name,
            number = number,
            mode = rule.mode,
            rings = rule.rings,
            time = System.currentTimeMillis(),
        )
        s.copy(events = (listOf(event) + s.events).take(MAX_EVENTS))
    }

    fun clearHistory() = mutate { it.copy(events = emptyList()) }

    private fun mutate(transform: (GuardState) -> GuardState) {
        _state.update(transform)
        scope.launch(Dispatchers.IO) { persist() }
    }

    private suspend fun persist() = writeLock.withLock {
        val snapshot = _state.value
        val out = try {
            file.startWrite()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open state file", e)
            return@withLock
        }
        try {
            out.write(json.encodeToString(GuardState.serializer(), snapshot).toByteArray())
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            Log.e(TAG, "Cannot save state", e)
        }
    }

    private fun load(): GuardState = try {
        if (!file.baseFile.exists()) {
            GuardState()
        } else {
            json.decodeFromString(GuardState.serializer(), file.readFully().decodeToString())
        }
    } catch (e: Exception) {
        Log.e(TAG, "Cannot read state, starting fresh", e)
        GuardState()
    }

    private companion object {
        const val TAG = "GuardRepository"
        const val MAX_EVENTS = 300
    }
}
