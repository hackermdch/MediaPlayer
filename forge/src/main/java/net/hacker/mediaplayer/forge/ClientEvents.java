package net.hacker.mediaplayer.forge;

import net.hacker.mediaplayer.*;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.hacker.mediaplayer.MediaPlayer.MOD_ID;
import static net.hacker.mediaplayer.MediaPlayer.getText;
import static net.minecraft.commands.Commands.literal;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    private static void onRegisterEntityRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MediaPlayerForge.video.get(), VideoEntityRenderer::new);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    private static class Forge {
        @SubscribeEvent
        private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(literal("mediaplayer").then(literal("video").executes(c -> {
                var f = NativeFileDialog.openFileDialog(getText("media.command.open"), "D:/", getText("media.command.video"), "*.*");
                if (f != null) {
                    assert Minecraft.getInstance().level != null;
                    Minecraft.getInstance().level.entitiesForRendering().forEach(e -> {
                        if (e instanceof VideoEntity v && v.decoder == null) {
                            v.decoder = VideoDecoder.create(f);
                        }
                    });
                }
                return 0;
            })).then(literal("audio").executes(c -> {
                var f = NativeFileDialog.openFileDialog(getText("media.command.open"), "D:/", getText("media.command.audio"), "*.*");
                if (f != null) {
                    try (var a = new AudioDecoder(f.getAbsolutePath())) {
                        Minecraft.getInstance().getSoundManager().play(new AudioInstance(a.decode(false), null));
                    } catch (Throwable ignore) {
                    }
                }
                return 0;
            })).then(literal("clear").executes(c -> {
                assert Minecraft.getInstance().level != null;
                Minecraft.getInstance().level.entitiesForRendering().forEach(e -> {
                    if (e instanceof VideoEntity v && v.decoder != null) {
                        v.decoder.close();
                        v.decoder = null;
                        v.playing = false;
                        Minecraft.getInstance().getSoundManager().stop();
                    }
                });
                return 0;
            })));
        }
    }
}
