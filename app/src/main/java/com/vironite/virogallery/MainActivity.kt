package com.vironite.virogallery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.core.content.ContextCompat
import com.vironite.virogallery.data.FolderStore
import com.vironite.virogallery.data.VideoItem
import com.vironite.virogallery.ui.AppTheme
import com.vironite.virogallery.ui.VideoThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { MainScreen() } }
    }
}

private data class FolderSummary(
    val name: String,
    val path: String,
    val videoCount: Int,
    val newestVideoDate: Long,
    val sample: VideoItem?
)

private enum class FolderView { GRID, LIST }
private enum class SortMode { RECENT, NAME, COUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = context.applicationContext as ViroGalleryApp
    val store = remember { FolderStore(context) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var refresh by remember { mutableIntStateOf(0) }
    var view by remember { mutableStateOf(FolderView.GRID) }
    var sort by remember { mutableStateOf(SortMode.RECENT) }
    var query by remember { mutableStateOf("") }
    var sortOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val navBarColor = Color(context.getSharedPreferences("viro_settings", Context.MODE_PRIVATE).getInt("nav_color", 0xFF000000.toInt()))

    val hiddenAllowed = remember { mutableStateOf(context.getSharedPreferences("viro_settings", Context.MODE_PRIVATE).getBoolean("show_hidden", true)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            store.addTree(uri)
            refresh++
        }
    }

    LaunchedEffect(refresh) {
        videos = withContext(Dispatchers.IO) { app.videoRepository.scanAll(store.getTreeUris()) }
    }

    val visibleVideos = videos.filter { hiddenAllowed.value || !it.isHidden }
    val folders = remember(visibleVideos, query, sort) {
        buildFolderSummaries(visibleVideos)
            .filter { query.isBlank() || it.name.contains(query, true) }
            .let { list ->
                when (sort) {
                    SortMode.RECENT -> list.sortedByDescending { it.newestVideoDate }
                    SortMode.NAME -> list.sortedBy { it.name.lowercase(Locale.getDefault()) }
                    SortMode.COUNT -> list.sortedByDescending { it.videoCount }
                }
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Videos", fontWeight = FontWeight.Medium) },
                actions = {
                    IconButton(onClick = { query = if (query.isBlank()) " " else "" }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                    IconButton(onClick = { sortOpen = true }) { Icon(Icons.Default.Sort, "Sort") }
                    IconButton(onClick = { view = if (view == FolderView.GRID) FolderView.LIST else FolderView.GRID }) {
                        Icon(if (view == FolderView.GRID) Icons.Default.GridView else Icons.Default.ViewList, "View mode")
                    }
                    IconButton(onClick = { moreOpen = true }) { Icon(Icons.Default.MoreVert, "More") }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = navBarColor) {
                NavigationBarItem(true, { }, icon = { Icon(Icons.Default.VideoLibrary, null) }, label = { Text("Video") })
                NavigationBarItem(false, { context.startActivity(Intent(context, SettingsActivity::class.java)) }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Setelan") })
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (query == " ") {
                OutlinedTextField(
                    value = if (query == " ") "" else query,
                    onValueChange = { query = it.ifBlank { " " } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Cari folder") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, null) } }
                )
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = { treeLauncher.launch(null) }, label = { Text("Tambah Folder") }, leadingIcon = { Icon(Icons.Default.FolderOpen, null) })
                AssistChip(onClick = { permissionLauncher.launch(mediaPermission()) }, label = { Text("MediaStore") }, leadingIcon = { Icon(Icons.Default.VideoLibrary, null) })
                AssistChip(onClick = { hiddenAllowed.value = !hiddenAllowed.value }, label = { Text(if (hiddenAllowed.value) "File tersembunyi" else "Tanpa tersembunyi") }, leadingIcon = { Icon(Icons.Default.Visibility, null) })
            }
            if (folders.isEmpty()) {
                EmptyState { treeLauncher.launch(null) }
            } else if (view == FolderView.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(folders, key = { it.path }) { FolderCard(it, visibleVideos, context) }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    folders.forEach { FolderListItem(it, visibleVideos, context) }
                }
            }
        }
    }

    if (sortOpen) AlertDialog(
        onDismissRequest = { sortOpen = false },
        title = { Text("Urutkan") },
        text = { Column {
            listOf(SortMode.RECENT to "Terbaru", SortMode.NAME to "Nama", SortMode.COUNT to "Jumlah video").forEach { (mode, label) ->
                Row(Modifier.fillMaxWidth().clickable { sort = mode; sortOpen = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(sort == mode, { sort = mode; sortOpen = false }); Text(label, Modifier.padding(start = 8.dp))
                }
            }
        } }, confirmButton = { TextButton({ sortOpen = false }) { Text("Tutup") } }
    )

    if (moreOpen) AlertDialog(
        onDismissRequest = { moreOpen = false },
        title = { Text("Video") },
        text = { Text("ViroGallery menampilkan video dari MediaStore dan folder yang Anda berikan aksesnya, termasuk folder tersembunyi.") },
        confirmButton = { TextButton({ moreOpen = false }) { Text("OK") } }
    )
}

@Composable
private fun FolderCard(folder: FolderSummary, videos: List<VideoItem>, context: Context) {
    Column(Modifier.fillMaxWidth().clickable {
        context.startActivity(Intent(context, FolderActivity::class.java).apply {
            putExtra(FolderActivity.EXTRA_FOLDER_PATH, folder.path)
            putExtra(FolderActivity.EXTRA_FOLDER_TITLE, folder.name)
        })
    }) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.18f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            folder.sample?.let { VideoThumbnail(it.uri, Modifier.fillMaxSize(), ContentScale.Crop) }
                ?: Icon(Icons.Default.Folder, null, Modifier.align(Alignment.Center).size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatDate(folder.newestVideoDate),
                Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.Black.copy(.62f), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
            IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, "Folder menu", tint = Color.White) }
        }
        Text(folder.name, Modifier.padding(top = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
        Text("${folder.videoCount} video", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FolderListItem(folder: FolderSummary, videos: List<VideoItem>, context: Context) {
    Row(Modifier.fillMaxWidth().clickable {
        context.startActivity(Intent(context, FolderActivity::class.java).apply {
            putExtra(FolderActivity.EXTRA_FOLDER_PATH, folder.path); putExtra(FolderActivity.EXTRA_FOLDER_TITLE, folder.name)
        })
    }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(96.dp, 72.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            folder.sample?.let { VideoThumbnail(it.uri, Modifier.fillMaxSize(), ContentScale.Crop) }
                ?: Icon(Icons.Default.Folder, null, Modifier.align(Alignment.Center))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text("${folder.videoCount} video • ${formatDate(folder.newestVideoDate)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, "Folder menu") }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.VideoLibrary, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("Belum ada video", style = MaterialTheme.typography.titleLarge)
            Text("Tambahkan folder atau berikan izin MediaStore.", Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onAdd, Modifier.padding(top = 16.dp)) { Text("Tambah Folder") }
        }
    }
}

private fun buildFolderSummaries(videos: List<VideoItem>): List<FolderSummary> {
    val groups = LinkedHashMap<String, MutableList<VideoItem>>()
    videos.forEach { video ->
        val path = video.relativePath.orEmpty().trimEnd('/')
        val parent = path.substringBeforeLast('/', "")
        val key = if (parent.isBlank()) "ROOT" else parent
        groups.getOrPut(key) { mutableListOf() }.add(video)
    }
    return groups.map { (path, items) -> FolderSummary(if (path == "ROOT") "Videos" else path.substringAfterLast('/'), path, items.size, items.maxOfOrNull { it.dateModifiedMs } ?: 0L, items.maxByOrNull { it.dateModifiedMs }) }
}

private fun mediaPermission(): String = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE
private fun formatDate(ms: Long): String = if (ms <= 0L) "—" else SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(ms))
