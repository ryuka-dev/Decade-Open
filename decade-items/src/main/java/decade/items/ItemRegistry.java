package decade.items;

import decade.items.block.FixtureBlock;
import decade.items.block.SalvageBenchBlock;
import decade.items.block.SalvageBenchHalfBlock;
import decade.items.block.StallBlock;
import decade.items.block.StallBlockEntity;
import decade.items.block.StallFillerBlock;
import decade.items.entity.LureEntity;
import decade.items.item.PackConsumable;
import decade.items.item.PackThrowable;
import me.xjqsh.lrtactical.init.ModRegistries;
import me.xjqsh.lrtactical.item.throwable.ThrowableData;
import me.xjqsh.lrtactical.item.throwable.ThrowableType;
import net.minecraft.world.entity.EntityType;
import decade.items.item.StallTarp;
import decade.items.item.SurveyWand;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Our items, and the one creative tab that lists all of them. */
public final class ItemRegistry {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DecadeItems.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DecadeItems.MOD_ID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DecadeItems.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DecadeItems.MOD_ID);
    private static final DeferredRegister<ThrowableType<?, ?>> THROWABLE_TYPES = DeferredRegister.create(ModRegistries.THROWABLE_TYPE, DecadeItems.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DecadeItems.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DecadeItems.MOD_ID);

    /** Its look and use come from content/tacz/decade: data/decade/index/consumable/armor_plate.json. */
    public static final RegistryObject<Item> ARMOR_PLATE = ITEMS.register("armor_plate", PackConsumable::new);

    /**
     * The only cure for infection, AI-2's box and needle. Its look and use come from content/tacz/decade:
     * data/decade/index/consumable/cure.json; what it does is decade_combat's on the server, by this registry name.
     */
    public static final RegistryObject<Item> CURE = ITEMS.register("cure", PackConsumable::new);

    /**
     * The disguise injector, Survivor's Arsenal's injector moved over to lrtactical. Its look and use come from
     * content/tacz/decade: data/decade/index/consumable/disguise.json; what it does is decade_combat's on the server.
     */
    public static final RegistryObject<Item> DISGUISE = ITEMS.register("disguise", PackConsumable::new);

    /**
     * The prevention injector, the medic's first work: Survivor's Arsenal's adrenaline injector moved over to
     * lrtactical. A work, so never stacked, each with its own number and maker; how long it keeps infection off by its
     * quality is decade_combat's on the server.
     */
    public static final RegistryObject<Item> PREVENTION = ITEMS.register("prevention", PackConsumable::new);

    /**
     * What the disguise injector leaves on a player while it lasts: a name and an icon only. How much closer a zombie has
     * to come before it notices is decade_combat's, read from the server's datapack.
     */
    /**
     * The lure: thrown, it lands and lies there; decade_noise draws wandering zombies to it on the server. Its
     * look and flight come from content/tacz/decade: data/decade/index/throwable/lure.json, of the type below.
     */
    public static final RegistryObject<Item> LURE = ITEMS.register("lure", PackThrowable::new);
    public static final RegistryObject<EntityType<LureEntity>> LURE_ENTITY = ENTITY_TYPES.register("lure", () -> LureEntity.TYPE);
    public static final RegistryObject<ThrowableType<ThrowableData, LureEntity>> LURE_TYPE = THROWABLE_TYPES.register("lure",
            () -> LureEntity.LURE);

    public static final RegistryObject<MobEffect> DISGUISED = EFFECTS.register("disguised",
            () -> new ShellEffect(MobEffectCategory.BENEFICIAL, 0x4F6B3A));
    /** Stress, after being hit: a server mod gives it and reads it; milk does not clear it (see StressEffect). */
    public static final RegistryObject<MobEffect> STRESS = EFFECTS.register("stress", () -> new StressEffect(0x8A2E2E));

    /** Admin tool; its clicks are handled by decade_ruins on the server, by this registry name. */
    public static final RegistryObject<Item> RUIN_WAND = ITEMS.register("ruin_wand", SurveyWand::new);

    // Crafting parts: plain items that only recipes use.
    // Civil parts come mostly from outer ruins, military parts from the dead zone and airdrops, the three
    // key parts only from scavenging. Which recipe takes how many is the server's datapack, not this jar.
    public static final RegistryObject<Item> CIVIL_PARTS = ITEMS.register("civil_parts", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MILITARY_PARTS = ITEMS.register("military_parts", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> OPTICS = ITEMS.register("optics", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PRECISION_SPRING = ITEMS.register("precision_spring", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RIFLED_BARREL = ITEMS.register("rifled_barrel", () -> new Item(new Item.Properties()));
    /** Made from civil parts and taken by regular ammunition, so rounds cost a little scavenging too. */
    public static final RegistryObject<Item> PRIMER = ITEMS.register("primer", () -> new Item(new Item.Properties()));

    /**
     * What a nest's core leaves when it is taken out: only nests give it, the recycling station buys it, later a
     * material for medical works. A plain item; who gets one and what it is worth is the server's (decade_director,
     * the decade-balance datapack).
     */
    public static final RegistryObject<Item> NEST_CORE = ITEMS.register("nest_core", () -> new Item(new Item.Properties()));

    /**
     * The specialty registration desk in Decade City: a public fixture, so nobody breaks it in survival
     * (admins place and remove it). Right-clicking it is handled by decade_specialty by this name.
     */
    public static final RegistryObject<Block> SPECIALTY_DESK = BLOCKS.register("specialty_desk", () -> new FixtureBlock(
            Block.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(-1.0F, 3_600_000.0F)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Item> SPECIALTY_DESK_ITEM = ITEMS.register("specialty_desk",
            () -> new BlockItem(SPECIALTY_DESK.get(), new Item.Properties()));

    /**
     * The workstation (制作站): civilian-tier works and consumables, made by a player and placed where they like,
     * so it breaks like a crafting table and drops itself. What it does is decade_crafting's, by this name.
     */
    public static final RegistryObject<Block> WORKSTATION = BLOCKS.register("workstation", () -> new FixtureBlock(
            Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(2.5F, 6.0F).noOcclusion()));
    public static final RegistryObject<Item> WORKSTATION_ITEM = ITEMS.register("workstation",
            () -> new BlockItem(WORKSTATION.get(), new Item.Properties()));

    /**
     * The armory (军火台): military-tier works. In phase 0 a public fixture in Decade City, so nobody
     * breaks it in survival; with territories it becomes a faction's asset.
     */
    public static final RegistryObject<Block> ARMORY = BLOCKS.register("armory", () -> new FixtureBlock(
            Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(-1.0F, 3_600_000.0F)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Item> ARMORY_ITEM = ITEMS.register("armory",
            () -> new BlockItem(ARMORY.get(), new Item.Properties()));

    /**
     * The public salvage bench in Decade City: a fixture like the armory, admins place and remove it. It wears
     * Survivor's Arsenal's recycler model, two blocks wide, so it comes with an invisible second half. Each player who
     * opens it sees their own; what it takes apart, how long it takes and what it costs are decade_economy's, by this name.
     */
    public static final RegistryObject<Block> SALVAGE_BENCH = BLOCKS.register("salvage_bench", () -> new SalvageBenchBlock(
            Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(-1.0F, 3_600_000.0F)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Item> SALVAGE_BENCH_ITEM = ITEMS.register("salvage_bench",
            () -> new BlockItem(SALVAGE_BENCH.get(), new Item.Properties()));
    /** The salvage bench's invisible second half; no item, it comes with the bench. */
    public static final RegistryObject<Block> SALVAGE_BENCH_HALF = BLOCKS.register("salvage_bench_half",
            () -> new SalvageBenchHalfBlock(Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
                    .strength(-1.0F, 3_600_000.0F).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));

    /**
     * The recycling station in Decade City: a fixture wearing Survivor's Arsenal's cash register. Selling to it is
     * decade_economy's, by this name; only admins place it, so the block itself says where the station is.
     */
    public static final RegistryObject<Block> RECYCLE_STATION = BLOCKS.register("recycle_station", () -> new FixtureBlock(
            Block.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(-1.0F, 3_600_000.0F)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK),
            Block.box(2, 0, 2, 14, 13, 14)));
    public static final RegistryObject<Item> RECYCLE_STATION_ITEM = ITEMS.register("recycle_station",
            () -> new BlockItem(RECYCLE_STATION.get(), new Item.Properties()));

    /**
     * A stall in Decade City: a 3×2 booth drawn from the parts its block entity names, the
     * parts being models in the client jar. A public fixture like the armory, admins place and remove it; claiming,
     * selling and what it may look like are decade_economy's, by this name.
     */
    public static final RegistryObject<Block> STALL = BLOCKS.register("stall", () -> new StallBlock(
            Block.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(-1.0F, 3_600_000.0F)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Item> STALL_ITEM = ITEMS.register("stall",
            () -> new BlockItem(STALL.get(), new Item.Properties()));
    /** The other five cells of a stall: invisible and solid. No item; it comes and goes with the stall. */
    public static final RegistryObject<Block> STALL_FILLER = BLOCKS.register("stall_filler", () -> new StallFillerBlock(
            Block.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(-1.0F, 3_600_000.0F)
                    .noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<StallBlockEntity>> STALL_ENTITY = BLOCK_ENTITIES.register("stall",
            () -> BlockEntityType.Builder.of(StallBlockEntity::new, STALL.get()).build(null));

    /**
     * The four tarps: found in ruins and airdrops, used up to unlock that pattern for one's stall roof.
     * The names follow the patterns in content/stall/styles.tsv (kind pattern, tier tarp); a new tarp pattern needs a
     * line here as well. What using one does is decade_economy's.
     */
    public static final RegistryObject<Item> TARP_TARP = ITEMS.register("tarp_tarp", StallTarp::new);
    public static final RegistryObject<Item> TARP_WOODLAND = ITEMS.register("tarp_woodland", StallTarp::new);
    public static final RegistryObject<Item> TARP_PATCHWORK = ITEMS.register("tarp_patchwork", StallTarp::new);
    public static final RegistryObject<Item> TARP_HAZARD = ITEMS.register("tarp_hazard", StallTarp::new);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.decade"))
            .icon(() -> ARMOR_PLATE.get().getDefaultInstance())
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get().getDefaultInstance())))
            .build());

    private ItemRegistry() {
    }

    /** A status effect that does nothing by itself: the server mod that gives it decides what it means. */
    private static final class ShellEffect extends MobEffect {
        ShellEffect(MobEffectCategory category, int colour) {
            super(category, colour);
        }
    }

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ITEMS.register(modBus);
        EFFECTS.register(modBus);
        ENTITY_TYPES.register(modBus);
        THROWABLE_TYPES.register(modBus);
        TABS.register(modBus);
    }
}
