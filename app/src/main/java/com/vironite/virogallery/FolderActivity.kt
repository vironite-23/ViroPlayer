package com.vironite.virogallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vironite.virogallery.data.FolderStore
import com.vironite.virogallery.data.VideoItem
import com.vironite.virogallery.player.PlayerActivity
import com.vironite.virogallery.ui.AppTheme
import com.vironite.virogallery.ui.VideoThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FolderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra(EXTRA_FOLDER_PATH) ?: "ROOT"
        val title = intent.getStringExtra(EXTRA_FOLDER_TITLE) ?: "Videos"
        setContent { AppTheme { FolderScreen(path, title) } }
    }
    companion object {
        const val EXTRA_FOLDER_PATH = "folder_path"
        const val EXTRA_FOLDER_TITLE = "folder_title"
    }
}

private enum class ViewMode { GRID, LIST }
private enum class SortMode { NAME, NEWEST, OLDEST, DURATION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderScreen(folderPath: String, folderTitle: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as ViroGalleryApp
    val store = remember { FolderStore(context) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var view by remember { mutableStateOf(ViewMode.GRID) }
    var sort by remember { mutableStateOf(SortMode.NEWEST) }
    var sortOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(folderPath) {
        loading = true
        val all = withContext(Dispatchers.IO) { app.videoRepository.scanAll(store.getTreeUris()) }
        val showHidden = context.getSharedPreferences("viro_settings", Context.MODE_PRIVATE).getBoolean("show_hidden", true)
        videos = all.filter { belongsToFolder(it, folderPath) && (showHidden || !it.isHidden) }
        loading = false
    }

    val shown = remember(videos, sort, query) {
        videos.filter { query.isBlank() || it.displayName.contains(query, true) }.let { list ->
            when (sort) {
                SortMode.NAME -> list.sortedBy { it.displayName.lowercase(Locale.getDefault()) }
                SortMode.NEWEST -> list.sortedByDescending { it.dateModifiedMs }
                SortMode.OLDEST -> list.sortedBy { it.dateModifiedMs }
                SortMode.DURATION -> list.sortedByDescending { it.durationMs }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (searchOpen) {
                        OutlinedTextField(query, { query = it }, placeholder = { Text("Cari video") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    } else Text(folderTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = { IconButton({ if (selected.isNotEmpty()) selected = emptySet() else (context as? ComponentActivity)?.finish() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton({ searchOpen = !searchOpen; if (!searchOpen) query = "" }) { Icon(if (searchOpen) Icons.Default.Close else Icons.Default.Search, "Search") }
                    IconButton({ view = if (view == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID }) { Icon(if (view == ViewMode.GRID) Icons.Default.GridView else Icons.Default.ViewList, "View mode") }
                    IconButton({ sortOpen = true }) { Icon(Icons.Default.Sort, "Sort") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (selected.isNotEmpty()) {
                Text("${selected.size} dipilih", Modifier.padding(horizontal = 18.dp, vertical = 8.dp), fontWeight = FontWeight.Medium)
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                shown.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Tidak ada video di folder ini", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                view == ViewMode.GRID -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
                ) { items(shown, key = { it.uri.toString() }) { VideoCard(it, context, selected.contains(it.uri.toString()), { key -> selected = selected.toggle(key) }) } }
                else -> Column(Modifier.fillMaxSize()) { shown.forEach { VideoListItem(it, context, selected.contains(it.uri.toString()), { key -> selected = selected.toggle(key) }) } }
            }
        }
    }

    if (sortOpen) AlertDialog(
        onDismissRequest = { sortOpen = false }, title = { Text("Urutkan") },
        text = { Column { listOf(SortMode.NEWEST to "Terbaru", SortMode.NAME to "Nama", SortMode.OLDEST to "Terlama", SortMode.DURATION to "Durasi").forEach { (mode, label) -> Row(Modifier.fillMaxWidth().clickable { sort = mode; sortOpen = false }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(sort == mode, { sort = mode; sortOpen = false }); Text(label, Modifier.padding(start = 8.dp)) } } } },
        confirmButton = { TextButton({ sortOpen = false }) { Text("Tutup") } }
    )
}

@Composable
private fun VideoCard(video: VideoItem, context: Context, selected: Boolean, onToggle: (String) -> Unit) {
    val selectedColor = Color(context.getSharedPreferences("viro_settings", Context.MODE_PRIVATE).getInt("selected_color", 0x334D90FF.toInt()))
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surfaceVariant).combinedClickable(
            onClick = { if (selected) onToggle(video.uri.toString()) else openPlayer(context, video) },
            onLongClick = { onToggle(video.uri.toString()) }
        )) {
            VideoThumbnail(video.uri, Modifier.fillMaxSize(), ContentScale.Crop)
            Text(formatDate(video.dateModifiedMs), Modifier.align(Alignment.BottomStart).padding(5.dp).background(Color.Black.copy(.62f), RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 3.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
            if (selected) Box(Modifier.fillMaxSize().background(selectedColor))
            IconButton({ }) { Icon(Icons.Default.MoreVert, "Video menu", tint = Color.White) }
        }
        Text(video.displayName, Modifier.padding(top = 5.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        Text(formatDuration(video.durationMs), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun VideoListItem(video: VideoItem, context: Context, selected: Boolean, onToggle: (String) -> Unit) {
    val selectedColor = Color(context.getSharedPreferences("viro_settings", Context.MODE_PRIVATE).getInt("selected_color", 0x334D90FF.toInt()))
    Row(Modifier.fillMaxWidth().clickable { if (selected) onToggle(video.uri.toString()) else openPlayer(context, video) }.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(126.dp, 72.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) { VideoThumbnail(video.uri, Modifier.fillMaxSize(), ContentScale.Crop); if (selected) Box(Modifier.fillMaxSize().background(selectedColor)) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(video.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium); Text(formatDuration(video.durationMs), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); Text(formatDate(video.dateModifiedMs), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall) }
        IconButton({ }) { Icon(Icons.Default.MoreVert, "Video menu") }
    }
}

private fun Set<String>.toggle(key: String): Set<String> = toMutableSet().also { if (!it.add(key)) it.remove(key) }.toSet()
private fun openPlayer(context: Context, video: VideoItem) { context.startActivity(Intent(context, PlayerActivity::class.java).apply { data = video.uri; putExtra(PlayerActivity.EXTRA_TITLE, video.displayName) }) }
private fun belongsToFolder(video: VideoItem, folderPath: String): Boolean { val path = video.relativePath.orEmpty().trimEnd('/'); val parent = path.substringBeforeLast('/', ""); return if (folderPath == "ROOT") parent.isBlank() else parent == folderPath }
private fun formatDate(ms: Long): String = if (ms <= 0L) "—" else SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(ms))
private fun formatDuration(ms: Long): String { if (ms <= 0L) return "0:00"; val total = ms / 1000; val s = total % 60; val m = (total / 60) % 60; val h = total / 3600; return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s) }
