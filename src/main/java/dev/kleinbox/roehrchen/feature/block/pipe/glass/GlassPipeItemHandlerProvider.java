package dev.kleinbox.roehrchen.feature.block.pipe.glass;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Glass Pipes have infinite amount of storage, in theory.
 * Because of that, we will just pretend that the first slot is always empty.
 */
public class GlassPipeItemHandlerProvider implements IItemHandler {
    private static final int EMPTY_SLOT = 0;
    private static final int INDEX_OFFSET = 1;

    private final GlassPipeBlockEntity blockEntity;
    private final Direction side;

    private GlassPipeItemHandlerProvider(GlassPipeBlockEntity blockEntity, Direction side) {
        this.blockEntity = blockEntity;
        this.side = side;
    }

    @Nullable
    public static GlassPipeItemHandlerProvider requestCapability(GlassPipeBlockEntity blockEntity, Direction side) {
        BlockState blockState = blockEntity.getBlockState();
        Block block = blockState.getBlock();

        if (!(block instanceof GlassPipeBlock glassPipeBlock))
            return null;

        // Must be facing the pipe correctly
        Pair<Direction, Direction> connectors = glassPipeBlock.getConnectors(blockState);
        if (side == connectors.getFirst() || side == connectors.getSecond())
            return new GlassPipeItemHandlerProvider(blockEntity, side);

        return null;
    }

    @Override
    public int getSlots() {
        // Used slots + one empty slot
        return blockEntity.transactions.size() + INDEX_OFFSET;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        // Out of bounds (or last slot)
        if (slot <= EMPTY_SLOT || slot > getSlots())
            return ItemStack.EMPTY;

        return blockEntity.transactions.get(slot-INDEX_OFFSET).getProduct();
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack itemStack, boolean simulate) {
        // We do not modify existing transactions
        if (slot != EMPTY_SLOT)
            return itemStack;

        // First slot
        if (!simulate)
            blockEntity.addTransaction(new TravelingItem(itemStack, side));

        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stackInSlot = getStackInSlot(slot);

        if (amount < stackInSlot.getCount()) {
            // So remember when I said we do not modify transaction?
            // Uh, yeah... To ensure compatibility, I guess we make a small exception here.
            if (!simulate)
                stackInSlot.setCount(stackInSlot.getCount()-amount);

            return stackInSlot.copyWithCount(amount);
        }

        if (!simulate)
            blockEntity.transactions.remove(slot-INDEX_OFFSET);

        return stackInSlot;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == EMPTY_SLOT ? getStackInSlot(slot).getMaxStackSize() : 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack itemStack) {
        return true;
    }
}
