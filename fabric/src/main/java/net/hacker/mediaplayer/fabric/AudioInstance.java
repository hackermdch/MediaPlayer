package net.hacker.mediaplayer.fabric;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class AudioInstance extends AbstractTickableSoundInstance implements SoundInstanceExt {
    private static final FloatProvider DEFAULT_FLOAT = ConstantFloat.of(1.0F);
    private final AudioStream audio;
    private final Entity entity;

    public AudioInstance(AudioStream audio, Entity entity) {
        super(SoundEvents.EMPTY, SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.audio = audio;
        this.entity = entity;
    }

    @Override
    public void tick() {
        if (entity != null) {
            x = entity.getX();
            y = entity.getY();
            z = entity.getZ();
            if (entity.isRemoved()) Minecraft.getInstance().getSoundManager().stop(this);
        }
    }

    @Override
    public WeighedSoundEvents resolve(@NotNull SoundManager manager) {
        sound = new Sound(getLocation().toString(), DEFAULT_FLOAT, DEFAULT_FLOAT, 1, Sound.Type.FILE, true, false, 32);
        return SoundManager.INTENTIONALLY_EMPTY_SOUND_EVENT;
    }

    @Override
    public CompletableFuture<AudioStream> getStream() {
        return CompletableFuture.supplyAsync(() -> audio, Util.backgroundExecutor());
    }

    @Override
    @NotNull
    public Attenuation getAttenuation() {
        return Attenuation.LINEAR;
    }

    @Override
    public boolean isRelative() {
        return false;
    }
}
