package net.hacker.mediaplayer;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.lang.ref.Cleaner;

import static org.lwjgl.glfw.GLFW.glfwGetTime;

public final class VideoDecoder implements AutoCloseable {
    @NativeUsed
    private final long ptr;
    @NativeUsed
    private double frameRate;
    private final Cleaner.Cleanable cleaner;
    public final VideoFrame frame;
    public final AudioDecoder audio;
    private double lastTime;
    private double deltaTime;
    private final Minecraft minecraft = Minecraft.getInstance();

    public VideoDecoder(String path, DeviceType type) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Video path is null or blank");
        }

        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        AudioDecoder a = null;
        try {
            a = new AudioDecoder(path);
        } catch (Throwable ignore) {
            // 你可以打印 warning 日志以调试
            MediaPlayer.LOGGER.warn("无法创建音频解码器: {}", path);
        }
        audio = a;

        frame = new VideoFrame();
        frame.setFilter(false, false);

        var textureId = frame.getId();
        if (textureId == 0) {
            throw new IllegalStateException("VideoFrame texture ID is 0");
        }

        try {
            long p = open(path, textureId, type.value);
            if (p == 0) {
                throw new RuntimeException("Native open() failed: returned NULL ptr (path=" + path + ", type=" + type + ")");
            }
            
            ptr = p;
            cleaner = MediaPlayer.cleaner.register(this, () -> release(p));
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Native method open() not found or incompatible", e);
        }

        lastTime = glfwGetTime();
        deltaTime = 0.0;
    }


    public double getFrameRate() {
        return frameRate;
    }

    public void fetch() {
        var currentTime = glfwGetTime();
        var frameInterval = currentTime - lastTime;
        lastTime = currentTime;
        deltaTime += frameInterval;
        if (deltaTime >= frameRate) {
            deltaTime -= frameRate;
            if (!minecraft.isPaused()) decode();
        }
    }

    private native long open(String path, int texture, int type);

    private static native void release(long ptr);

    public native void decode();

    public static VideoDecoder create(File file) {
        try {
            return new VideoDecoder(file.getAbsolutePath(), DeviceType.CUDA);
        } catch (Throwable e) {
            try {
                return new VideoDecoder(file.getAbsolutePath(), DeviceType.D3D12VA);
            } catch (Throwable e1) {
                try {
                    return new VideoDecoder(file.getAbsolutePath(), DeviceType.D3D11VA);
                } catch (Throwable e2) {
                    try {
                        return new VideoDecoder(file.getAbsolutePath(), DeviceType.NONE);
                    } catch (Throwable e3) {
                        throw new RuntimeException(e3);
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        if (cleaner != null) cleaner.clean();
    }
}
