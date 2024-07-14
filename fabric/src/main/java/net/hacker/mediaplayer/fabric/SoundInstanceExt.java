package net.hacker.mediaplayer.fabric;

import net.minecraft.client.sounds.AudioStream;

import java.util.concurrent.CompletableFuture;

public interface SoundInstanceExt {
    CompletableFuture<AudioStream> getStream();
}
