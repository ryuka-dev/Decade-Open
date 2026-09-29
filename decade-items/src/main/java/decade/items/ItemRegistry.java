package decade.items;

import decade.items.item.PackConsumable;
import decade.items.item.SurveyWand;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Our items, and the one creative tab that lists all of them. */
public final class ItemRegistry {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, DecadeItems.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DecadeItems.MOD_ID);

    /** Its look and use come from content/tacz/decade: data/decade/index/consumable/armor_plate.json. */
    public static final RegistryObject<Item> ARMOR_PLATE = ITEMS.register("armor_plate", PackConsumable::new);

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

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.decade"))
            .icon(() -> ARMOR_PLATE.get().getDefaultInstance())
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(item -> output.accept(item.get().getDefaultInstance())))
            .build());

    private ItemRegistry() {
    }

    static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
