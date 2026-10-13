package decade.items.entity;

import me.xjqsh.lrtactical.config.ServerConfig;
import me.xjqsh.lrtactical.entity.ThrowableItemEntity;
import me.xjqsh.lrtactical.item.throwable.ThrowableData;
import me.xjqsh.lrtactical.item.throwable.ThrowableType;
import me.xjqsh.lrtactical.resource.CommonAssetsManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;

/**
 * The thrown lure: an lrtactical throwable that lands and lies there for its life time, then goes. A shell
 * only: drawing zombies to it, and the beep players hear, are decade_noise's on the server, which finds it by this
 * entity type's registry name {@code decade:lure}. Not saved, like lrtactical's smoke grenade: a restart ends it.
 */
public class LureEntity extends ThrowableItemEntity {
    public static final EntityType<LureEntity> TYPE = EntityType.Builder.<LureEntity>of(LureEntity::new, MobCategory.MISC)
            .setShouldReceiveVelocityUpdates(true)
            .setTrackingRange(64)
            .setUpdateInterval(1)
            .setCustomClientFactory(LureEntity::new)
            .sized(0.3f, 0.3f)
            .noSave()
            .noSummon()
            .fireImmune()
            .build("lure");

    /** The throwable type the gun pack's index names ({@code "type": "decade:lure"}). */
    public static final ThrowableType<ThrowableData, LureEntity> LURE = ThrowableType.Builder.<ThrowableData, LureEntity>of()
            .setFactory(LureEntity::create)
            .setSerializer(json -> CommonAssetsManager.GSON.fromJson(json, ThrowableData.class))
            .build();

    public LureEntity(LivingEntity thrower, Level level, int lifeTime) {
        super(TYPE, thrower, level, lifeTime);
    }

    public LureEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        super(TYPE, level);
    }

    public LureEntity(EntityType<LureEntity> type, Level level) {
        super(type, level);
    }

    /** As lrtactical makes its smoke grenade: thrown along the thrower's view, slower when crouching. */
    static LureEntity create(ItemStack stack, LivingEntity thrower, ThrowableData data) {
        LureEntity entity = new LureEntity(thrower, thrower.level(), data.getEntityData().getLifeTime());
        float speed = (float) data.getInitialSpeed();
        if (thrower.isCrouching()) {
            speed *= ServerConfig.CROUCHING_INIT_SPEED_PERCENT.get();
        }
        entity.shootFromRotation(entity, thrower.getXRot(), thrower.getYRot(), 0.0F, speed, 1.0F);
        entity.setItem(stack);
        entity.setBaseData(data.getEntityData());
        return entity;
    }
}
