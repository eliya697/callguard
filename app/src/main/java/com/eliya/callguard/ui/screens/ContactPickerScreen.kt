package com.eliya.callguard.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eliya.callguard.data.DeviceContact
import com.eliya.callguard.data.PhoneNumbers
import com.eliya.callguard.data.Rule
import com.eliya.callguard.ui.components.ContactAvatar
import com.eliya.callguard.ui.components.EmptyState
import com.eliya.callguard.ui.components.ModePill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    contacts: List<DeviceContact>?,
    existingRules: List<Rule>,
    hasContactsPermission: Boolean,
    onRequestContacts: () -> Unit,
    onBack: () -> Unit,
    onPick: (DeviceContact) -> Unit,
    onManualNumber: (name: String, number: String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var manualOpen by rememberSaveable { mutableStateOf(false) }

    val filtered = remember(contacts, query) {
        val q = query.trim()
        val qDigits = q.filter { it.isDigit() }
        contacts.orEmpty().filter { c ->
            q.isEmpty() ||
                c.name.contains(q, ignoreCase = true) ||
                (qDigits.length >= 3 && c.numbers.any { it.filter(Char::isDigit).contains(qDigits) })
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("בחירת איש קשר") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "חזרה")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("חיפוש לפי שם או מספר") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, contentDescription = "ניקוי") }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                onClick = { manualOpen = true },
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Dialpad, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("הזנת מספר ידנית", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "למספר שלא שמור באנשי הקשר",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            when {
                !hasContactsPermission -> EmptyState(
                    icon = Icons.Outlined.Contacts,
                    title = "אין גישה לאנשי הקשר",
                    body = "כדי לבחור מתוך אנשי הקשר ולזהות אותם כשהם מתקשרים, צריך לאשר גישה.",
                    action = { FilledTonalButton(onClick = onRequestContacts) { Text("אישור גישה") } },
                )

                contacts == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                filtered.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Search,
                    title = if (query.isBlank()) "אין אנשי קשר עם מספר טלפון" else "לא נמצאו תוצאות",
                    body = if (query.isBlank()) "אפשר להוסיף מספר ידנית." else "נסה שם אחר או חלק מהמספר.",
                )

                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(filtered, key = { it.id }) { contact ->
                        val keys = remember(contact) { contact.numbers.mapNotNull(PhoneNumbers::key) }
                        val existing = existingRules.firstOrNull { r -> keys.any(r::matches) }
                        ContactRow(contact, existing, onClick = { onPick(contact) })
                    }
                }
            }
        }
    }

    if (manualOpen) {
        ManualNumberDialog(
            onDismiss = { manualOpen = false },
            onConfirm = { name, number ->
                manualOpen = false
                onManualNumber(name, number)
            },
        )
    }
}

@Composable
private fun ContactRow(contact: DeviceContact, existing: Rule?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(contact.name, contact.photoUri, size = 42.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(contact.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                contact.numbers.joinToString(" · ") { PhoneNumbers.pretty(it) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (existing != null) {
            Spacer(Modifier.width(8.dp))
            ModePill(existing.mode, existing.rings)
        }
    }
}

@Composable
private fun ManualNumberDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    val valid = number.count { it.isDigit() } >= 5

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הוספת מספר") },
        text = {
            Column {
                OutlinedTextField(
                    value = number,
                    onValueChange = { v -> number = v.filter { it.isDigit() || it in "+-() " } },
                    label = { Text("מספר טלפון") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("שם (רשות)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(name.trim(), number.trim()) }) { Text("המשך") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    )
}
