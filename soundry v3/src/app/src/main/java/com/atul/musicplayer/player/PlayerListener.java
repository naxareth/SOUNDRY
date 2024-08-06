package com.atul.musicplayer.player;

import androidx.annotation.IntDef;

import com.atul.musicplayer.model.Music;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface PlayerListener {

    void onPrepared();

    void onStateChanged(@State int state);

    void onPositionChanged(int position);

    void onMusicSet(Music music);

    void onPlaybackCompleted();

    void onRelease();

    @IntDef({State.INVALID,
            State.PLAYING,
            State.PAUSED,
            State.COMPLETED,
            State.RESUMED})
    @Retention(RetentionPolicy.SOURCE)
    @interface State {
        int INVALID = -1;
        int PLAYING = 0;
        int PAUSED = 1;
        int COMPLETED = 2;
        int RESUMED = 3;
    }
}
