package net.hacker.mediaplayer.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.hacker.mediaplayer.*;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.hacker.mediaplayer.MediaPlayer.getText;

public class Client implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(MediaPlayerFabric.video, VideoEntityRenderer::new);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("mediaplayer").then(literal("video").executes(c -> {
                var f = NativeFileDialog.openFileDialog(getText("media.command.open"), "D:/", getText("media.command.video"), "*.*");
                if (f != null) {
                    c.getSource().getWorld().entitiesForRendering().forEach(e -> {
                        if (e instanceof VideoEntity v && v.decoder == null) {
                            v.decoder = VideoDecoder.create(f);
                        }
                    });
                }
                return 0;
            }).then(argument("hardware", StringArgumentType.word()).suggests((c, b) -> b.suggest("cuda").suggest("d3d12").suggest("d3d11").suggest("none").buildFuture()).executes(c -> {
                var f = NativeFileDialog.openFileDialog(getText("media.command.open"), "D:/", getText("media.command.video"), "*.*");
                if (f != null) {
                    assert Minecraft.getInstance().level != null;
                    Minecraft.getInstance().level.entitiesForRendering().forEach(e -> {
                        if (e instanceof VideoEntity v && v.decoder == null) {
                            var type = StringArgumentType.getString(c, "hardware");
                            v.decoder = new VideoDecoder(f.getAbsolutePath(), switch (type) {
                                case "cuda" -> DeviceType.CUDA;
                                case "d3d12" -> DeviceType.D3D12VA;
                                case "d3d11" -> DeviceType.D3D11VA;
                                case "none" -> DeviceType.NONE;
                                default -> throw new RuntimeException("Unknown device type: " + type);
                            });
                        }
                    });
                }
                return 0;
            }))).then(literal("audio").executes(c -> {
                var f = NativeFileDialog.openFileDialog(getText("media.command.open"), "D:/", getText("media.command.audio"), "*.*");
                if (f != null) {
                    try (var a = new AudioDecoder(f.getAbsolutePath())) {
                        c.getSource().getClient().getSoundManager().play(new AudioInstance(a.decode(false), null));
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
        });
    }
}
