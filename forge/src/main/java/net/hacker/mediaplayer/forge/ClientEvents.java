package net.hacker.mediaplayer.forge;

import net.hacker.mediaplayer.*;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.server.command.EnumArgument;

import static net.hacker.mediaplayer.MediaPlayer.MOD_ID;
import static net.hacker.mediaplayer.MediaPlayer.getText;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    private static void onRegisterEntityRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MediaPlayerForge.video.get(), VideoEntityRenderer::new);
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
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
            }).then(Commands.argument("hardware", EnumArgument.enumArgument(DeviceType.class)).executes(c -> {
                var f = NativeFileDialog.openFileDialog(getText("media.command.open"), "D:/", getText("media.command.video"), "*.*");
                if (f != null) {
                    assert Minecraft.getInstance().level != null;
                    Minecraft.getInstance().level.entitiesForRendering().forEach(e -> {
                        if (e instanceof VideoEntity v && v.decoder == null) {
                            v.decoder = new VideoDecoder(f.getAbsolutePath(), c.getArgument("hardware", DeviceType.class));
                        }
                    });
                }
                return 0;
            }))).then(literal("audio").executes(c -> {
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
