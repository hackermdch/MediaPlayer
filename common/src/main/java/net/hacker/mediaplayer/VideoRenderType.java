package net.hacker.mediaplayer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

public class VideoRenderType extends RenderType {
    public static RenderType create(VideoFrame frame) {
        var state = RenderType.CompositeState.builder()
                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new VideoTextureStateShard(frame))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setCullState(NO_CULL)  // 添加这一行，禁用背面剔除
                .createCompositeState(true);
        return create("video", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES, 1536, true, false, state);
    }

    private VideoRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    private static class VideoTextureStateShard extends RenderStateShard.EmptyTextureStateShard {
        public VideoTextureStateShard(VideoFrame frame) {
            super(frame == null ? () -> RenderSystem.setShaderTexture(0, 0) : () -> RenderSystem.setShaderTexture(0, frame.getId()), () -> {
            });
        }
    }
}