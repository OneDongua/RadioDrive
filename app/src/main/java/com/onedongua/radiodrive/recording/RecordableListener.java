package com.onedongua.radiodrive.recording;

public interface RecordableListener {
    void onBytesAvailable(byte[] buffer, int offset, int length);

    void onRecordingEnded();
}
