package net.hacker.mediaplayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

public class VideoEntity extends Entity {
    @Environment(EnvType.CLIENT)
    public VideoDecoder decoder;
    @Environment(EnvType.CLIENT)
    public boolean playing = false;

    public VideoEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData () {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }

    public static EntityType<VideoEntity> type() {
        return EntityType.Builder.of(VideoEntity::new, MobCategory.MISC).sized(1, 1).build("video");
    }
}
