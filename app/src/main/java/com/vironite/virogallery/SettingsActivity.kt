package com.vironite.virogallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vironite.virogallery.ui.AppTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { SettingsScreen() } }
    }
}

private class SettingsStore(context: Context) {
    private val p = context.getSharedPreferences("viro_settings", Context.MODE_PRIVATE)
    fun bool(key: String, default: Boolean) = p.getBoolean(key, default)
    fun setBool(key: String, value: Boolean) = p.edit().putBoolean(key, value).apply()
    fun string(key: String, default: String) = p.getString(key, default) ?: default
    fun setString(key: String, value: String) = p.edit().putString(key, value).apply()
    fun int(key: String, default: Int) = p.getInt(key, default)
    fun setInt(key: String, value: Int) = p.edit().putInt(key, value).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { SettingsStore(context) }
    var showHidden by remember { mutableStateOf(store.bool("show_hidden", true)) }
    var showNoMedia by remember { mutableStateOf(store.bool("show_nomedia", true)) }
    var resume by remember { mutableStateOf(store.string("resume", "Tanya setiap kali")) }
    var autoNext by remember { mutableStateOf(store.bool("auto_next", true)) }
    var autoPip by remember { mutableStateOf(store.bool("auto_pip", false)) }
    var rememberBrightness by remember { mutableStateOf(store.bool("brightness", true)) }
    var rememberAspect by remember { mutableStateOf(store.bool("aspect", false)) }
    var rememberSpeed by remember { mutableStateOf(store.bool("speed", true)) }
    var subtitles by remember { mutableStateOf(store.bool("subtitles", true)) }
    var gesture by remember { mutableStateOf(store.bool("gesture", true)) }
    var navColor by remember { mutableStateOf(store.int("nav_color", 0xFF000000.toInt())) }
    var accentColor by remember { mutableStateOf(store.int("accent_color", 0xFF4D90FF.toInt())) }
    var bgColor by remember { mutableStateOf(store.int("bg_color", 0xFF101114.toInt())) }
    var cardColor by remember { mutableStateOf(store.int("card_color", 0xFF1F1F1F.toInt())) }
    var selectedColor by remember { mutableStateOf(store.int("selected_color", 0x334D90FF.toInt())) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val navBarColor = Color(context.getSharedPreferences("viro_settings", Context.MODE_PRIVATE).getInt("nav_color", 0xFF000000.toInt()))

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("Setelan") }, navigationIcon = { IconButton({ context.startActivity(Intent(context, MainActivity::class.java)); (context as? ComponentActivity)?.finish() }) { Icon(Icons.Default.ArrowBack, "Back") } }) },
        bottomBar = { NavigationBar(containerColor = navBarColor) {
            NavigationBarItem(false, { context.startActivity(Intent(context, MainActivity::class.java)); (context as? ComponentActivity)?.finish() }, icon = { Icon(Icons.Default.VideoLibrary, null) }, label = { Text("Video") })
            NavigationBarItem(true, { }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Setelan") })
        } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Header("Pustaka") }
            item { ToggleRow(Icons.Default.Visibility, "Tampilkan file tersembunyi", "Baca video di folder tersembunyi dan file yang diawali titik (.)", showHidden) { showHidden = it; store.setBool("show_hidden", it) } }
            item { ToggleRow(Icons.Default.Folder, "Tampilkan folder .nomedia", "Jangan mengecualikan folder yang memiliki .nomedia", showNoMedia) { showNoMedia = it; store.setBool("show_nomedia", it) } }

            item { Header("Tampilan utama") }
            item { SettingRow(Icons.Default.GridView, "Kolom grid video", "3 kolom", {}) }
            item { SettingRow(Icons.Default.ViewList, "Mode tampilan folder", "Grid / daftar", {}) }
            item { SettingRow(Icons.Default.Sort, "Urutan default", "Terbaru", {}) }
            item { SettingRow(Icons.Default.FolderOpen, "Tambah folder", "Pilih folder dengan Android Directory Picker", {}) }

            item { Header("Pemutaran") }
            item { SettingRow(Icons.Default.PlayArrow, "Lanjutkan pemutaran", resume) { dialog = "resume" } }
            item { ToggleRow(Icons.Default.SkipNext, "Putar otomatis berikutnya", "Putar video berikutnya setelah video selesai", autoNext) { autoNext = it; store.setBool("auto_next", it) } }
            item { ToggleRow(Icons.Default.PictureInPictureAlt, "Auto PiP", "Masuk Picture-in-Picture saat berpindah aplikasi", autoPip) { autoPip = it; store.setBool("auto_pip", it) } }
            item { ToggleRow(Icons.Default.Brightness6, "Ingat kecerahan", "Simpan pengaturan kecerahan player", rememberBrightness) { rememberBrightness = it; store.setBool("brightness", it) } }
            item { ToggleRow(Icons.Default.AspectRatio, "Ingat rasio aspek", "Simpan mode aspect ratio player", rememberAspect) { rememberAspect = it; store.setBool("aspect", it) } }
            item { ToggleRow(Icons.Default.Speed, "Ingat kecepatan", "Simpan kecepatan pemutaran", rememberSpeed) { rememberSpeed = it; store.setBool("speed", it) } }
            item { ToggleRow(Icons.Default.Subtitles, "Tampilkan subtitle", "Aktifkan subtitle secara default", subtitles) { subtitles = it; store.setBool("subtitles", it) } }
            item { ToggleRow(Icons.Default.TouchApp, "Kontrol gerakan", "Gerakan untuk volume, kecerahan, zoom dan pencarian", gesture) { gesture = it; store.setBool("gesture", it) } }
            item { SettingRow(Icons.Default.SwapHoriz, "Maju / mundur", "10 detik", {}) }
            item { SettingRow(Icons.Default.ScreenRotation, "Orientasi default", "Ikuti sensor", {}) }

            item { Header("UI Colors") }
            item { ColorRow("Accent color", accentColor) { accentColor = it; store.setInt("accent_color", it) } }
            item { ColorRow("Background color", bgColor) { bgColor = it; store.setInt("bg_color", it) } }
            item { ColorRow("Card / panel color", cardColor) { cardColor = it; store.setInt("card_color", it) } }
            item { ColorRow("Selected / highlight color", selectedColor) { selectedColor = it; store.setInt("selected_color", it) } }
            item { ColorRow("Navigation bar color", navColor) { navColor = it; store.setInt("nav_color", it) } }

            item { Header("Tentang") }
            item { SettingRow(Icons.Default.Info, "ViroGallery", "Open-source video gallery and player", {}) }
            item { SettingRow(Icons.Default.BugReport, "Kirim umpan balik", "Laporkan bug atau sarankan fitur", {}) }
        }
    }

    if (dialog == "resume") ChoiceDialog("Lanjutkan pemutaran", listOf("Tanya setiap kali", "Selalu lanjutkan pemutaran", "Selalu mulai dari awal"), resume, { resume = it; store.setString("resume", it); dialog = null }) { dialog = null }
}

