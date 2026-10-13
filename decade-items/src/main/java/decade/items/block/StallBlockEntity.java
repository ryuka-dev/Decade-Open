package decade.items.block;

import decade.items.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * Holds a stall's look. Written only by the server — decade_economy, through vanilla's block-entity tag, the
 * way {@code /data merge block} does — and sent to clients, which rebuild the chunk to show it. Purely what it looks
 * like: nothing about who holds the stall or what it sells is here (that is the economy log's).
 */
public class StallBlockEntity extends BlockEntity {
    public static final ModelProperty<StallLook> LOOK = new ModelProperty<>();

    private StallLook look = StallLook.DEFAULT;

    public StallBlockEntity(BlockPos pos, BlockState state) {
        super(ItemRegistry.STALL_ENTITY.get(), pos, state);
    }

    public StallLook look() {
        return look;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        StallLook before = look;
        look = StallLook.read(tag);
        if (!look.equals(before) && level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        look.write(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            load(packet.getTag());
        }
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(LOOK, look).build();
    }
}
