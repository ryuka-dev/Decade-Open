package com.tacz.guns.config.util;

import com.google.common.collect.Lists;
import com.tacz.guns.GunMod;
import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class InteractKeyConfigRead {
    private static final EnumMap<Type, List<ResourceLocation>> WHITELIST = new EnumMap<>(Type.class);
    private static final EnumMap<Type, List<ResourceLocation>> BLACKLIST = new EnumMap<>(Type.class);
    private static final TagKey<Block> WHITELIST_BLOCKS = BlockTags.create(new ResourceLocation(GunMod.MOD_ID, "interact_key/whitelist"));
    private static final TagKey<Block> BLACKLIST_BLOCKS = BlockTags.create(new ResourceLocation(GunMod.MOD_ID, "interact_key/blacklist"));
    private static final TagKey<EntityType<?>> WHITELIST_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(GunMod.MOD_ID, "interact_key/whitelist"));
    private static final TagKey<EntityType<?>> BLACKLIST_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(GunMod.MOD_ID, "interact_key/blacklist"));

    // Decade: rules other mods register by inter-mod message (InteractRuleMessages): a block by where it is, an entity by
    // what it is. Asked after the blacklists and before the whitelists, so a blacklist still wins
    private static final List<BiPredicate<BlockPos, BlockState>> BLOCK_RULES = new CopyOnWriteArrayList<>();
    private static final List<Predicate<Entity>> ENTITY_RULES = new CopyOnWriteArrayList<>();
    // Decade: vetoes, asked right after the blacklists: a block or an entity a veto names is never reached, whatever the
    // rules and the whitelists say (a ruin's furnace the server refuses there, decade.17)
    private static final List<BiPredicate<BlockPos, BlockState>> BLOCK_VETOES = new CopyOnWriteArrayList<>();
    private static final List<Predicate<Entity>> ENTITY_VETOES = new CopyOnWriteArrayList<>();

    // Decade
    public static void addBlockRule(BiPredicate<BlockPos, BlockState> rule) {
        BLOCK_RULES.add(rule);
    }

    // Decade
    public static void addBlockVeto(BiPredicate<BlockPos, BlockState> veto) {
        BLOCK_VETOES.add(veto);
    }

    // Decade
    public static void addEntityVeto(Predicate<Entity> veto) {
        ENTITY_VETOES.add(veto);
    }

    // Decade
    public static void addEntityRule(Predicate<Entity> rule) {
        ENTITY_RULES.add(rule);
    }

    // Decade: the block where it is, so a rule can tell a marked container from the same block elsewhere
    public static boolean canInteractBlock(BlockPos pos, BlockState block) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        if (blockId == null || BLACKLIST.containsKey(Type.BLOCK) && BLACKLIST.get(Type.BLOCK).contains(blockId)
                || block.is(BLACKLIST_BLOCKS)) {
            return false;
        }
        for (BiPredicate<BlockPos, BlockState> veto : BLOCK_VETOES) {
            if (veto.test(pos, block)) {
                return false;
            }
        }
        for (BiPredicate<BlockPos, BlockState> rule : BLOCK_RULES) {
            if (rule.test(pos, block)) {
                return true;
            }
        }
        return canInteractBlock(block);
    }

    public static void init() {
        WHITELIST.clear();
        BLACKLIST.clear();
        handleConfigData(SyncConfig.INTERACT_KEY_WHITELIST_BLOCKS.get(), WHITELIST, Type.BLOCK);
        handleConfigData(SyncConfig.INTERACT_KEY_WHITELIST_ENTITIES.get(), WHITELIST, Type.ENTITY);
        handleConfigData(SyncConfig.INTERACT_KEY_BLACKLIST_BLOCKS.get(), BLACKLIST, Type.BLOCK);
        handleConfigData(SyncConfig.INTERACT_KEY_BLACKLIST_ENTITIES.get(), BLACKLIST, Type.ENTITY);
    }

    public static boolean canInteractBlock(BlockState block) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block.getBlock());
        if (blockId == null) {
            return false;
        }
        // 先检查黑名单
        if (BLACKLIST.containsKey(Type.BLOCK) && BLACKLIST.get(Type.BLOCK).contains(blockId)) {
            return false;
        }
        if (block.is(BLACKLIST_BLOCKS)) {
            return false;
        }
        // 再检查白名单
        if (WHITELIST.containsKey(Type.BLOCK) && WHITELIST.get(Type.BLOCK).contains(blockId)) {
            return true;
        }
        return block.is(WHITELIST_BLOCKS);
    }

    public static boolean canInteractEntity(Entity entity) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId == null) {
            return false;
        }
        // 先检查黑名单
        if (BLACKLIST.containsKey(Type.ENTITY) && BLACKLIST.get(Type.ENTITY).contains(entityId)) {
            return false;
        }
        if (entity.getType().is(BLACKLIST_ENTITIES)) {
            return false;
        }
        // Decade
        for (Predicate<Entity> veto : ENTITY_VETOES) {
            if (veto.test(entity)) {
                return false;
            }
        }
        for (Predicate<Entity> rule : ENTITY_RULES) {
            if (rule.test(entity)) {
                return true;
            }
        }
        // 再检查白名单
        if (WHITELIST.containsKey(Type.ENTITY) && WHITELIST.get(Type.ENTITY).contains(entityId)) {
            return true;
        }
        return entity.getType().is(WHITELIST_ENTITIES);
    }

    private static void handleConfigData(List<String> configData, EnumMap<Type, List<ResourceLocation>> storeList, Type type) {
        configData.forEach(data -> {
            if (data.isEmpty()) {
                return;
            }
            if (StringUtils.isBlank(data)) {
                return;
            }
            ResourceLocation id = new ResourceLocation(data);
            storeList.computeIfAbsent(type, t -> Lists.newArrayList()).add(id);
        });
    }

    public enum Type {
        BLOCK, ENTITY;
    }
}
