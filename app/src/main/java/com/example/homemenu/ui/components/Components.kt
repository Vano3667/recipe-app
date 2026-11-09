package com.example.homemenu.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.homemenu.domain.model.Tag
import com.example.homemenu.ui.theme.isAppDark
import com.example.homemenu.ui.theme.tagColors

enum class Illustration { BOOK, CALENDAR, BASKET, SEARCH }

/** Self-drawn (CC0) line illustration, tinted with the theme. */
@Composable
fun EmptyIllustration(kind: Illustration, modifier: Modifier = Modifier) {
    val stroke = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primaryContainer
    val accent = MaterialTheme.colorScheme.secondaryContainer
    Canvas(modifier.size(140.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(accent, radius = w * 0.46f, center = Offset(w / 2, h / 2))
        val line = Stroke(width = w * 0.03f)
        when (kind) {
            Illustration.BOOK -> {
                drawRoundRect(fill, topLeft = Offset(w * 0.22f, h * 0.28f), size = Size(w * 0.56f, h * 0.44f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
                drawRoundRect(stroke, topLeft = Offset(w * 0.22f, h * 0.28f), size = Size(w * 0.56f, h * 0.44f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = line)
                drawLine(stroke, Offset(w * 0.5f, h * 0.28f), Offset(w * 0.5f, h * 0.72f), strokeWidth = w * 0.03f)
                for (i in 0..2) {
                    val y = h * (0.38f + i * 0.1f)
                    drawLine(stroke, Offset(w * 0.28f, y), Offset(w * 0.44f, y), strokeWidth = w * 0.02f)
                    drawLine(stroke, Offset(w * 0.56f, y), Offset(w * 0.72f, y), strokeWidth = w * 0.02f)
                }
            }
            Illustration.CALENDAR -> {
                drawRoundRect(fill, topLeft = Offset(w * 0.24f, h * 0.27f), size = Size(w * 0.52f, h * 0.48f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
                drawRoundRect(stroke, topLeft = Offset(w * 0.24f, h * 0.27f), size = Size(w * 0.52f, h * 0.48f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f), style = line)
                drawLine(stroke, Offset(w * 0.24f, h * 0.38f), Offset(w * 0.76f, h * 0.38f), strokeWidth = w * 0.03f)
                for (r in 0..1) for (c in 0..2) drawCircle(stroke, radius = w * 0.025f, center = Offset(w * (0.35f + c * 0.15f), h * (0.5f + r * 0.13f)))
            }
            Illustration.BASKET -> {
                drawArc(stroke, 180f, 180f, false, topLeft = Offset(w * 0.32f, h * 0.24f), size = Size(w * 0.36f, h * 0.32f), style = line)
                drawRoundRect(fill, topLeft = Offset(w * 0.22f, h * 0.42f), size = Size(w * 0.56f, h * 0.3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f))
                drawRoundRect(stroke, topLeft = Offset(w * 0.22f, h * 0.42f), size = Size(w * 0.56f, h * 0.3f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f), style = line)
            }
            Illustration.SEARCH -> {
                drawCircle(fill, radius = w * 0.16f, center = Offset(w * 0.45f, h * 0.45f))
                drawCircle(stroke, radius = w * 0.16f, center = Offset(w * 0.45f, h * 0.45f), style = line)
                drawLine(stroke, Offset(w * 0.57f, h * 0.57f), Offset(w * 0.7f, h * 0.7f), strokeWidth = w * 0.05f)
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    illustration: Illustration,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp).testTag("empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyIllustration(illustration)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 360.dp))
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun TagChip(tag: Tag, modifier: Modifier = Modifier) {
    val (bg, fg) = tagColors(tag.colorHint, isAppDark())
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            tag.name, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun TagDot(hint: String, modifier: Modifier = Modifier) {
    val (bg, _) = tagColors(hint, isAppDark())
    Box(modifier.size(12.dp).clip(CircleShape).background(bg))
}

/** − N + stepper; never goes below [min]. */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 99,
    label: String? = null,
    testTagPrefix: String = "stepper",
) {
    val pop by animateFloatAsState(if (value % 2 == 0) 1f else 1.0001f, spring(), label = "pop")
    Row(modifier.animateContentSize(), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = { if (value > min) onValueChange(value - 1) }, enabled = value > min,
            modifier = Modifier.size(36.dp).testTag("${testTagPrefix}_minus"),
        ) { Icon(Icons.Filled.Remove, contentDescription = "Меньше") }
        Text(
            text = if (label != null) "$value $label" else "$value",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp).scale(pop).testTag("${testTagPrefix}_value")
                .semantics { contentDescription = "$value" },
        )
        FilledTonalIconButton(
            onClick = { if (value < max) onValueChange(value + 1) }, enabled = value < max,
            modifier = Modifier.size(36.dp).testTag("${testTagPrefix}_plus"),
        ) { Icon(Icons.Filled.Add, contentDescription = "Больше") }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.fillMaxWidth().padding(vertical = 8.dp))
}

fun servingsWord(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "порция"
        mod10 in 2..4 && mod100 !in 12..14 -> "порции"
        else -> "порций"
    }
}
