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
    // 占位纹理，避免 null
    private static final ResourceLocation BLANK = new ResourceLocation("minecraft", "textures/misc/unknown_entity.png");

    public VideoEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(VideoEntity entity) {
        // 这里返回占位或实际视频帧纹理
        return BLANK;
    }

    @Override
    public void render(VideoEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 1. 播放音频（仅首次）
        if (entity.decoder != null && !entity.playing) {
            entity.playing = true;
            var audioData = entity.decoder.audio.decode(true);
            Minecraft.getInstance()
                    .getSoundManager()
                    .play(MediaPlayer.audioFactory.apply(audioData, entity));
        }

        // 2. 更新视频帧
        if (entity.decoder != null) {
            entity.decoder.fetch();
        }
        VideoFrame frame = entity.decoder != null ? entity.decoder.frame : null;

        // 3. 获取顶点缓冲和当前矩阵/法线
        VertexConsumer consumer = bufferSource.getBuffer(VideoRenderType.create(frame));
        var pose = poseStack.last();
        var mat4 = pose.pose();
        var mat3 = pose.normal();

        // 4. 四边形绘制（两组三角形）
        // 顶点顺序：左下、左上、右上、右下，逆时针
        drawVertex(consumer, mat4, mat3, -12.5f,  0f,   0f, 1f, 1f, packedLight);
        drawVertex(consumer, mat4, mat3, -12.5f, 14.1f, 0f, 1f, 0f, packedLight);
        drawVertex(consumer, mat4, mat3,  12.5f, 14.1f, 0f, 0f, 0f, packedLight);

        drawVertex(consumer, mat4, mat3,  12.5f, 14.1f, 0f, 0f, 0f, packedLight);
        drawVertex(consumer, mat4, mat3,  12.5f,  0f,   0f, 0f, 1f, packedLight);
        drawVertex(consumer, mat4, mat3, -12.5f,  0f,   0f, 1f, 1f, packedLight);
    }

    /** 绘制单个顶点的辅助方法 **/
    private void drawVertex(VertexConsumer consumer,
                            Matrix4f mat4,
                            Matrix3f mat3,
                            float x, float y, float z,
                            float u, float v,
                            int packedLight) {
        consumer
                .vertex(mat4, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(mat3, 0f, 0f, -1f)
                .endVertex();
    }


    @Override
    public boolean shouldRender(VideoEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        // 永远渲染
        return true;
    }
}
