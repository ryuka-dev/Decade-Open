package me.xjqsh.lrtactical.item;

import com.tacz.guns.api.item.IAnimationItem;
import me.xjqsh.lrtactical.EquipmentMod;
import me.xjqsh.lrtactical.api.event.ConsumableUseEvent;
import me.xjqsh.lrtactical.api.item.IConsumable;
import me.xjqsh.lrtactical.capability.CombatPropertiesProvider;
import me.xjqsh.lrtactical.capability.CustomItemCoolDownsProvider;
import me.xjqsh.lrtactical.client.renderer.item.ConsumableItemRenderer;
import me.xjqsh.lrtactical.inventory.tooltip.ConsumableTooltip;
import me.xjqsh.lrtactical.item.consumable.ConsumableData;
import me.xjqsh.lrtactical.item.index.ConsumableIndex;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ConsumableItem extends Item implements IAnimationItem, IConsumable {
    /**
     * Effects a category selector (@harmful, @beneficial, @neutral) leaves alone, for effects that
     * another mod removes only through its own cure. Naming one in remove_effects still removes it.
     */
    public static final TagKey<MobEffect> CATEGORY_REMOVAL_IMMUNE =
            TagKey.create(Registries.MOB_EFFECT, new ResourceLocation(EquipmentMod.MOD_ID, "category_removal_immune"));

    public ConsumableItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @Nullable FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        return this.getConsumableIndex(stack).map(ConsumableIndex::getFoodProperties).orElse(null);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return this.getConsumableIndex(stack).map(ConsumableIndex::getMaxStackSize).orElse(1);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return this.getConsumableIndex(stack)
                .map(index -> index.getData().getMaxDurability())
                .orElse(0);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return this.getMaxDamage(stack) > 0;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return this.getConsumableIndex(stack).map(index -> {
            if (index.getData().isToggleUse()) {
                return 72000;
            }
            return index.getData().getUseDuration();
        }).orElse(0);
    }

    @NotNull
    @Override
    public UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @NotNull
    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.EMPTY;
    }

    @Override
    public int getDrawTime(ItemStack stack) {
        return this.getConsumableIndex(stack).map(index -> index.getData().getDrawTime()).orElse(0);
    }

    @Override
    public int getPutAwayTime(ItemStack stack) {
        return this.getConsumableIndex(stack).map(index -> index.getData().getPutAwayTime()).orElse(0);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ConsumableItemRenderer renderer = null;

            @Override
            public ConsumableItemRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new ConsumableItemRenderer();
                }
                return renderer;
            }
        });
    }

    @NotNull
    @Override
    public String getDescriptionId(@NotNull ItemStack stack) {
        return this.getConsumableIndex(stack).map(ConsumableIndex::getDescriptionId).orElse(super.getDescriptionId(stack));
    }

    @ParametersAreNonnullByDefault
    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (usedHand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.fail(player.getItemInHand(usedHand));
        }

        ItemStack stack = player.getItemInHand(usedHand);
        boolean drawing = player.getCapability(CombatPropertiesProvider.CAPABILITY)
                .map(cap -> cap.getCoolDownTick() > 0)
                .orElse(false);
        if (drawing) {
            return InteractionResultHolder.fail(stack);
        }

        boolean onCooldown = getCoolDownId(stack)
                .map(id -> player.getCapability(CustomItemCoolDownsProvider.CAPABILITY)
                        .map(cap -> cap.isOnCooldown(id))
                        .orElse(false)
                ).orElse(false);
        if (onCooldown) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(usedHand);
        return InteractionResultHolder.consume(stack);
    }

    @ParametersAreNonnullByDefault
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return finishConsumableUse(stack, level, entity);
    }

    @ParametersAreNonnullByDefault
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        this.getConsumableIndex(stack).ifPresent(index -> {
            if (index.getData().isToggleUse() && entity.getTicksUsingItem() >= index.getData().getUseDuration()) {
                // on the client too, for the local player: see finishConsumableUse
                if (!level.isClientSide() || isLocalPlayer(entity)) {
                    finishConsumableUse(stack, level, entity);
                    entity.stopUsingItem();
                }
            }
        });
    }

    /**
     * The server applies the effects and has the final word on the stack. The local player's client
     * also takes one off right away, as vanilla does for food: the use ending and the count dropping
     * then reach the animation together, instead of as two updates from the server whose order and
     * timing vary.
     */
    private ItemStack finishConsumableUse(ItemStack stack, Level level, LivingEntity entity) {
        this.getConsumableIndex(stack).ifPresent(index -> {
            if (level.isClientSide()) {
                if (isLocalPlayer(entity) && !((Player) entity).getAbilities().instabuild) {
                    predictConsumption(stack, index);
                }
            } else {
                applyEffects(entity, stack, index);
                if (entity instanceof Player player) {
                    player.awardStat(Stats.ITEM_USED.get(this));
                    if (!player.getAbilities().instabuild) {
                        consumeAfterUse(stack, entity, index);
                    }
                } else {
                    consumeAfterUse(stack, entity, index);
                }
            }
        });
        return stack;
    }

    /** What consumeAfterUse will do on the server; hurtAndBreak does nothing on the client. */
    private static void predictConsumption(ItemStack stack, ConsumableIndex index) {
        ConsumableData data = index.getData();
        if (!data.hasDurability()) {
            stack.shrink(1);
            return;
        }
        int damage = stack.getDamageValue() + data.getDurabilityDamage();
        if (damage >= stack.getMaxDamage()) {
            stack.shrink(1);
        } else {
            stack.setDamageValue(damage);
        }
    }

    private static boolean isLocalPlayer(LivingEntity entity) {
        return entity instanceof Player player && player.isLocalPlayer();
    }

    private void consumeAfterUse(ItemStack stack, LivingEntity entity, ConsumableIndex index) {
        ConsumableData data = index.getData();
        if (data.hasDurability()) {
            stack.hurtAndBreak(data.getDurabilityDamage(), entity, (living) -> {
                living.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            });
        } else {
            stack.shrink(1);
        }
    }

    private void removeEffect(LivingEntity entity, ConsumableData.RemoveEffectSelector selector) {
        if (selector.isCategory()) {
            removeEffectsByCategory(entity, selector.getCategory());
            return;
        }

        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(selector.getEffect());
        if (effect != null) {
            entity.removeEffect(effect);
        }
    }

    private void removeEffectsByCategory(LivingEntity entity, MobEffectCategory category) {
        var immune = ForgeRegistries.MOB_EFFECTS.tags().getTag(CATEGORY_REMOVAL_IMMUNE);
        List<MobEffect> effects = entity.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .filter(effect -> effect.getCategory() == category)
                .filter(effect -> !immune.contains(effect))
                .toList();

        for (MobEffect effect : effects) {
            entity.removeEffect(effect);
        }
    }

    private void applyEffects(LivingEntity entity, ItemStack stack, ConsumableIndex index) {
        ConsumableData data = index.getData();
        if (data.getHeal() > 0f) {
            entity.heal(data.getHeal());
        }

        for (ConsumableData.RemoveEffectSelector selector : data.getRemoveEffects()) {
            removeEffect(entity, selector);
        }

        for (ConsumableData.EffectData effectData : data.getEffects()) {
            if (entity.getRandom().nextFloat() > effectData.getChance()) {
                continue;
            }
            MobEffectInstance effect = effectData.createInstance();
            if (effect != null) {
                entity.addEffect(effect);
            }
        }

        if (entity instanceof Player player && (data.getFood() > 0 || data.getSaturation() > 0)) {
            FoodData foodData = player.getFoodData();
            foodData.eat(data.getFood(), data.getSaturation());
        }

        MinecraftForge.EVENT_BUS.post(new ConsumableUseEvent(entity, stack.copy(), index.getId(), index));

        ResourceLocation cooldownId = data.getCooldownCategory();
        if (cooldownId != null && data.getCooldown() > 0) {
            entity.getCapability(CustomItemCoolDownsProvider.CAPABILITY).ifPresent(cap -> cap.addCooldown(cooldownId, data.getCooldown()));
        }
    }

    @Override
    public boolean isSame(ItemStack stack1, ItemStack stack2) {
        return IConsumable.super.isSame(stack1, stack2);
    }

    /**
     * Every consumable is this one item, told apart by NBT, and Forge's default only compares the
     * item: switching to another consumable mid-use would carry on the use with the new one and
     * finish it early. The count or durability changing keeps the same consumable, so that goes on.
     */
    @Override
    public boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
        return isSame(oldStack, newStack);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (this.getConsumableIndex(stack).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ConsumableTooltip(stack));
    }
}
