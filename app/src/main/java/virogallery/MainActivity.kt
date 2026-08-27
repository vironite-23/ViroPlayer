package virogallery

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var grid: GridLayout
    private lateinit var title: TextView
    private lateinit var empty: TextView
    private var currentFolder: String? = null
    private var currentTreeUri: Uri? = null
    private val folderStack = ArrayDeque<Uri>()
    private val videos = mutableListOf<VideoItem>()
    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("virogallery", MODE_PRIVATE) }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        loadVideos()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
        requestMediaPermissionIfNeeded()
    }

    override fun onResume() { super.onResume(); if (::grid.isInitialized) loadVideos() }

    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(18, 19, 24))
        }
        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(8), dp(4))
        }
        val back = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_media_previous)
            setColorFilter(Color.WHITE)
            background = null
            visibility = View.GONE
            setOnClickListener { navigateBack() }
        }
        title = TextView(this).apply {
            text = "Videos"
            setTextColor(Color.WHITE); textSize = 27f
            setPadding(dp(10), 0, 0, 0)
        }
        val spacer = Space(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) }
        val gridButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_sort_by_size)
            setColorFilter(Color.LTGRAY); background = null
            setOnClickListener { toggleGridMode() }
        }
        val sortButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_sort_alphabetically)
            setColorFilter(Color.LTGRAY); background = null
            setOnClickListener { showSortMenu() }
        }
        val settings = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_preferences)
            setColorFilter(Color.LTGRAY); background = null
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
        }
        bar.addView(back, LinearLayout.LayoutParams(dp(44), dp(52)))
        bar.addView(title, LinearLayout.LayoutParams(0, dp(52), 1f))
        bar.addView(gridButton, LinearLayout.LayoutParams(dp(48), dp(52)))
        bar.addView(sortButton, LinearLayout.LayoutParams(dp(48), dp(52)))
        bar.addView(settings, LinearLayout.LayoutParams(dp(48), dp(52)))
        root.addView(bar)

        val scroll = ScrollView(this)
        grid = GridLayout(this).apply {
            columnCount = 3
            useDefaultMargins = false
            alignmentMode = GridLayout.ALIGN_MARGINS
            setPadding(dp(8), dp(8), dp(8), dp(16))
        }
        empty = TextView(this).apply {
            text = "No videos found\nGrant video access or choose a folder in Settings."
            textSize = 16f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY)
            visibility = View.GONE
        }
        scroll.addView(grid)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(empty, LinearLayout.LayoutParams(-1, 0, 1f))
        return root
    }

    private fun requestMediaPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED)
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
            else loadVideos()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            else loadVideos()
        }
    }

    private fun loadVideos() {
        val tree = prefs.getString("root_uri", null)
        executor.execute {
            val result = if (tree != null) scanTree(currentTreeUri ?: Uri.parse(tree)) else scanMediaStore()
            runOnUiThread { render(result) }
        }
    }

    private fun scanMediaStore(): List<VideoItem> {
        val out = mutableListOf<VideoItem>()
        val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION, MediaStore.Video.Media.DATE_ADDED, MediaStore.Video.Media.DATA)
        val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, sort)?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val name = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val duration = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val date = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val data = c.getColumnIndex(MediaStore.Video.Media.DATA)
            while (c.moveToNext()) {
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, c.getLong(id))
                out.add(VideoItem(uri, c.getString(name), c.getLong(duration), c.getLong(date), if (data >= 0) c.getString(data) else null))
            }
        }
        return out
    }

    private fun scanTree(tree: Uri): List<VideoItem> {
        val out = mutableListOf<VideoItem>()
        val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, tree) ?: return out
        root.listFiles().forEach { f ->
            if (f.isFile && isVideo(f.name ?: "")) out.add(VideoItem(f.uri, f.name ?: "Video", 0, f.lastModified(), f.uri.toString()))
        }
        return out.sortedByDescending { it.dateAdded }
    }

    private fun render(items: List<VideoItem>) {
        videos.clear(); videos.addAll(items)
        grid.removeAllViews()
        val tree = prefs.getString("root_uri", null)
        if (tree != null) renderFoldersAndVideos(Uri.parse(tree), items) else renderVideosOnly(items)
    }

    private fun renderVideosOnly(items: List<VideoItem>) {
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        grid.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        if (items.isNotEmpty()) items.forEachIndexed { index, item -> addVideoCard(item, index) }
    }

    private fun renderFoldersAndVideos(tree: Uri, items: List<VideoItem>) {
        val dir = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, tree)
        val children = dir?.listFiles()?.filter { it.isDirectory } ?: emptyList()
        empty.visibility = if (children.isEmpty() && items.isEmpty()) View.VISIBLE else View.GONE
        grid.visibility = if (children.isEmpty() && items.isEmpty()) View.GONE else View.VISIBLE
        children.sortedBy { it.name?.lowercase() ?: "" }.forEach { addFolderCard(it) }
        items.forEachIndexed { index, item -> addVideoCard(item, index) }
        title.text = dir?.name ?: "Videos"
    }

    private fun addFolderCard(folder: androidx.documentfile.provider.DocumentFile) {
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(5), dp(5), dp(5), dp(10)); setOnClickListener {
            currentTreeUri?.let { folderStack.addLast(it) } ?: prefs.getString("root_uri", null)?.let { folderStack.addLast(Uri.parse(it)) }
            currentTreeUri = folder.uri; currentFolder = folder.name; title.text = folder.name ?: "Folder"; loadVideos()
        }}
        val icon = ImageView(this).apply { setImageResource(android.R.drawable.ic_menu_agenda); setColorFilter(Color.LTGRAY); setBackgroundColor(Color.rgb(35,36,42)); scaleType=ImageView.ScaleType.CENTER }
        card.addView(icon, LinearLayout.LayoutParams(-1, dp(105)))
        card.addView(TextView(this).apply { text=folder.name ?: "Folder"; textSize=14f; setTextColor(Color.WHITE); maxLines=2; ellipsize=android.text.TextUtils.TruncateAt.END; setPadding(dp(2),dp(5),dp(2),0) }, LinearLayout.LayoutParams(-1,dp(64)))
        grid.addView(card, GridLayout.LayoutParams().apply { width=0; height=GridLayout.LayoutParams.WRAP_CONTENT; columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f) })
    }

    private fun addVideoCard(item: VideoItem, index: Int) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(10))
            setOnClickListener { openPlayer(index) }
            setOnLongClickListener { showVideoMenu(item); true }
        }
        val thumb = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(35, 36, 42))
        }
        val h = dp(105)
        card.addView(thumb, LinearLayout.LayoutParams(-1, h))
        val name = TextView(this).apply {
            text = item.name; textSize = 14f; setTextColor(Color.WHITE)
            maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(2), dp(5), dp(2), 0)
        }
        card.addView(name, LinearLayout.LayoutParams(-1, dp(42)))
        val duration = TextView(this).apply {
            text = formatDuration(item.durationMs); textSize = 12f; setTextColor(Color.GRAY)
            setPadding(dp(2), 0, 0, 0)
        }
        card.addView(duration, LinearLayout.LayoutParams(-1, dp(22)))
        grid.addView(card, GridLayout.LayoutParams().apply {
            width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        })
        executor.execute {
            val bmp = loadThumbnail(item.uri)
            runOnUiThread { if (bmp != null && thumb.parent != null) thumb.setImageBitmap(bmp) }
        }
    }

    private fun loadThumbnail(uri: Uri): android.graphics.Bitmap? = try {
        val retriever = android.media.MediaMetadataRetriever()
        retriever.setDataSource(this, uri)
        val b = retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retriever.release(); b
    } catch (_: Exception) { null }

    private fun openPlayer(index: Int) {
        if (index !in videos.indices) return
        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
            putExtra("index", index); putExtra("folder", currentFolder)
            putExtra("items", ArrayList(videos))
        }
        startActivity(intent)
    }

    private fun showVideoMenu(item: VideoItem) {
        PopupMenu(this, grid).apply {
            menu.add("Open"); menu.add("Share"); menu.add("Delete")
            setOnMenuItemClickListener { m ->
                when (m.title.toString()) {
                    "Open" -> { val i = videos.indexOf(item); openPlayer(i) }
                    "Share" -> share(item)
                    "Delete" -> delete(item)
                }; true
            }; show()
        }
    }

    private fun share(item: VideoItem) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "video/*"; putExtra(Intent.EXTRA_STREAM, item.uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share video"))
    }

    private fun delete(item: VideoItem) {
        try { contentResolver.delete(item.uri, null, null); loadVideos() } catch (_: Exception) { Toast.makeText(this, "Delete permission required", Toast.LENGTH_SHORT).show() }
    }

    private fun showSortMenu() {
        PopupMenu(this, findViewById(android.R.id.content)).apply {
            menu.add("Name"); menu.add("Date added"); menu.add("Duration")
            setOnMenuItemClickListener { m ->
                val sorted = when (m.title.toString()) { "Name" -> videos.sortedBy { it.name.lowercase() }; "Duration" -> videos.sortedByDescending { it.durationMs }; else -> videos.sortedByDescending { it.dateAdded } }
                render(sorted); true
            }; show()
        }
    }

    private fun toggleGridMode() {
        val current = prefs.getInt("columns", 3)
        prefs.edit().putInt("columns", if (current == 3) 2 else 3).apply(); render(videos)
    }

    private fun navigateBack() {
        if (currentTreeUri != null) {
            if (folderStack.isNotEmpty()) { currentTreeUri = folderStack.removeLast(); title.text = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, currentTreeUri!!)?.name ?: "Videos"; loadVideos() }
            else showRoot()
        } else finish()
    }

    private fun showRoot() { currentFolder = null; currentTreeUri = null; folderStack.clear(); title.text = "Videos"; loadVideos() }

    private fun isVideo(name: String) = listOf(".mp4", ".mkv", ".webm", ".avi", ".mov", ".m4v", ".3gp").any { name.lowercase().endsWith(it) }
    private fun formatDuration(ms: Long): String { if (ms <= 0) return ""; val s=ms/1000; return "%d:%02d:%02d".format(s/3600,(s%3600)/60,s%60) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
