package com.drama.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.drama.app.data.Series
import com.drama.app.data.Store
import com.drama.app.data.formatDuration

object C {
    val Bg = Color(0xFF0A0A0F)
    val Surface = Color(0xFF16161F)
    val Surface2 = Color(0xFF22222E)
    val Accent = Color(0xFFFF3B6B)
    val Accent2 = Color(0xFFFF8A3D)
    val Gold = Color(0xFFFFC857)
    val Text = Color(0xFFF5F5F8)
    val Muted = Color(0xFFA0A0B2)
    val Faint = Color(0xFF6B6B7E)
    val Green = Color(0xFF36D399)
    val Grad = Brush.horizontalGradient(listOf(Accent, Accent2))
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.Accent, onPrimary = Color.White, background = C.Bg, onBackground = C.Text,
            surface = C.Surface, onSurface = C.Text, surfaceVariant = C.Surface2, onSurfaceVariant = C.Muted,
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 34.sp),
            headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 29.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 25.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
            labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
        ),
        content = content,
    )
}

@Composable
fun KindBadge(s: Series, modifier: Modifier = Modifier) {
    val (text, col) = when (s.kind) {
        "full" -> "Pełna seria" to C.Green
        "episodes" -> "Wszystkie odcinki" to C.Green
        else -> "Niepełna" to C.Gold
    }
    Text(
        text, color = Color.Black, style = MaterialTheme.typography.labelMedium,
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(col).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

fun seriesMeta(s: Series): String {
    val parts = ArrayList<String>()
    parts += s.studio
    formatDuration(s.duration).takeIf { it.isNotBlank() }?.let { parts += it }
    if (s.kind == "episodes") parts += "${s.videos.size} odc."
    else if (s.videos.size > 1) parts += "${s.videos.size} części"
    return parts.joinToString(" · ")
}

/** Plakat w siatce/rzędzie. */
@Composable
fun PosterCard(s: Series, width: Int = 150, onClick: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val prog = Store.seriesProgress(s)
    Column(Modifier.width(width.dp).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.72f).clip(RoundedCornerShape(12.dp)).background(C.Surface2)) {
            AsyncImage(model = s.thumbOrDefault(), contentDescription = s.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0xCC000000))))
            KindBadge(s, Modifier.padding(6.dp).align(Alignment.TopStart))
            Text(
                formatDuration(s.duration), color = Color.White, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp),
            )
            if (prog > 0.01f) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth(prog).fillMaxHeight().background(C.Accent))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(s.studio, style = MaterialTheme.typography.bodyMedium, color = C.Muted, maxLines = 1)
    }
}

/** Wiersz w liście (wyszukiwanie, moja lista). */
@Composable
fun SeriesRow(s: Series, subtitle: String? = null, onClick: () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val v = Store.version
    val prog = Store.seriesProgress(s)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(132.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).background(C.Surface2)) {
            AsyncImage(model = s.thumbOrDefault(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(30.dp))
            if (prog > 0.01f) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0x55FFFFFF))) {
                    Box(Modifier.fillMaxWidth(prog).fillMaxHeight().background(C.Accent))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle ?: seriesMeta(s), style = MaterialTheme.typography.bodyMedium, color = C.Muted, maxLines = 1)
            KindBadge(s)
        }
    }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.clip(RoundedCornerShape(26.dp)).background(C.Grad).clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White)
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier.padding(start = 16.dp, top = 22.dp, bottom = 10.dp))
}
