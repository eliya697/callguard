package com.eliya.callguard.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eliya.callguard.data.GuardState
import com.eliya.callguard.data.PhoneNumbers
import com.eliya.callguard.data.Rule
import com.eliya.callguard.screening.SetupStatus
import com.eliya.callguard.ui.components.ContactAvatar
import com.eliya.callguard.ui.components.EmptyState
import com.eliya.callguard.ui.components.ModePill
import com.eliya.callguard.ui.components.SectionLabel
import com.eliya.callguard.ui.components.scheduleSummary
import androidx.compose.material.icons.outlined.Schedule

class SetupActions(
    val requestRole: () -> Unit,
    val requestContacts: () -> Unit,
    val requestCallControl: () -> Unit,
    val requestNotifications: () -> Unit,
)

@Composable
fun RulesScreen(
    state: GuardState,
    setup: SetupStatus,
    setupActions: SetupActions,
    contentPadding: PaddingValues,
    onTogglePaused: () -> Unit,
    onEdit: (Rule) -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onAdd: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 4.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
    ) {
        if (!setup.isComplete) {
            item(key = "setup") { SetupCard(setup, setupActions) }
        }
        if (setup.isReady) {
            item(key = "status") {
                StatusCard(
                    paused = state.paused,
                    activeRules = state.rules.count { it.enabled },
                    blockedToday = state.events.count { isToday(it.time) },
                    onToggle = onTogglePaused,
                )
            }
        }

        if (state.rules.isEmpty()) {
            if (setup.isReady) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Outlined.Shield,
                        title = "אין עדיין אנשי קשר מסוננים",
                        body = "בחר איש קשר והחלט מה יקרה כשהוא מתקשר: חסימה, התעלמות שקטה או ניתוק אחרי כמה צלצולים.",
                        action = {
                            FilledTonalButton(onClick = onAdd) {
                                Icon(Icons.Outlined.PersonAddAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("בחירת איש קשר")
                            }
                        },
                    )
                }
            }
        } else {
            item(key = "label") { SectionLabel("אנשי קשר מסוננים · ${state.rules.size}", Modifier.padding(top = 12.dp)) }
            item(key = "list") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 0.dp,
                ) {
                    Column {
                        state.rules.forEachIndexed { index, rule ->
                            RuleRow(
                                rule = rule,
                                paused = state.paused,
                                onClick = { onEdit(rule) },
                                onToggle = { onToggleRule(rule.id, it) },
                            )
                            if (index != state.rules.lastIndex) {
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
private fun StatusCard(paused: Boolean, activeRules: Int, blockedToday: Int, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(if (paused) scheme.surfaceContainerHigh else scheme.primary, label = "status")
    val content = if (paused) scheme.onSurface else scheme.onPrimary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
        contentColor = content,
        onClick = onToggle,
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(content.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (paused) Icons.Outlined.GppMaybe else Icons.Outlined.GppGood,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (paused) "הסינון מושהה" else "הסינון פעיל", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (paused) "כל השיחות יגיעו אליך כרגיל" else "השיחות מסוננות לפי הכללים שלך",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(0.8f),
                    )
                }
                Switch(
                    checked = !paused,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = scheme.primary,
                        checkedTrackColor = scheme.onPrimary,
                        checkedBorderColor = scheme.onPrimary,
                    ),
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Stat(value = activeRules.toString(), label = "כללים פעילים")
                Stat(value = blockedToday.toString(), label = "שיחות סוננו היום")
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.alpha(0.75f))
    }
}

@Composable
private fun RuleRow(rule: Rule, paused: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val dim = !rule.enabled || paused
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(rule.name, rule.photoUri, modifier = Modifier.alpha(if (dim) 0.55f else 1f))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                rule.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dim) 0.6f else 1f),
            )
            Text(
                rule.numbers.joinToString(" · ") { PhoneNumbers.pretty(it) },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            ModePill(rule.mode, rule.rings, dimmed = dim)
            rule.schedule?.let { schedule ->
                val activeNow = schedule.isActiveAt(java.util.Calendar.getInstance())
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        scheduleSummary(schedule) + if (activeNow && !dim) " · פעיל עכשיו" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (activeNow && !dim) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = rule.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun SetupCard(setup: SetupStatus, actions: SetupActions) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(Modifier.padding(vertical = 20.dp)) {
            Text(
                if (setup.isReady) "כמעט סיימנו" else "הגדרה ראשונית",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                "כדי לסנן שיחות, Android צריך לאשר לאפליקציה כמה הרשאות.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            SetupStep(
                icon = Icons.Outlined.Shield,
                title = "אפליקציית סינון שיחות",
                body = "מאפשר לאפליקציה להחליט על כל שיחה נכנסת",
                done = setup.screeningRole,
                onClick = actions.requestRole,
            )
            SetupStep(
                icon = Icons.Outlined.Contacts,
                title = "גישה לאנשי הקשר",
                body = "לבחירת אנשי קשר ולזיהוי שלהם כשהם מתקשרים",
                done = setup.contacts,
                onClick = actions.requestContacts,
            )
            SetupStep(
                icon = Icons.Outlined.PhoneInTalk,
                title = "ניהול שיחות",
                body = "נדרש רק לניתוק אוטומטי אחרי מספר צלצולים",
                done = setup.callControl,
                onClick = actions.requestCallControl,
                optional = true,
            )
            SetupStep(
                icon = Icons.Outlined.Notifications,
                title = "התראות",
                body = "כדי שתדע על שיחות שנחסמו",
                done = setup.notifications,
                onClick = actions.requestNotifications,
                optional = true,
            )
        }
    }
}

@Composable
private fun SetupStep(
    icon: ImageVector,
    title: String,
    body: String,
    done: Boolean,
    onClick: () -> Unit,
    optional: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (done) Icons.Outlined.CheckCircle else icon,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (optional && !done) "$title (רשות)" else title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!done) {
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                Text("אישור")
            }
        }
    }
}

internal fun isToday(time: Long): Boolean {
    val cal = java.util.Calendar.getInstance()
    val today = cal.get(java.util.Calendar.DAY_OF_YEAR) to cal.get(java.util.Calendar.YEAR)
    cal.timeInMillis = time
    return (cal.get(java.util.Calendar.DAY_OF_YEAR) to cal.get(java.util.Calendar.YEAR)) == today
}
