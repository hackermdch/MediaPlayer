package net.hacker.mediaplayer.forge;

import net.hacker.mediaplayer.MediaPlayer;
import net.hacker.mediaplayer.VideoEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static net.hacker.mediaplayer.MediaPlayer.MOD_ID;

@Mod(MOD_ID)
public class MediaPlayerForge {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MOD_ID);
    public static DeferredHolder<EntityType<?>, EntityType<VideoEntity>> video;

    public MediaPlayerForge(IEventBus modEventBus) {
        MediaPlayer.audioFactory = AudioInstance::new;
        video = ENTITY_TYPES.register("video", VideoEntity::type);
        ENTITY_TYPES.register(modEventBus);
    }
}