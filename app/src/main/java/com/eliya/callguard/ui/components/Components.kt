package com.eliya.callguard.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CallEnd
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eliya.callguard.data.RuleMode
import com.eliya.callguard.data.Schedule
import com.eliya.callguard.ui.theme.LocalModeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ---------- Mode presentation ----------

val RuleMode.title: String
    get() = when (this) {
        RuleMode.BLOCK -> "חסימה"
        RuleMode.IGNORE -> "התעלמות"
        RuleMode.HANG_UP -> "ניתוק אוטומטי"
    }

val RuleMode.description: String
    get() = when (this) {
        RuleMode.BLOCK -> "השיחה נדחית מיד. הטלפון לא מצלצל, והשיחה נרשמת כחסומה."
        RuleMode.IGNORE -> "אצל המתקשר ממשיך לצלצל, אצלך הטלפון שקט. תראה שיחה שלא נענתה."
        RuleMode.HANG_UP -> "השיחה מצלצלת ומתנתקת אוטומטית אחרי מספר הצלצולים שתבחר."
    }

val RuleMode.icon: ImageVector
    get() = when (this) {
        RuleMode.BLOCK -> Icons.Outlined.Block
        RuleMode.IGNORE -> Icons.Outlined.NotificationsOff
        RuleMode.HANG_UP -> Icons.Outlined.CallEnd
    }

fun ringsLabel(rings: Int): String = if (rings == 1) "צלצול אחד" else "$rings צלצולים"

fun formatMinute(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

private val dayLetters = mapOf(1 to "א׳", 2 to "ב׳", 3 to "ג׳", 4 to "ד׳", 5 to "ה׳", 6 to "ו׳", 7 to "ש׳")

fun dayLetter(day: Int): String = dayLetters.getValue(day)

/** e.g. "16:00–00:00" or "22:00–07:00 · א׳ ב׳ ג׳". */
fun scheduleSummary(schedule: Schedule): String {
    val hours = if (schedule.startMinute == schedule.endMinute) {
        "כל היום"
    } else {
        "${formatMinute(schedule.startMinute)}–${formatMinute(schedule.endMinute)}"
    }
    val days = when (schedule.days) {
        Schedule.ALL_DAYS -> ""
        setOf(1, 2, 3, 4, 5) -> " · א׳–ה׳"
        setOf(6, 7) -> " · סופ״ש"
        else -> " · " + schedule.days.sorted().joinToString(" ") { dayLetter(it) }
    }
    return hours + days
}

fun modeSummary(mode: RuleMode, rings: Int): String =
    if (mode == RuleMode.HANG_UP) "ניתוק אחרי ${ringsLabel(rings)}" else mode.title

@Composable
fun modeContainerColors(mode: RuleMode): Pair<Color, Color> {
    val c = LocalModeColors.current
    return when (mode) {
        RuleMode.BLOCK -> c.blockContainer to c.onBlockContainer
        RuleMode.IGNORE -> c.ignoreContainer to c.onIgnoreContainer
        RuleMode.HANG_UP -> c.hangUpContainer to c.onHangUpContainer
    }
}

@Composable
fun ModePill(mode: RuleMode, rings: Int, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val (bg, fg) = modeContainerColors(mode)
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (dimmed) MaterialTheme.colorScheme.surfaceContainerHigh else bg,
        contentColor = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else fg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(mode.icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(modeSummary(mode, rings), style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ---------- Avatar ----------

private val avatarTints = listOf(
    0xFF1E3A5F, 0xFF8A6D3B, 0xFF4F6B5A, 0xFF6B4F6B, 0xFF5F4A3A, 0xFF3A5A6B,
)

@Composable
fun ContactAvatar(name: String, photoUri: String?, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var photo by remember(photoUri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photoUri) {
        if (photoUri == null) return@LaunchedEffect
        photo = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(photoUri))?.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    val tint = Color(avatarTints[(name.hashCode() and Int.MAX_VALUE) % avatarTints.size])
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = photo
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(
                text = initials(name),
                color = if (MaterialTheme.colorScheme.background.luminanceIsDark()) tint.lighten() else tint,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.36f).sp,
            )
        }
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val letters = parts.take(2).mapNotNull { p -> p.firstOrNull { it.isLetterOrDigit() } }
    return letters.joinToString("").uppercase().ifEmpty { "#" }
}

private fun Color.luminanceIsDark(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f
private fun Color.lighten(): Color = Color(
    red = red + (1f - red) * 0.55f,
    green = green + (1f - green) * 0.55f,
    blue = blue + (1f - blue) * 0.55f,
)

// ---------- Layout helpers ----------

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}
