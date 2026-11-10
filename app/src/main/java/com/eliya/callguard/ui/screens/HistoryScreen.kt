package com.eliya.callguard.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eliya.callguard.data.CallEvent
import com.eliya.callguard.data.PhoneNumbers
import com.eliya.callguard.data.Rule
import com.eliya.callguard.ui.components.ContactAvatar
import com.eliya.callguard.ui.components.EmptyState
import com.eliya.callguard.ui.components.ModePill
import com.eliya.callguard.ui.components.SectionLabel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HistoryScreen(events: List<CallEvent>, rules: List<Rule>, contentPadding: PaddingValues) {
    if (events.isEmpty()) {
        Column(Modifier.padding(contentPadding)) {
            EmptyState(
                icon = Icons.Outlined.History,
                title = "היומן ריק",
                body = "כאן יופיעו שיחות שהאפליקציה חסמה, השתיקה או ניתקה.",
            )
        }
        return
    }

    val photos = remember(rules) { rules.associate { it.id to it.photoUri } }
    val groups = remember(events) { events.groupBy { dayLabel(it.time) } }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.forLanguageTag("he")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        groups.forEach { (day, dayEvents) ->
            item(key = "h_$day") { SectionLabel(day, Modifier.padding(top = 8.dp)) }
            item(key = "g_$day") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Column {
                        dayEvents.forEachIndexed { index, event ->
                            EventRow(event, photos[event.ruleId], timeFormat.format(event.time))
                            if (index != dayEvents.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: CallEvent, photoUri: String?, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(event.name, photoUri)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                PhoneNumbers.pretty(event.number),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(0.dp))
            ModePill(event.mode, event.rings, modifier = Modifier.padding(top = 8.dp))
        }
        Text(time, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun dayLabel(time: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = time }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> "היום"
        sameYear && dayDiff == 1 -> "אתמול"
        sameYear -> SimpleDateFormat("EEEE, d בMMMM", Locale.forLanguageTag("he")).format(time)
        else -> SimpleDateFormat("d בMMMM yyyy", Locale.forLanguageTag("he")).format(time)
    }
}
