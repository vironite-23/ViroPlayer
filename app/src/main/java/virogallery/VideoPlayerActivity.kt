package virogallery

import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.Locale

class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var root: FrameLayout
    private lateinit var controls: LinearLayout
    private lateinit var seek: SeekBar
    private lateinit var timeCurrent: TextView
    private lateinit var timeTotal: TextView
    private lateinit var title: TextView
    private var items = arrayListOf<VideoItem>()
    private var index = 0
    private var locked = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        items = intent.getSerializableExtra("items") as? ArrayList<VideoItem> ?: arrayListOf()
        index = intent.getIntExtra("index", 0).coerceIn(0, (items.size - 1).coerceAtLeast(0))
        buildUi()
        initPlayer()
    }

    private fun buildUi() {
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        playerView = PlayerView(this).apply {
            useController = false
            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        root.addView(playerView, FrameLayout.LayoutParams(-1, -1))

        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(8), dp(10), dp(4)) }
        val back = button(android.R.drawable.ic_media_previous) { finish() }
        val queue = button(android.R.drawable.ic_menu_sort_by_size) { showQueue() }
        title = TextView(this).apply { setTextColor(Color.WHITE); textSize = 18f; gravity = Gravity.CENTER; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.MIDDLE }
        top.addView(back, lp(48, 56)); top.addView(queue, lp(48,56)); top.addView(title, LinearLayout.LayoutParams(0,dp(56),1f))
        val cast = button(android.R.drawable.ic_menu_share) { Toast.makeText(this,"Cast",Toast.LENGTH_SHORT).show() }
        val camera = button(android.R.drawable.ic_menu_camera) { Toast.makeText(this,"Screenshot",Toast.LENGTH_SHORT).show() }
        val mute = button(android.R.drawable.ic_lock_silent_mode) { player.volume = if (player.volume > 0f) 0f else 1f }
        val lock = button(android.R.drawable.ic_lock_lock) { locked = !locked; controls.visibility = if (locked) View.GONE else View.VISIBLE }
        top.addView(cast, lp(48,56)); top.addView(camera,lp(48,56)); top.addView(mute,lp(48,56)); top.addView(lock,lp(48,56))
        root.addView(top, FrameLayout.LayoutParams(-1, dp(68), Gravity.TOP))

        controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(28),dp(6),dp(28),dp(10)) }
        val actionRow = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val repeat = button(android.R.drawable.ic_popup_sync) { player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE }
        val audio = button(android.R.drawable.ic_btn_speak_now) { showAudioMenu() }
        val cc = textButton("CC") { showSubtitle() }
        val fit = button(android.R.drawable.ic_menu_crop) { playerView.resizeMode = if (playerView.resizeMode == 3) 0 else 3 }
        val speed = button(android.R.drawable.ic_menu_manage) { showSpeedMenu() }
        val overflow = button(android.R.drawable.ic_menu_more) { showOverlayMenu() }
        actionRow.addView(repeat, lp(56,52)); actionRow.addView(audio,lp(56,52)); actionRow.addView(cc,lp(56,52)); actionRow.addView(fit,lp(56,52)); actionRow.addView(speed,lp(56,52)); actionRow.addView(overflow,lp(56,52))
        controls.addView(actionRow, LinearLayout.LayoutParams(-1, dp(60)))
        seek = SeekBar(this).apply { max = 1000; setPadding(0,0,0,0); setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, from: Boolean) { if(from && player.duration>0) player.seekTo(player.duration*p/1000) }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }) }
        controls.addView(seek, LinearLayout.LayoutParams(-1, dp(26)))
        val times = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        timeCurrent = timeLabel("00:00"); timeTotal = timeLabel("00:00")
        val sp = Space(this).apply { layoutParams = LinearLayout.LayoutParams(0,1,1f) }
        times.addView(timeCurrent); times.addView(sp); times.addView(timeTotal)
        controls.addView(times, LinearLayout.LayoutParams(-1,dp(30)))
        val transport = LinearLayout(this).apply { gravity = Gravity.CENTER; setBackgroundColor(Color.argb(150,40,40,40)) }
        transport.addView(button(android.R.drawable.ic_lock_lock) { locked = !locked },lp(70,64))
        transport.addView(button(android.R.drawable.ic_media_previous) { player.seekToPreviousMediaItem() },lp(72,64))
        transport.addView(button(android.R.drawable.ic_media_play) { if(player.isPlaying) player.pause() else player.play() },lp(72,64))
        transport.addView(button(android.R.drawable.ic_media_next) { player.seekToNextMediaItem() },lp(72,64))
        transport.addView(button(android.R.drawable.ic_menu_view) { toggleFullscreen() },lp(70,64))
        controls.addView(transport, LinearLayout.LayoutParams(-1,dp(64)))
        val bottomParams = FrameLayout.LayoutParams(-1, dp(220), Gravity.BOTTOM)
        root.addView(controls,bottomParams)
        setContentView(root)
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        items.forEach { player.addMediaItem(MediaItem.fromUri(it.uri)) }
        player.seekTo(index, 0L); player.prepare(); player.play()
        updateTitle(); player.addListener(object: Player.Listener { override fun onMediaItemTransition(item: MediaItem?, reason: Int) { index=player.currentMediaItemIndex; updateTitle() } })
        handler.post(updateRunnable)
    }

    private val updateRunnable = object: Runnable { override fun run() { if(::player.isInitialized) { val d=player.duration; val p=player.currentPosition; if(d>0) seek.progress=(p*1000/d).toInt(); timeCurrent.text=format(p); timeTotal.text=format(d) }; handler.postDelayed(this,500) } }

    private fun updateTitle() { title.text = items.getOrNull(index)?.name ?: "ViroGallery" }

    private fun showOverlayMenu() {
        val dialog = android.app.Dialog(this)
        val panel = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),dp(14),dp(12),dp(8)); setBackgroundColor(Color.argb(245,25,26,30)) }
        val rows = listOf("Putar di Latar Belakang","Pengatur Nada","Mode Malam","Pengatur Waktu","Ulangi AB","Cermin","Dekoder","Ulang","Acak","Properti","Bagikan","Hapus","Umpan Balik")
        val icons = listOf(android.R.drawable.ic_menu_save,android.R.drawable.ic_menu_preferences,android.R.drawable.ic_menu_day,android.R.drawable.ic_lock_idle_alarm,android.R.drawable.ic_menu_revert,android.R.drawable.ic_menu_rotate,android.R.drawable.ic_menu_info_details,android.R.drawable.ic_popup_sync,android.R.drawable.ic_menu_set_as,android.R.drawable.ic_menu_info_details,android.R.drawable.ic_menu_share,android.R.drawable.ic_menu_delete,android.R.drawable.ic_menu_help)
        var row: LinearLayout?=null
        rows.forEachIndexed { i,s -> if(i%4==0){row=LinearLayout(this); row!!.gravity=Gravity.CENTER; panel.addView(row,LinearLayout.LayoutParams(-1,dp(82)))}; val b=textIconButton(icons[i],s); b.setOnClickListener { when(s){"Bagikan"->shareCurrent();"Hapus"->deleteCurrent();"Ulang"->player.repeatMode=if(player.repeatMode==Player.REPEAT_MODE_ONE)Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE;"Acak"->player.shuffleModeEnabled=!player.shuffleModeEnabled}; if(s!="Properti"&&s!="Umpan Balik") dialog.dismiss() }; row!!.addView(b,LinearLayout.LayoutParams(0,-1,1f)) }
        dialog.setContentView(panel); dialog.window?.setBackgroundDrawableResource(android.R.color.transparent); dialog.window?.setLayout(dp(560),WindowManager.LayoutParams.WRAP_CONTENT); dialog.show(); dialog.window?.setLayout(if(resources.configuration.orientation==2)dp(540) else dp(360),WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun showSubtitle() { showBottom("Subtitle", listOf("Nonaktifkan","Impor file lokal","Unduh","Kustomisasi","Sinkronisasi   −     0,0s     +")) }
    private fun showQueue() { showBottom("Antrian pemutaran", items.map { it.name + "\n" + format(it.durationMs) }) }
    private fun showAudioMenu() { showBottom("Audio", listOf("Track 1","Track 2","Pengaturan audio")) }
    private fun showSpeedMenu() { showBottom("Kecepatan", listOf("0,5x","1,0x","1,25x","1,5x","2,0x"), true) }

    private fun showBottom(titleText:String, entries:List<String>, speed:Boolean=false){ val d=android.app.Dialog(this); val p=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(28),dp(18),dp(28),dp(20));setBackgroundColor(Color.rgb(32,33,36))}; val t=TextView(this).apply{text=titleText;textSize=28f;setTextColor(Color.LTGRAY);setPadding(0,0,0,dp(18))};p.addView(t); entries.forEachIndexed{idx,e->val b=TextView(this).apply{text=e;textSize=17f;setTextColor(Color.WHITE);gravity=Gravity.CENTER_VERTICAL;setPadding(0,dp(14),0,dp(14));setOnClickListener{if(speed){val v=e.substringBefore('x').replace(',','.').toFloat();player.setPlaybackSpeed(v)};d.dismiss()}};p.addView(b)};d.setContentView(p);d.window?.setBackgroundDrawableResource(android.R.color.transparent);d.show();d.window?.setLayout(if(resources.configuration.orientation==2)dp(700) else -1,WindowManager.LayoutParams.WRAP_CONTENT)}

    private fun shareCurrent(){items.getOrNull(index)?.let{startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="video/*";putExtra(Intent.EXTRA_STREAM,it.uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Share video"))}}
    private fun deleteCurrent(){ items.getOrNull(index)?.let{try{contentResolver.delete(it.uri,null,null);finish()}catch(_:Exception){Toast.makeText(this,"Delete permission required",Toast.LENGTH_SHORT).show()}} }
    private fun toggleFullscreen(){ requestedOrientation=if(resources.configuration.orientation==1) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    private fun button(icon:Int, click:()->Unit)=ImageButton(this).apply{setImageResource(icon);setColorFilter(Color.WHITE);background=null;setOnClickListener{click()}}
    private fun textButton(text:String, click:()->Unit)=TextView(this).apply{this.text=text;textSize=16f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setOnClickListener{click()}}
    private fun textIconButton(icon:Int,text:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;addView(ImageView(this@VideoPlayerActivity).apply{setImageResource(icon);setColorFilter(Color.WHITE)},LinearLayout.LayoutParams(-1,dp(40)));addView(TextView(this@VideoPlayerActivity).apply{this.text=text;textSize=10f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);maxLines=1})}
    private fun timeLabel(s:String)=TextView(this).apply{text=s;textSize=14f;setTextColor(Color.LTGRAY)}
    private fun lp(w:Int,h:Int)=LinearLayout.LayoutParams(dp(w),dp(h))
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun format(ms:Long):String{if(ms<0)return"00:00";val s=ms/1000;return if(s>=3600)String.format(Locale.US,"%d:%02d:%02d",s/3600,(s%3600)/60,s%60) else String.format(Locale.US,"%02d:%02d",s/60,s%60)}
    override fun onDestroy(){handler.removeCallbacks(updateRunnable);if(::player.isInitialized)player.release();super.onDestroy()}
}
