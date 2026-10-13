package decade.items.client;

import java.util.HashMap;
import java.util.Map;

import decade.items.DecadeItems;
import decade.items.entity.LureEntity;
import me.xjqsh.lrtactical.client.renderer.entity.ThrowableEntityRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client only: our throwables are drawn by lrtactical's own throwable renderer, as its grenades are; stalls by {@link StallModel}. */
@Mod.EventBusSubscriber(modid = DecadeItems.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ItemsClient {
    /** Where the stall's parts are: models/stall/ of the decade namespace, shipped in the client jar. */
    private static final String STALL_PARTS = "models/stall";

    private ItemsClient() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(LureEntity.TYPE, ThrowableEntityRenderer::new);
    }

    /** No block state names the parts, so each is baked on its own: every model under decade:stall/. */
    @SubscribeEvent
    public static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        Minecraft.getInstance().getResourceManager()
                .listResources(STALL_PARTS, rl -> rl.getNamespace().equals(DecadeItems.MOD_ID) && rl.getPath().endsWith(".json"))
                .keySet().forEach(rl -> event.register(partName(rl)));
    }

    /** Every block state of the stall draws through {@link StallModel}, which picks the parts by the block entity's look. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        Map<ResourceLocation, BakedModel> parts = new HashMap<>();
        models.forEach((rl, model) -> {
            if (!(rl instanceof ModelResourceLocation) && rl.getNamespace().equals(DecadeItems.MOD_ID)
                    && rl.getPath().startsWith("stall/")) {
                parts.put(rl, model);
            }
        });
        for (ResourceLocation rl : models.keySet().toArray(ResourceLocation[]::new)) {
            if (rl instanceof ModelResourceLocation mrl && mrl.getNamespace().equals(DecadeItems.MOD_ID)
                    && mrl.getPath().equals("stall") && !mrl.getVariant().equals("inventory")) {
                models.put(rl, new StallModel(models.get(rl), parts));
            }
        }
    }

    /** decade:models/stall/roof/white/red.json → decade:stall/roof/white/red */
    private static ResourceLocation partName(ResourceLocation file) {
        String path = file.getPath();
        return ResourceLocation.fromNamespaceAndPath(file.getNamespace(),
                path.substring("models/".length(), path.length() - ".json".length()));
    }
}
