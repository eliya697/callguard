package com.eliya.callguard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliya.callguard.data.ContactsSource
import com.eliya.callguard.data.DeviceContact
import com.eliya.callguard.data.GuardState
import com.eliya.callguard.data.Rule
import com.eliya.callguard.guardApp
import com.eliya.callguard.screening.SetupStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = app.guardApp.repository

    val state: StateFlow<GuardState> = repository.state

    private val _setup = MutableStateFlow(SetupStatus.read(app))
    val setup: StateFlow<SetupStatus> = _setup.asStateFlow()

    private val _contacts = MutableStateFlow<List<DeviceContact>?>(null)
    val contacts: StateFlow<List<DeviceContact>?> = _contacts.asStateFlow()

    fun refreshSetup() {
        val status = SetupStatus.read(getApplication())
        _setup.value = status
        if (status.contacts && _contacts.value == null) loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            _contacts.value = runCatching { ContactsSource.load(getApplication()) }.getOrDefault(emptyList())
        }
    }

    fun saveRule(rule: Rule) = repository.upsertRule(rule)
    fun deleteRule(id: String) = repository.deleteRule(id)
    fun setRuleEnabled(id: String, enabled: Boolean) = repository.setRuleEnabled(id, enabled)
    fun setPaused(paused: Boolean) = repository.setPaused(paused)
    fun clearHistory() = repository.clearHistory()
}
