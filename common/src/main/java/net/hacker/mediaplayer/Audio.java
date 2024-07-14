package net.hacker.mediaplayer;

import net.minecraft.client.sounds.AudioStream;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;

public class Audio implements AudioStream {
    private final AudioFormat format;
    private final ByteBuffer data;
    private int offset;

    public Audio(ByteBuffer data, int channels) {
        this.data = data;
        format = new AudioFormat(44100, 16, channels, false, false);
    }

    @Override
    @NotNull
    public AudioFormat getFormat() {
        return format;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ByteBuffer read(int size) {
        var buff = BufferUtils.createByteBuffer(size);
        if (offset + size < data.capacity()) {
            MemoryUtil.memCopy(MemoryUtil.memAddress(data) + offset, MemoryUtil.memAddress(buff), size);
            offset += size;
        } else if (offset < data.capacity() - 1) {
            MemoryUtil.memSet(buff, 0);
            MemoryUtil.memCopy(MemoryUtil.memAddress(data) + offset, MemoryUtil.memAddress(buff), data.capacity() - offset - 1);
            offset = data.capacity() - 1;
        } else return null;
        return buff;
    }

    @Override
    public void close() {
    }
}
