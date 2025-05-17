package net.hacker.mediaplayer.forge;

import net.hacker.mediaplayer.MediaPlayer;
import net.hacker.mediaplayer.VideoEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


@Mod(MediaPlayer.MOD_ID)
public class MediaPlayerForge {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MediaPlayer.MOD_ID);
    public static RegistryObject<EntityType<VideoEntity>> video;

    public MediaPlayerForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MediaPlayer.audioFactory = AudioInstance::new;
        video = ENTITY_TYPES.register("video", VideoEntity::type);
        ENTITY_TYPES.register(modEventBus);
    }
}