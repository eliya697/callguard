package com.eliya.callguard.ui

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eliya.callguard.data.DeviceContact
import com.eliya.callguard.data.PhoneNumbers
import com.eliya.callguard.data.Rule
import com.eliya.callguard.ui.screens.ContactPickerScreen
import com.eliya.callguard.ui.screens.HistoryScreen
import com.eliya.callguard.ui.screens.RuleEditorSheet
import com.eliya.callguard.ui.screens.RulesScreen
import com.eliya.callguard.ui.screens.SetupActions
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: MainViewModel, openHistorySignal: Int) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val setup by viewModel.setup.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Rule?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(openHistorySignal) {
        if (openHistorySignal > 0) {
            tab = 1
            picking = false
        }
    }

    val setupActions = rememberSetupActions(onChanged = viewModel::refreshSetup)

    BackHandler(enabled = picking) { picking = false }

    Crossfade(targetState = picking, label = "screen") { showPicker ->
        if (showPicker) {
            ContactPickerScreen(
                contacts = contacts,
                existingRules = state.rules,
                hasContactsPermission = setup.contacts,
                onRequestContacts = setupActions.requestContacts,
                onBack = { picking = false },
                onPick = { contact -> editing = draftFor(contact, state.rules) },
                onManualNumber = { name, number ->
                    editing = draftFor(
                        DeviceContact(id = -1, name = name.ifBlank { PhoneNumbers.pretty(number) }, numbers = listOf(number), photoUri = null),
                        state.rules,
                    )
                },
            )
        } else {
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    LargeTopAppBar(
                        title = { Text(if (tab == 0) "שומר שיחות" else "יומן פעילות") },
                        actions = {
                            if (tab == 1 && state.events.isNotEmpty()) {
                                IconButton(onClick = { confirmClear = true }) {
                                    Icon(Icons.Outlined.DeleteSweep, contentDescription = "ניקוי היומן")
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Outlined.Shield, contentDescription = null) },
                            label = { Text("סינון") },
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                            label = { Text("יומן") },
                        )
                    }
                },
                floatingActionButton = {
                    if (tab == 0 && setup.isReady) {
                        ExtendedFloatingActionButton(
                            onClick = { picking = true },
                            icon = { Icon(Icons.Outlined.PersonAddAlt, contentDescription = null) },
                            text = { Text("הוספת איש קשר") },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
            ) { padding ->
                Crossfade(targetState = tab, label = "tab") { current ->
                    when (current) {
                        0 -> RulesScreen(
                            state = state,
                            setup = setup,
                            setupActions = setupActions,
                            contentPadding = padding,
                            onTogglePaused = { viewModel.setPaused(!state.paused) },
                            onEdit = { editing = it },
                            onToggleRule = viewModel::setRuleEnabled,
                            onAdd = { picking = true },
                        )
                        else -> HistoryScreen(events = state.events, rules = state.rules, contentPadding = padding)
                    }
                }
            }
        }
    }

    editing?.let { draft ->
        RuleEditorSheet(
            draft = draft,
            isNew = state.rules.none { it.id == draft.id },
            canHangUp = setup.callControl,
            onRequestCallControl = setupActions.requestCallControl,
            onDismiss = { editing = null },
            onSave = {
                viewModel.saveRule(it)
                editing = null
                picking = false
            },
            onDelete = {
                viewModel.deleteRule(it.id)
                editing = null
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("לנקות את היומן?") },
            text = { Text("כל רשומות הפעילות יימחקו. הכללים שהגדרת לא ישתנו.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    confirmClear = false
                }) { Text("ניקוי") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("ביטול") } },
        )
    }
}

/** A new rule for [contact], or the existing rule if one already covers one of its numbers. */
private fun draftFor(contact: DeviceContact, rules: List<Rule>): Rule {
    val keys = contact.numbers.mapNotNull(PhoneNumbers::key).toSet()
    return rules.firstOrNull { rule -> keys.any(rule::matches) }
        ?: Rule(
            id = UUID.randomUUID().toString(),
            name = contact.name,
            numbers = contact.numbers,
            photoUri = contact.photoUri,
        )
}

@Composable
private fun rememberSetupActions(onChanged: () -> Unit): SetupActions {
    val context = LocalContext.current
    var lastRequested by remember { mutableStateOf(emptyArray<String>()) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onChanged()
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onChanged()
        // If Android no longer shows the dialog ("don't ask again"), send the user to settings.
        val activity = context.findActivity()
        val blocked = result.any { (permission, granted) ->
            !granted && activity != null && !activity.shouldShowRequestPermissionRationale(permission)
        }
        if (blocked && lastRequested.isNotEmpty()) openAppSettings(context)
    }

    fun request(vararg permissions: String) {
        lastRequested = arrayOf(*permissions)
        permissionLauncher.launch(arrayOf(*permissions))
    }

    return remember {
        SetupActions(
            requestRole = {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
                }
            },
            requestContacts = { request(Manifest.permission.READ_CONTACTS) },
            requestCallControl = {
                request(Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.READ_PHONE_STATE)
            },
            requestNotifications = {
                if (Build.VERSION.SDK_INT >= 33) request(Manifest.permission.POST_NOTIFICATIONS)
            },
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
