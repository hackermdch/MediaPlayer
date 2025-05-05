package net.hacker.mediaplayer.fabric;

import net.fabricmc.api.ModInitializer;
import net.hacker.mediaplayer.MediaPlayer;
import net.hacker.mediaplayer.VideoEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import static net.hacker.mediaplayer.MediaPlayer.MOD_ID;

public class MediaPlayerFabric implements ModInitializer {
    public static EntityType<VideoEntity> video;

    @Override
    public void onInitialize() {
        MediaPlayer.audioFactory = AudioInstance::new;
        video = Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation(MOD_ID, "video"), VideoEntity.type());
    }
}