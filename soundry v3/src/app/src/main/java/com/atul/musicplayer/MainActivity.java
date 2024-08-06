package com.atul.musicplayer;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.atul.musicplayer.activities.PlayerDialog;
import com.atul.musicplayer.adapter.MainPagerAdapter;
import com.atul.musicplayer.helper.MusicLibraryHelper;
import com.atul.musicplayer.helper.PermissionHelper;
import com.atul.musicplayer.helper.ThemeHelper;
import com.atul.musicplayer.listener.MusicSelectListener;
import com.atul.musicplayer.listener.PlayerDialogListener;
import com.atul.musicplayer.model.Music;
import com.atul.musicplayer.player.PlayerBuilder;
import com.atul.musicplayer.player.PlayerListener;
import com.atul.musicplayer.player.PlayerManager;
import com.atul.musicplayer.viewmodel.MainViewModel;
import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayout;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements MusicSelectListener, PlayerListener, View.OnClickListener, PlayerDialogListener {

    public static boolean isSleepTimerRunning;
    public static MutableLiveData<Long> sleepTimerTick;
    private static CountDownTimer sleepTimer;
    private RelativeLayout playerView;
    private ImageView albumArt;
    private TextView songName;
    private TextView songDetails;
    private ImageButton play_pause;
    private LinearProgressIndicator progressIndicator;
    private PlayerDialog playerDialog;
    private PlayerBuilder playerBuilder;
    private PlayerManager playerManager;
    private boolean albumState;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ThemeHelper.getTheme(MPPreferences.getTheme(MainActivity.this)));
        AppCompatDelegate.setDefaultNightMode(MPPreferences.getThemeMode(MainActivity.this));
        setContentView(R.layout.activity_main);
        MPConstants.musicSelectListener = this;

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        if (PermissionHelper.hasReadStoragePermission(MainActivity.this)) {
            fetchMusicList();
            setUpUiElements();
        } else {
            manageStoragePermission(MainActivity.this);
        }

        if (!PermissionHelper.hasNotificationPermission(MainActivity.this)) {
            PermissionHelper.requestNotificationPermission(MainActivity.this);
        }

        albumState = MPPreferences.getAlbumRequest(this);

        MaterialCardView playerLayout = findViewById(R.id.player_layout);
        albumArt = findViewById(R.id.albumArt);
        progressIndicator = findViewById(R.id.song_progress);
        playerView = findViewById(R.id.player_view);
        songName = findViewById(R.id.song_title);
        songDetails = findViewById(R.id.song_details);
        play_pause = findViewById(R.id.control_play_pause);

        play_pause.setOnClickListener(this);
        playerLayout.setOnClickListener(this);
    }

    private void setPlayerView() {
        if (playerManager != null && playerManager.isPlaying()) {
            playerView.setVisibility(View.VISIBLE);
            onMusicSet(playerManager.getCurrentMusic());
        }
    }

    private void fetchMusicList() {
        new Handler().post(() -> {
            List<Music> musicList = MusicLibraryHelper.fetchMusicLibrary(MainActivity.this);
            viewModel.setSongsList(musicList);
            viewModel.parseFolderList(musicList);
        });
    }

    public void setUpUiElements() {
        playerBuilder = new PlayerBuilder(MainActivity.this, this);
        MainPagerAdapter sectionsPagerAdapter = new MainPagerAdapter(
                getSupportFragmentManager(), this);

        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(3);

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        for (int i = 0; i < tabs.getTabCount(); i++) {
            TabLayout.Tab tab = tabs.getTabAt(i);
            if (tab != null) {
                tab.setIcon(MPConstants.TAB_ICONS[i]);
            }
        }
    }

    public void manageStoragePermission(Activity context) {
            // required a dialog?
        if (PermissionHelper.requirePermissionRationale(context)) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Requesting permission")
                    .setMessage("Enable storage permission for accessing the media files.")
                    .setPositiveButton("Accept", (dialog, which) -> PermissionHelper.requestStoragePermission(context)).show();
        } else {
            PermissionHelper.requestStoragePermission(context);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ThemeHelper.applySettings(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerBuilder != null)
            playerBuilder.unBindService();

        if (playerDialog != null)
            playerDialog.dismiss();
    }

    @Override
    public void playQueue(List<Music> musicList, boolean shuffle) {
        if (shuffle) {
            Collections.shuffle(musicList);
        }

        if (!musicList.isEmpty()) {
            playerManager.setMusicList(musicList);
            setPlayerView();
        }
    }

    @Override
    public void addToQueue(List<Music> musicList) {
        if (!musicList.isEmpty()) {
            if (playerManager != null && playerManager.isPlaying())
                playerManager.addMusicQueue(musicList);
            else if (playerManager != null)
                playerManager.setMusicList(musicList);

            setPlayerView();
        }
    }

    @Override
    public void refreshMediaLibrary() {
        fetchMusicList();
    }

    @Override
    public void onPrepared() {
        playerManager = playerBuilder.getPlayerManager();
        setPlayerView();
    }

    @Override
    public void onStateChanged(int state) {
        if (state == State.PLAYING)
            play_pause.setImageResource(R.drawable.ic_controls_pause);
        else
            play_pause.setImageResource(R.drawable.ic_controls_play);
    }

    @Override
    public void onPositionChanged(int position) {
        progressIndicator.setProgress(position);
    }

    @Override
    public void onMusicSet(Music music) {
        songName.setText(music.title);
        songDetails.setText(
                String.format(Locale.getDefault(), "%s • %s",
                        music.artist, music.album));
        playerView.setVisibility(View.VISIBLE);

        if (albumState)
            Glide.with(getApplicationContext())
                    .load(music.albumArt)
                    .centerCrop()
                    .into(albumArt);

        if (playerManager != null && playerManager.isPlaying())
            play_pause.setImageResource(R.drawable.ic_controls_pause);
        else
            play_pause.setImageResource(R.drawable.ic_controls_play);
    }

    @Override
    public void onPlaybackCompleted() {
    }

    @Override
    public void onRelease() {
        playerView.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.control_play_pause)
            playerManager.playPause();

        else if (id == R.id.player_layout)
            setUpPlayerDialog();



        int rewind = v.getId();

        if (rewind == R.id.minusrewind)
            playerManager.seekTo(24000);

        int forward = v.getId();

        if (forward == R.id.plusforward)
            playerManager.seekTo(24000);

    }

    public void forwardSong(){
        if (playerManager != null){
            int currentPosition = playerManager.getCurrentPosition();
            if (currentPosition + plusforward <= playerManager.getDuration()){
            playerManager.seekTo(currentPosition + plusforward);
            } else {
            playerManager.seekTo(playerManager.getDuration());
        }
    }




    private void setUpPlayerDialog() {
        playerDialog = new PlayerDialog(this, playerManager, this);

        playerDialog.show();
    }
}