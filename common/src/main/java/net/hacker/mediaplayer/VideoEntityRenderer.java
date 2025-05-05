package net.hacker.mediaplayer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;

import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

public class VideoEntityRenderer extends EntityRenderer<VideoEntity> {
    public VideoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(VideoEntity entity) {
        return new ResourceLocation("");
    }

    @Override
    public void render(VideoEntity entity, float entityYaw, float partialTick, PoseStack poseStack , MultiBufferSource bufferSource, int packedLight) {
        VideoFrame frame = null;
        final Matrix3f normal = poseStack.last().normal();
//        System.out.println("[VideoEntityRenderer] decoder=" + entity.decoder);
        if (entity.decoder != null) {
            if (!entity.playing) {
                entity.playing = true;
//                System.out.println("[VideoEntityRenderer] Start playing audio");
                Minecraft.getInstance().getSoundManager().play(MediaPlayer.audioFactory.apply(entity.decoder.audio.decode(true), entity));
            }
            entity.decoder.fetch();
            frame = entity.decoder.frame;
//            System.out.println("[VideoEntityRenderer] frame=" + frame);
        } else {
//            System.out.println("[VideoEntityRenderer] decoder is null");
        }
        var buf = bufferSource.getBuffer(VideoRenderType.create(frame));
        var matrix = poseStack.last().pose();
//        System.out.println("[VideoEntityRenderer] buffer=" + buf + ", matrix=" + matrix);
        buf.vertex(matrix, -12.5f, 0, 0).uv(1, 1).color(0xffffffff).overlayCoords(NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0);
        buf.vertex(matrix, -12.5f, 14.1f, 0).uv(1, 0).color(0xffffffff).overlayCoords(NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0);
        buf.vertex(matrix, 12.5f, 14.1f, 0).uv(0, 0).color(0xffffffff).overlayCoords(NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0);
        buf.vertex(matrix, 12.5f, 0, 0).uv(0, 1).color(0xffffffff).overlayCoords(NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0);
        buf.vertex(matrix, 12.5f, 0, 0).uv(1, 1).color(0xffffffff).overlayCoords(NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0);
        buf.vertex(matrix, 12.5f, 14.1f, 0).uv(1, 0).color(0xffffffff).overlayCoords(NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0);
        buf.vertex(matrix, -12.5f, 14.1f, 0).uv(0, 0).color(0xffffffff).overlayCoords(NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0);
        buf.vertex(matrix, -12.5f, 0, 0).uv(0, 1).color(0xffffffff).overlayCoords(NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0);
    }

    @Override
    public boolean shouldRender(VideoEntity livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }
}
