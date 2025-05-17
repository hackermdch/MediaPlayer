package net.hacker.mediaplayer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 1.20.1 重构版 VideoEntityRenderer
 */
public class VideoEntityRenderer extends EntityRenderer<VideoEntity> {
    private static final ResourceLocation BLANK = new ResourceLocation("");

    public VideoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(VideoEntity entity) {
        return BLANK;
    }

    @Override
    public void render(VideoEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!entity.playing && entity.decoder != null) {
            entity.playing = true;
            var audioData = entity.decoder.audio.decode(true);
            Minecraft.getInstance().getSoundManager().play(MediaPlayer.audioFactory.apply(audioData, entity));
        }

        if (entity.decoder != null) entity.decoder.fetch();

        var consumer = bufferSource.getBuffer(VideoRenderType.create(entity.decoder != null ? entity.decoder.frame : null));
        var pose = poseStack.last();
        var mat4 = pose.pose();
        var mat3 = pose.normal();

        drawVertex(consumer, mat4, mat3, -12.5f, 0f, 1f, 1f, packedLight);
        drawVertex(consumer, mat4, mat3, -12.5f, 14.1f, 1f, 0f, packedLight);
        drawVertex(consumer, mat4, mat3, 12.5f, 14.1f, 0f, 0f, packedLight);

        drawVertex(consumer, mat4, mat3, 12.5f, 14.1f, 0f, 0f, packedLight);
        drawVertex(consumer, mat4, mat3, 12.5f, 0f, 0f, 1f, packedLight);
        drawVertex(consumer, mat4, mat3, -12.5f, 0f, 1f, 1f, packedLight);
    }

    private void drawVertex(VertexConsumer consumer, Matrix4f mat4, Matrix3f mat3, float x, float y, float u, float v, int packedLight) {
        consumer.vertex(mat4, x, y, 0).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(mat3, 0f, 0f, -1f).endVertex();
    }

    @Override
    public boolean shouldRender(VideoEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }
}
