package com.novatube.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.novatube.app.data.model.SearchResult
import com.novatube.app.util.FileUtils
import java.util.concurrent.TimeUnit

@Composable
fun MediaCard(
    title: String,
    uploader: String?,
    durationSec: Long?,
    thumbnail: String?,
    platform: String,
    viewCount: Long? = null,
    onClick: () -> Unit,
    onDownload: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                if (!thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
                        )
                    )
                }
                PlatformBadge(
                    platform = platform,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                )
                if (durationSec != null && durationSec > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = formatDuration(durationSec),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uploader ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (viewCount != null) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = formatViews(viewCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onDownload != null || onMore != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onDownload != null) {
                            FilledTonalButton(onClick = onDownload, modifier = Modifier.weight(1f)) {
                                Text("Download")
                            }
                        }
                        if (onMore != null) {
                            OutlinedButton(onClick = onMore) { Text("Open") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformBadge(platform: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = platform,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ResultListItem(
    result: SearchResult,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(96.dp, 64.dp).clip(RoundedCornerShape(10.dp))) {
            if (!result.thumbnail.isNullOrBlank()) {
                AsyncImage(model = result.thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            }
            if (result.duration != null && result.duration > 0) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        formatDuration(result.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    append(result.uploader ?: "Unknown")
                    append(" • ")
                    append(result.platform)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalIconButton(onClick = onDownload) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(android.R.drawable.stat_sys_download),
                contentDescription = "Download"
            )
        }
    }
}

fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    val h = TimeUnit.SECONDS.toHours(safe)
    val m = TimeUnit.SECONDS.toMinutes(safe) - TimeUnit.HOURS.toMinutes(h)
    val s = safe - TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(safe))
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%d:%02d", m, s)
}

fun formatViews(views: Long): String {
    if (views < 1000) return views.toString()
    val units = arrayOf("", "K", "M", "B", "T")
    var value = views.toDouble()
    var i = 0
    while (value >= 1000 && i < units.lastIndex) { value /= 1000; i++ }
    return String.format(if (value >= 10) "%.0f%s" else "%.1f%s", value, units[i])
}
