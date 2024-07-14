package net.hacker.mediaplayer;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.io.FileOutputStream;
import java.lang.ref.Cleaner;
import java.util.Objects;
import java.util.function.BiFunction;

public final class MediaPlayer {
    public static final String MOD_ID = "mediaplayer";
    static final Cleaner cleaner = Cleaner.create();
    public static BiFunction<Audio, Entity, SoundInstance> audioFactory;

    static {
        if (!System.getProperty("os.name").toLowerCase().contains("windows"))
            throw new RuntimeException("Unsupported system");
        var lib = System.getProperty("java.io.tmpdir") + "/MediaPlayer.dll";
        try (var fo = new FileOutputStream(lib); var in = MediaPlayer.class.getResourceAsStream("/MediaPlayer.dll")) {
            fo.write(Objects.requireNonNull(in).readAllBytes());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        System.load(lib);
    }

    public static String getText(String key) {
        return Component.translatable(key).getString();
    }

    public static native void init(long proc);
}