@Composable private fun Header(text: String) { Text(text, Modifier.padding(start = 20.dp, top = 20.dp, bottom = 7.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }

@Composable private fun ToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(25.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(horizontal = 17.dp)) { Text(title); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        Switch(value, onChange)
    }
}

@Composable private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(25.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(horizontal = 17.dp)) { Text(title); if (subtitle.isNotEmpty()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable private fun ColorRow(title: String, colorInt: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { open = true }.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).padding(3.dp), contentAlignment = Alignment.Center) { Surface(Modifier.size(24.dp), shape = MaterialTheme.shapes.small, color = Color(colorInt)) {} }
        Text(title, Modifier.weight(1f).padding(horizontal = 17.dp))
        Text("#%08X".format(colorInt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) ColorChoiceDialog(title, colorInt, { onPick(it); open = false }) { open = false }
}

@Composable private fun ColorChoiceDialog(title: String, current: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = listOf(0xFF000000, 0xFF101114, 0xFF1F1F1F, 0xFF4D90FF, 0xFF5E35B1, 0xFF00897B, 0xFFEF5350, 0xFFFFFFFF, 0xFFE0E0E0, 0xFF333333, 0x334D90FF, 0x66FFFFFF)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column { colors.chunked(4).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { value -> Surface(Modifier.size(52.dp).padding(6.dp).clickable { onPick(value.toInt()) }, shape = MaterialTheme.shapes.medium, color = Color(value.toInt()), tonalElevation = 3.dp) {} } } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } })
}

@Composable private fun ChoiceDialog(title: String, options: List<String>, selected: String, onSelected: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column { options.forEach { option -> Row(Modifier.fillMaxWidth().clickable { onSelected(option) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected == option, { onSelected(option) }); Text(option, Modifier.padding(start = 8.dp)) } } } }, confirmButton = { TextButton(onDismiss) { Text("Tutup") } })
}
