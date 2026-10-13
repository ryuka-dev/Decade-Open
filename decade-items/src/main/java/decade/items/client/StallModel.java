package decade.items.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.math.Transformation;
import decade.items.block.StallBlockEntity;
import decade.items.block.StallLook;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.QuadTransformers;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Draws a whole stall from its parts: the roof, the furniture set and the carpet its block
 * entity's {@link StallLook} names, each a plain block model under {@code decade:stall/…} shipped in the client jar.
 * Baked into the chunk like any block, not drawn every frame; the chunk is rebuilt when the look changes.
 *
 * <p>Why not one block state per combination: roofs × colours × sets × carpets × colours × board colours × facings is
 * well over a hundred thousand states. The parts are modelled as in Blockbench, the carpet one block below the model's
 * origin (block models can only reach −16..32), so they are lifted one block here and turned to the stall's facing.
 */
public final class StallModel implements IDynamicBakedModel {
    private static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout());
    /**
     * Faces that look down (the roof cloth seen from under it). Embeddium lights a face lying flat on the block grid from
     * the block on the side it faces: for a face looking down that is the ground under the stall block, solid and dark,
     * so the underside of the roof came out black (2026-10-13; a tilted face is lit from the stall's own cell and was
     * fine). So such a face is given the stall's front as its side, open air, and darkened by its vertex colour from the
     * front's shade (0.8) to vanilla's underside shade (0.5). Its normal stays down, for shader packs.
     */
    private static final IQuadTransformer UNDERSIDE = QuadTransformers.applyingColor(160, 160, 160);

    private final BakedModel base;
    private final Map<ResourceLocation, BakedModel> parts;
    private final Map<Key, List<BakedQuad>> cache = new ConcurrentHashMap<>();

    private record Key(StallLook look, Direction facing) {
    }

    /** {@code parts}: every model under decade:stall/, by its location; {@code base} gives the particle texture. */
    public StallModel(BakedModel base, Map<ResourceLocation, BakedModel> parts) {
        this.base = base;
        this.parts = parts;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
                                             @NotNull ModelData data, @Nullable RenderType renderType) {
        if (side != null || (renderType != null && renderType != RenderType.cutout())) {
            return Collections.emptyList();
        }
        StallLook look = data.get(StallBlockEntity.LOOK);
        if (look == null) {
            look = StallLook.DEFAULT;
        }
        Direction facing = state != null && state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return cache.computeIfAbsent(new Key(look, facing), k -> compose(k.look, k.facing, rand));
    }

    private List<BakedQuad> compose(StallLook look, Direction facing, RandomSource rand) {
        List<BakedModel> models = new ArrayList<>();
        add(models, StallLook.BARE.equals(look.cloth())
                ? part("shape/roof_" + look.roof() + "_bare") : part("roof/" + look.roof() + "/" + look.cloth()),
                part("roof/" + StallLook.DEFAULT.roof() + "/white"));
        add(models, part("set/" + look.set() + "/" + look.board()), part("set/" + look.set() + "/none"),
                part("set/" + StallLook.DEFAULT.set() + "/none"));
        if (!StallLook.NONE.equals(look.carpet())) {
            add(models, part("carpet/" + look.carpet() + "/" + look.rug()));
        }
        IQuadTransformer move = QuadTransformers.applying(new Transformation(placement(facing)));
        int steps = Math.floorMod(facing.get2DDataValue() - Direction.NORTH.get2DDataValue(), 4);
        List<BakedQuad> out = new ArrayList<>();
        for (BakedModel model : models) {
            for (BakedQuad quad : model.getQuads(null, null, rand, ModelData.EMPTY, null)) {
                BakedQuad moved = move.process(quad);
                Direction d = turn(moved.getDirection(), steps);
                if (d == Direction.DOWN) {
                    // see UNDERSIDE: lit from the open front, darkened by hand to vanilla's underside shade
                    moved = UNDERSIDE.process(moved);
                    d = facing;
                }
                out.add(new BakedQuad(moved.getVertices(), moved.getTintIndex(), d, moved.getSprite(), moved.isShade()));
            }
        }
        return List.copyOf(out);
    }

    /** Lift the parts one block, then turn them about the block's centre so the model's front (north) faces {@code facing}. */
    static Matrix4f placement(Direction facing) {
        float angle = (float) Math.toRadians(180.0F - facing.toYRot());
        return new Matrix4f().translate(0.5F, 0.0F, 0.5F).rotateY(angle).translate(-0.5F, 1.0F, -0.5F);
    }

    /** The quad's own facing turns with it, so the game shades the face as the side it now is. */
    static Direction turn(Direction direction, int steps) {
        if (direction.getAxis().isVertical() || steps == 0) {
            return direction;
        }
        return Direction.from2DDataValue(direction.get2DDataValue() + steps);
    }

    private BakedModel part(String path) {
        return parts.get(ResourceLocation.fromNamespaceAndPath("decade", "stall/" + path));
    }

    /** The first of the candidates that exists: a name this jar does not know falls back to a default part. */
    private static void add(List<BakedModel> into, BakedModel... candidates) {
        for (BakedModel m : candidates) {
            if (m != null) {
                into.add(m);
                return;
            }
        }
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return CUTOUT;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false; // the parts reach two blocks out; ambient occlusion sampled at one block shades them wrongly
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation") // abstract in BakedModel; Forge prefers the ModelData one below
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return base.getParticleIcon();
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return base.getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
