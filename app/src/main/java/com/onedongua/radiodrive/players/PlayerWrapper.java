package com.onedongua.radiodrive.players;

import android.content.Context;

import androidx.annotation.NonNull;

import com.onedongua.radiodrive.recording.Recordable;
import com.onedongua.radiodrive.station.live.ShoutcastInfo;
import com.onedongua.radiodrive.station.live.StreamLiveInfo;

import okhttp3.OkHttpClient;

public interface PlayerWrapper extends Recordable {
    interface PlayListener {
        void onStateChanged(PlayState state);

        void onPlayerWarning(final int messageId);

        void onPlayerError(final int messageId);

        void onDataSourceShoutcastInfo(ShoutcastInfo shoutcastInfo, boolean isHls);

        void onDataSourceStreamLiveInfo(StreamLiveInfo liveInfo);
    }

    void playRemote(@NonNull OkHttpClient httpClient, @NonNull String streamUrl, @NonNull Context context, boolean isAlarm);

    void pause();

    void stop();

    boolean isPlaying();

    long getBufferedMs();

    int getAudioSessionId();

    long getTotalTransferredBytes();

    long getCurrentPlaybackTransferredBytes();

    boolean isLocal();

    void setVolume(float newVolume);

    void setStateListener(PlayListener listener);
}
