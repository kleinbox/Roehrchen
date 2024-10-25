package dev.kleinbox.roehrchen.feature.block.pipe.glass;

import dev.kleinbox.roehrchen.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;

import static dev.kleinbox.roehrchen.Initialization.LOGGER;


public class GlassPipeBlockEntity extends BlockEntity {
    public LinkedList<TravelingItem> transactions = new LinkedList<>();
    private int dataHash = transactions.hashCode();

    public GlassPipeBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registries.GLASS_PIPE_BLOCK_ENTITY.get(), pos, blockState);
    }

    /**
     * Used to add a transaction to the glass pipe network.
     * Only this method should be used instead of manually adding to the transactions list.
     *
     * @param transaction The new transaction to add.
     */
    public void addTransaction(TravelingItem transaction) {
        transactions.add(transaction);

        if (level == null || level.isClientSide())
            return;

        BlockPos blockPos = this.getBlockPos();
        GlassPipeNetwork.makeChunkAwareOfPipe(level, blockPos);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (level == null || level.isClientSide())
            return;

        GlassPipeNetwork.unwatchBlock(level, this.getBlockPos());

        // Todo: Drop products
    }

    // TODO: Invalidate capabilities on death
    //       also ensure that the chunk does not still try to visit us... I think?
    //       In theory we could just do that in tick actually...
    // https://docs.neoforged.net/docs/datastorage/capabilities#block-capability-invalidation

    // TODO: Make pipes movable, even with block entities.
    // https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/mixins/PistonBaseBlock_movableBEMixin.java

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        CompoundTag list = new CompoundTag();
        for (TravelingItem transaction : transactions)
            list.put(String.valueOf(transaction.hashCode()), transaction.save(registries));

        tag.put("transactions", list);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        CompoundTag list = tag.getCompound("transactions");
        transactions = new LinkedList<>();

        for (String hash : list.getAllKeys()) {
            CompoundTag data = list.getCompound(hash);
            TravelingItem transaction = TravelingItem.load(data, registries);

            if (transaction == null) {
                LOGGER.warn("Some transactions of the pipe at {} have gotten lost for unknown reasons",
                        this.getBlockPos());
                continue;
            }

            if (!Objects.equals(String.valueOf(transaction), hash))
                LOGGER.warn("Data of pipe at {} seems to have invalid sections (Some hashes do not match).",
                        this.getBlockPos());

            transactions.add(transaction);
        }
    }
}
