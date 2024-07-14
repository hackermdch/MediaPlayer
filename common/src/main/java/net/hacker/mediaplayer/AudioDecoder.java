package net.hacker.mediaplayer;

import java.lang.ref.Cleaner;

public final class AudioDecoder implements AutoCloseable {
    @NativeUsed
    private final long ptr;
    private final Cleaner.Cleanable cleaner;

    public AudioDecoder(String path) {
        var p = ptr = open(path);
        cleaner = MediaPlayer.cleaner.register(this, () -> release(p));
    }

    private native long open(String path);

    private static native void release(long ptr);

    public native Audio decode(boolean mono);

    @Override
    public void close() {
        if (cleaner != null) cleaner.clean();
    }
}
