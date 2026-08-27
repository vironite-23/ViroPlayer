package com.vironite.virogallery.player;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.activity.ComponentActivity;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.vironite.virogallery.R;

import java.util.Locale;

public class PlayerActivity extends ComponentActivity {
    public static final String EXTRA_TITLE = "title";

    private PlayerView playerView;
    private ExoPlayer player;
    private SeekBar progress;
    private TextView positionText, durationText, titleText;
    private ImageButton playButton, muteButton, lockButton;
    private View controlsOverlay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean locked = false;
    private boolean muted = false;
    private boolean repeat = false;

    private final Runnable progressUpdater = new Runnable() {
        @Override public void run() {
            if (player != null) {
                long duration = Math.max(0, player.getDuration());
                long position = Math.max(0, player.getCurrentPosition());
                durationText.setText(formatTime(duration));
                positionText.setText(formatTime(position));
                if (duration > 0) progress.setProgress((int) Math.min(1000, (position * 1000L) / duration));
                playButton.setImageResource(player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            }
            handler.postDelayed(this, 500);
        }
    };

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        setContentView(R.layout.activity_player);
        bindViews();
        setupPlayer(getIntent().getData());
        setupControls();
    }

    private void bindViews() {
        playerView = findViewById(R.id.player_view);
        controlsOverlay = findViewById(R.id.controls_overlay);
        progress = findViewById(R.id.progress_bar);
        positionText = findViewById(R.id.position_text);
        durationText = findViewById(R.id.duration_text);
        titleText = findViewById(R.id.title_text);
        playButton = findViewById(R.id.play_button);
        muteButton = findViewById(R.id.mute_button);
        lockButton = findViewById(R.id.lock_button);
    }

    private void setupPlayer(Uri uri) {
        if (uri == null) { finish(); return; }
        player = new ExoPlayer.Builder(this).build();
        playerView.setUseController(false);
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.setPlayWhenReady(true);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        titleText.setText(title == null ? "" : title);
        handler.post(progressUpdater);
    }

