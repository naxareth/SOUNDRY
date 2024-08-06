package com.atul.musicplayer.fragments;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atul.musicplayer.MPConstants;
import com.atul.musicplayer.MPPreferences;
import com.atul.musicplayer.R;
import com.atul.musicplayer.activities.FolderDialog;
import com.atul.musicplayer.adapter.AccentAdapter;
import com.atul.musicplayer.helper.ThemeHelper;
import com.atul.musicplayer.model.Folder;
import com.atul.musicplayer.viewmodel.MainViewModel;
import com.bullhead.equalizer.DialogEqualizerFragment;
import com.bullhead.equalizer.EqualizerFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.atul.musicplayer.player.PlayerManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment implements View.OnClickListener {

    private MainViewModel viewModel;
    private RecyclerView accentView;
    private boolean state;
    private boolean autoPlayState;
    private LinearLayout chipLayout;
    private ImageView currentThemeMode;

    private List<Folder> folderList;
    private MaterialToolbar toolbar;
    private FolderDialog folderDialog;

    private MediaPlayer mediaPlayer;

    private EqualizerFragment equalizerFragment;

    private FragmentManager fragmentManager;

    private FrameLayout eqFrame;

    private PlayerManager playerManager;

    public SettingsFragment() {
        // Unused
    }

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getActivity().getSupportFragmentManager();
        mediaPlayer = new MediaPlayer();
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        viewModel.getFolderList().observe(requireActivity(), folders -> {
            if (folderList == null)
                folderList = new ArrayList<>();
            folderList.clear();
            folderList.addAll(folders);
        });


        SwitchMaterial autoPlaySwitch = view.findViewById(R.id.auto_play_switch);
        accentView = view.findViewById(R.id.accent_view);
        chipLayout = view.findViewById(R.id.chip_layout);
        currentThemeMode = view.findViewById(R.id.current_theme_mode);
        toolbar = view.findViewById(R.id.toolbar);


        LinearLayout themeModeOption = view.findViewById(R.id.theme_mode_option);
        LinearLayout folderOption = view.findViewById(R.id.folder_options);
        LinearLayout refreshOption = view.findViewById(R.id.refresh_options);
        LinearLayout equalizerPress = view.findViewById(R.id.equalizer_press);

        state = MPPreferences.getAlbumRequest(requireActivity().getApplicationContext());
        autoPlayState = MPPreferences.getAutoPlay(requireActivity().getApplicationContext());
        autoPlaySwitch.setChecked(autoPlayState);
        setCurrentThemeMode();

        accentView.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
        accentView.setAdapter(new AccentAdapter(getActivity()));

        autoPlaySwitch.setOnClickListener(this);
        themeModeOption.setOnClickListener(this);
        folderOption.setOnClickListener(this);
        refreshOption.setOnClickListener(this);
        equalizerPress.setOnClickListener(this);

        view.findViewById(R.id.night_chip).setOnClickListener(this);
        view.findViewById(R.id.light_chip).setOnClickListener(this);
        view.findViewById(R.id.auto_chip).setOnClickListener(this);


        setUpOptions();

        return view;

    }


    private void setUpOptions() {
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            return false;
        });
        toolbar.setNavigationOnClickListener(v -> requireActivity().finish());
    }

    private void setCurrentThemeMode() {
        int mode = MPPreferences.getThemeMode(requireActivity().getApplicationContext());

        if (mode == AppCompatDelegate.MODE_NIGHT_NO)
            currentThemeMode.setImageResource(R.drawable.ic_theme_mode_light);

        else if (mode == AppCompatDelegate.MODE_NIGHT_YES)
            currentThemeMode.setImageResource(R.drawable.ic_theme_mode_night);

        else
            currentThemeMode.setImageResource(R.drawable.ic_theme_mode_auto);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.auto_play_switch)
            setAutoPlay();

        else if (id == R.id.theme_mode_option) {
            int mode = chipLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            chipLayout.setVisibility(mode);
        } else if (id == R.id.night_chip)
            selectTheme(AppCompatDelegate.MODE_NIGHT_YES);

        else if (id == R.id.light_chip)
            selectTheme(AppCompatDelegate.MODE_NIGHT_NO);

        else if (id == R.id.auto_chip)
            selectTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        else if (id == R.id.folder_options)
            showFolderSelectionDialog();

        else if (id == R.id.equalizer_press){
            DialogEqualizerFragment fragment = DialogEqualizerFragment.newBuilder()
                    .setAudioSessionId(0)
                    .themeColor(ContextCompat.getColor(getActivity(), R.color.black))
                    .build();
           fragment.show(getActivity().getSupportFragmentManager(), "eq");
        }

        else if (id == R.id.refresh_options) {
            refreshMediaLibrary();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (folderDialog != null)
            folderDialog.dismiss();
    }

    private void showFolderSelectionDialog() {
        if (folderList != null) {
            folderDialog = new FolderDialog(requireActivity(), folderList);
            folderDialog.show();

            folderDialog.setOnDismissListener(dialog -> refreshMediaLibrary());
        } else {
            Toast.makeText(requireActivity(), "Folder list missing", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshMediaLibrary() {
        Toast.makeText(requireActivity(), "Refreshing media library", Toast.LENGTH_SHORT).show();
        MPConstants.musicSelectListener.refreshMediaLibrary();
    }

    private void selectTheme(int theme) {
        AppCompatDelegate.setDefaultNightMode(theme);
        MPPreferences.storeThemeMode(requireActivity().getApplicationContext(), theme);
    }

    private void setAlbumRequest() {
        MPPreferences.storeAlbumRequest(requireActivity().getApplicationContext(), (!state));
        ThemeHelper.applySettings(getActivity());
    }

    private void setAutoPlay() {
        MPPreferences.storeAutoPlay(requireActivity().getApplicationContext(), (!autoPlayState));
    }
}