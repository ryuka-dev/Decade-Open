package decade.items;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Decade's own items and their creative tab. Every item we add lives here, under the "decade"
 * namespace, in our own tab, even when it borrows another mod's machinery. Shells only: what an
 * item does is decided by server-side mods, so nothing here is a rule.
 */
@Mod(DecadeItems.MOD_ID)
public final class DecadeItems {
    public static final String MOD_ID = "decade";

    public DecadeItems(FMLJavaModLoadingContext context) {
        ItemRegistry.register(context.getModEventBus());
    }
}