    private void setupControls() {
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.queue_button).setOnClickListener(v -> showQueueSheet());
        findViewById(R.id.player_menu_button).setOnClickListener(v -> showPlayerMenu());
        findViewById(R.id.subtitle_button).setOnClickListener(v -> showSubtitleSheet());
        findViewById(R.id.audio_button).setOnClickListener(v -> showAudioDialog());
        findViewById(R.id.aspect_button).setOnClickListener(v -> showSimpleChoice("Aspect Ratio", new String[]{"Fit", "Fill", "16:9", "4:3", "Original"}));
        findViewById(R.id.speed_button).setOnClickListener(v -> showSpeedDialog());
        findViewById(R.id.repeat_button).setOnClickListener(v -> {
            repeat = !repeat;
            player.setRepeatMode(repeat ? ExoPlayer.REPEAT_MODE_ONE : ExoPlayer.REPEAT_MODE_OFF);
            v.setAlpha(repeat ? 1f : .7f);
        });
        findViewById(R.id.cast_button).setOnClickListener(v -> Toast.makeText(this, "Cast belum dikonfigurasi", Toast.LENGTH_SHORT).show());
        findViewById(R.id.screenshot_button).setOnClickListener(v -> Toast.makeText(this, "Screenshot", Toast.LENGTH_SHORT).show());
        muteButton.setOnClickListener(v -> toggleMute());
        lockButton.setOnClickListener(v -> setLocked(!locked));
        findViewById(R.id.transport_lock).setOnClickListener(v -> setLocked(!locked));
        playButton.setOnClickListener(v -> {
            if (player.isPlaying()) player.pause(); else player.play();
        });
        findViewById(R.id.rewind_button).setOnClickListener(v -> player.seekTo(Math.max(0, player.getCurrentPosition() - 10000)));
        findViewById(R.id.forward_button).setOnClickListener(v -> player.seekTo(Math.min(player.getDuration(), player.getCurrentPosition() + 10000)));
        findViewById(R.id.fullscreen_button).setOnClickListener(v -> toggleOrientation());
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int value, boolean fromUser) {
                if (fromUser && player != null && player.getDuration() > 0) player.seekTo((player.getDuration() * value) / 1000L);
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void toggleMute() {
        muted = !muted;
        player.setVolume(muted ? 0f : 1f);
        muteButton.setImageResource(muted ? R.drawable.ic_volume_off : R.drawable.ic_music_note);
    }

    private void setLocked(boolean value) {
        locked = value;
        controlsOverlay.setVisibility(locked ? View.INVISIBLE : View.VISIBLE);
        if (!locked) return;
        ImageButton unlock = new ImageButton(this);
        unlock.setImageResource(R.drawable.ic_lock);
        unlock.setBackgroundColor(Color.TRANSPARENT);
        unlock.setColorFilter(Color.WHITE);
        addContentView(unlock, new WindowManager.LayoutParams(58, 58, Gravity.TOP | Gravity.END));
        unlock.setOnClickListener(v -> { ((View)v.getParent()); ((android.view.ViewGroup)v.getParent()).removeView(v); setLocked(false); });
    }

    private void toggleOrientation() {
        int current = getResources().getConfiguration().orientation;
        setRequestedOrientation(current == android.content.res.Configuration.ORIENTATION_LANDSCAPE ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextColor(Color.WHITE); t.setTextSize(size); t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(20, 14, 20, 14);
        return t;
    }

    private Button menuButton(String icon, String label) {
        Button b = new Button(this);
        b.setText(icon + "\n" + label);
        b.setTextColor(Color.WHITE); b.setTextSize(13); b.setAllCaps(false);
        b.setGravity(Gravity.CENTER); b.setBackgroundColor(Color.TRANSPARENT);
        GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width = 0; p.height = 92; p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); b.setLayoutParams(p);
        return b;
    }

    private android.app.Dialog baseDialog(boolean bottom) {
        android.app.Dialog d = new android.app.Dialog(this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window w = d.getWindow();
        if (w != null) w.setBackgroundDrawableResource(android.R.color.transparent);
        return d;
    }

    private void showPlayerMenu() {
        android.app.Dialog d = baseDialog(false);
        GridLayout grid = new GridLayout(this); grid.setColumnCount(4); grid.setPadding(12, 12, 12, 12);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(16,17,20)); bg.setCornerRadius(30);
        grid.setBackground(bg);
        String[][] items = {{"🎧","Putar di Latar Belakang"},{"☷","Pengatur Nada"},{"☾","Mode Malam"},{"◷","Pengatur Waktu"},{"↔","Ulangi AB"},{"▣","Cermin"},{"HW","Dekoder"},{"↻","Ulang"},{"⤨","Acak"},{"ⓘ","Properti"},{"⌯","Bagikan"},{"□","Hapus"},{"!","Umpan Balik"}};
        for (String[] item : items) {
            Button b = menuButton(item[0], item[1]); grid.addView(b);
            if (item[1].equals("Hapus")) b.setOnClickListener(v -> { d.dismiss(); Toast.makeText(this, "Hapus: konfirmasi diperlukan", Toast.LENGTH_SHORT).show(); });
            else if (item[1].equals("Bagikan")) b.setOnClickListener(v -> shareVideo());
        }
        d.setContentView(grid);
        Window w = d.getWindow(); if (w != null) { w.setLayout(dp(540), WindowManager.LayoutParams.WRAP_CONTENT); w.setGravity(Gravity.CENTER); }
        d.show();
        w = d.getWindow(); if (w != null) w.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(560)), WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void shareVideo() {
        Uri uri = getIntent().getData(); if (uri == null) return;
        android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_SEND); i.setType("video/*"); i.putExtra(android.content.Intent.EXTRA_STREAM, uri); i.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(android.content.Intent.createChooser(i, "Bagikan video"));
    }

    private void showSubtitleSheet() {
        LinearLayout box = bottomSheetRoot("Subtitle");
        addRow(box, "◉", "Nonaktifkan", true, null);
        addRow(box, "□", "Impor file lokal", false, v -> Toast.makeText(this, "Pilih file subtitle", Toast.LENGTH_SHORT).show());
        addRow(box, "⇩", "Unduh", false, v -> Toast.makeText(this, "Unduh subtitle", Toast.LENGTH_SHORT).show());
        addRow(box, "→", "Kustomisasi", false, v -> showSimpleChoice("Kustomisasi Subtitle", new String[]{"Ukuran", "Warna", "Posisi", "Latar belakang"}));
        LinearLayout sync = new LinearLayout(this); sync.setGravity(Gravity.CENTER_VERTICAL); sync.setPadding(20, 8, 20, 8);
        TextView label = text("Sinkronisasi", 18); sync.addView(label, new LinearLayout.LayoutParams(0, 70, 1)); Button minus = small("−"); TextView val = text("0,0s", 18); val.setGravity(Gravity.CENTER); Button plus = small("+"); sync.addView(minus); sync.addView(val, new LinearLayout.LayoutParams(dp(90),70)); sync.addView(plus); box.addView(sync);
        minus.setOnClickListener(v -> val.setText("-0,5s")); plus.setOnClickListener(v -> val.setText("+0,5s"));
        showBottom(box);
    }

    private void showQueueSheet() {
        LinearLayout box = bottomSheetRoot("Antrian pemutaran");
        LinearLayout header = (LinearLayout) box.getChildAt(0); TextView title = (TextView) header.getChildAt(0); title.setLayoutParams(new LinearLayout.LayoutParams(0,70,1));
        Button repeatBtn = small("↻"); Button shuffleBtn = small("⤨"); header.addView(repeatBtn); header.addView(shuffleBtn);
        addRow(box, "▶", getIntent().getStringExtra(EXTRA_TITLE) == null ? "Video saat ini" : getIntent().getStringExtra(EXTRA_TITLE), true, null);
        addRow(box, "", "Antrian berikutnya", false, null);
        showBottom(box);
    }

    private LinearLayout bottomSheetRoot(String title) {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(28, 16, 28, 24);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(31,31,31)); bg.setCornerRadii(new float[]{30,30,30,30,0,0,0,0}); box.setBackground(bg);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); TextView t = text(title, 24); header.addView(t, new LinearLayout.LayoutParams(0,70,1)); box.addView(header); return box;
    }

    private void addRow(LinearLayout box, String icon, String label, boolean selected, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0,4,0,4);
        TextView i = text(icon, 25); i.setGravity(Gravity.CENTER); row.addView(i, new LinearLayout.LayoutParams(dp(56),70)); TextView t = text(label,18); row.addView(t,new LinearLayout.LayoutParams(0,70,1));
        if (selected) { i.setTextColor(Color.rgb(77,144,255)); t.setTextColor(Color.WHITE); }
        if (listener != null) row.setOnClickListener(listener); box.addView(row);
    }

    private Button small(String s) { Button b = new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(20); b.setAllCaps(false); b.setBackgroundColor(Color.TRANSPARENT); b.setMinWidth(dp(52)); return b; }

    private void showBottom(View content) {
        android.app.Dialog d = baseDialog(true); d.setContentView(content); d.setCanceledOnTouchOutside(true); d.show();
        Window w = d.getWindow(); if (w != null) { w.setBackgroundDrawableResource(android.R.color.transparent); w.setGravity(Gravity.BOTTOM); w.setLayout(-1, WindowManager.LayoutParams.WRAP_CONTENT); }
    }

    private void showSpeedDialog() { showSimpleChoice("Kecepatan", new String[]{"0,5×","0,75×","1,0×","1,25×","1,5×","2,0×"}); }
    private void showAudioDialog() { showSimpleChoice("Audio", new String[]{"Audio 1", "Audio 2", "Stereo", "Mono"}); }

    private void showSimpleChoice(String title, String[] choices) {
        android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(this); b.setTitle(title); b.setItems(choices, (d, which) -> {
            String choice = choices[which];
            if (title.equals("Kecepatan")) {
                try { player.setPlaybackSpeed(Float.parseFloat(choice.replace('×',' ').trim().replace(',','.'))); } catch (Exception ignored) {}
            }
        }); b.show();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private String formatTime(long ms) { long sec = Math.max(0, ms / 1000); long h = sec / 3600; long m = (sec % 3600) / 60; long s = sec % 60; return h > 0 ? String.format(Locale.getDefault(), "%d:%02d:%02d", h,m,s) : String.format(Locale.getDefault(), "%02d:%02d", m,s); }

    @Override protected void onDestroy() { handler.removeCallbacks(progressUpdater); if (player != null) player.release(); super.onDestroy(); }
}
