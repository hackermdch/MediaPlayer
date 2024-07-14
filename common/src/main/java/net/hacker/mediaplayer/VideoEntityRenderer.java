package net.hacker.mediaplayer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

public class VideoEntityRenderer extends EntityRenderer<VideoEntity> {
    public VideoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(VideoEntity entity) {
        return ResourceLocation.withDefaultNamespace("");
    }

    @Override
    public void render(VideoEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        VideoFrame frame = null;
        if (entity.decoder != null) {
            if (!entity.playing) {
                entity.playing = true;
                Minecraft.getInstance().getSoundManager().play(MediaPlayer.audioFactory.apply(entity.decoder.audio.decode(true), entity));
            }
            entity.decoder.fetch();
            frame = entity.decoder.frame;
        }
        var buf = bufferSource.getBuffer(VideoRenderType.create(frame));
        var matrix = poseStack.last().pose();
        buf.addVertex(matrix, -12.5f, 0, 0).setUv(1, 1).setColor(0xffffffff).setOverlay(NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        buf.addVertex(matrix, -12.5f, 14.1f, 0).setUv(1, 0).setColor(0xffffffff).setOverlay(NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        buf.addVertex(matrix, 12.5f, 14.1f, 0).setUv(0, 0).setColor(0xffffffff).setOverlay(NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        buf.addVertex(matrix, 12.5f, 0, 0).setUv(0, 1).setColor(0xffffffff).setOverlay(NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        buf.addVertex(matrix, 12.5f, 0, 0).setUv(1, 1).setColor(0xffffffff).setOverlay(NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        buf.addVertex(matrix, 12.5f, 14.1f, 0).setUv(1, 0).setColor(0xffffffff).setOverlay(NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        buf.addVertex(matrix, -12.5f, 14.1f, 0).setUv(0, 0).setColor(0xffffffff).setOverlay(NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
        buf.addVertex(matrix, -12.5f, 0, 0).setUv(0, 1).setColor(0xffffffff).setOverlay(NO_OVERLAY).setLight(packedLight).setNormal(poseStack.last(), 0, 1, 0);
    }

    @Override
    public boolean shouldRender(VideoEntity livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}
