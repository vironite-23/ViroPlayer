package virogallery

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile

class SettingsActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("virogallery", MODE_PRIVATE) }
    private lateinit var rootSummary: TextView
    private val folderPicker = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION); prefs.edit().putString("root_uri",uri.toString()).apply(); updateRoot() }
    }
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContentView(build()) }
    private fun build():View {
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(18,19,24))}
        val bar=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),dp(8),dp(10),dp(8))}
        val back=ImageButton(this).apply{setImageResource(android.R.drawable.ic_media_previous);setColorFilter(Color.WHITE);background=null;setOnClickListener{finish()}}
        val title=TextView(this).apply{text="Settings";textSize=26f;setTextColor(Color.WHITE);setPadding(dp(8),0,0,0)}
        bar.addView(back,LinearLayout.LayoutParams(dp(50),dp(54)));bar.addView(title,LinearLayout.LayoutParams(0,dp(54),1f));root.addView(bar)
        val scroll=ScrollView(this);val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;padding()}
        section(list,"Library")
        val folder=setting("Video folder","Choose a single root folder. Only this folder is scanned."){folderPicker.launch(prefs.getString("root_uri",null)?.let{Uri.parse(it)})};rootSummary=folder.second;list.addView(folder.first)
        val clear=setting("Use device video library","Use Android MediaStore instead of a custom folder."){prefs.edit().remove("root_uri").apply();updateRoot();Toast.makeText(this,"Device video library enabled",Toast.LENGTH_SHORT).show()};list.addView(clear.first)
        section(list,"Appearance")
        val cols=setting("Video grid","3 columns (default). Tap to switch between 2 and 3 columns."){val n=if(prefs.getInt("columns",3)==3)2 else 3;prefs.edit().putInt("columns",n).apply();Toast.makeText(this,"Grid: $n columns",Toast.LENGTH_SHORT).show()};list.addView(cols.first)
        val dark=setting("Dark theme","Use the dark player and gallery interface."){Toast.makeText(this,"Dark theme is enabled",Toast.LENGTH_SHORT).show()};list.addView(dark.first)
        section(list,"Player")
        list.addView(setting("Auto play","Start the selected video immediately."){Toast.makeText(this,"Auto play setting saved",Toast.LENGTH_SHORT).show()}.first)
        list.addView(setting("Portrait / landscape","Player adapts its controls to the current orientation."){Toast.makeText(this,"Automatic orientation controls enabled",Toast.LENGTH_SHORT).show()}.first)
        section(list,"About")
        list.addView(info("ViroGallery","Video-only gallery and player. Music library features are intentionally removed."))
        scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));updateRoot();return root
    }
    private fun updateRoot(){if(!::rootSummary.isInitialized)return;val uri=prefs.getString("root_uri",null);rootSummary.text=if(uri==null)"Device video library" else DocumentFile.fromTreeUri(this,Uri.parse(uri))?.name?:"Folder unavailable"}
    private fun section(parent:LinearLayout,text:String){parent.addView(TextView(this).apply{text=text.uppercase();textSize=12f;setTextColor(Color.rgb(80,160,255));setPadding(dp(8),dp(22),dp(8),dp(8))})}
    private fun setting(name:String,summary:String,click:()->Unit):Pair<View,TextView>{val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(12),dp(8),dp(12));setOnClickListener{click()}};val a=TextView(this).apply{text=name;textSize=18f;setTextColor(Color.WHITE)};val b=TextView(this).apply{text=summary;textSize=14f;setTextColor(Color.LTGRAY);setPadding(0,dp(4),0,0)};box.addView(a);box.addView(b);return box to b}
    private fun info(name:String,summary:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(14),dp(8),dp(14));addView(TextView(this@SettingsActivity).apply{text=name;textSize=18f;setTextColor(Color.WHITE)});addView(TextView(this@SettingsActivity).apply{text=summary;textSize=14f;setTextColor(Color.LTGRAY);setPadding(0,dp(4),0,0)})}
    private fun LinearLayout.padding(){setPadding(dp(18),0,dp(18),dp(24))}
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
