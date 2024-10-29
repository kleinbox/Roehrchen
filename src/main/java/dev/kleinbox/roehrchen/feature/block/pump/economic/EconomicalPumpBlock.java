package dev.kleinbox.roehrchen.feature.block.pump.economic;

import dev.kleinbox.roehrchen.core.transaction.tracker.TransactionTracker;
import dev.kleinbox.roehrchen.core.transaction.ItemTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class EconomicalPumpBlock extends Block {
    public static final Properties PROPERTIES = Properties.of()
            .noOcclusion()
            .isValidSpawn(Blocks::never)
            .isRedstoneConductor((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);

    public static final int COOLDOWN = 20 * 5;

    public EconomicalPumpBlock() {
        super(PROPERTIES);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Override
    protected void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos,
                        @NotNull RandomSource random) {
        super.tick(state, level, pos, random);

        Direction front = state.getValue(BlockStateProperties.FACING);
        Direction back = front.getOpposite();

        IItemHandler capability = level.getCapability(Capabilities.ItemHandler.BLOCK, pos.relative(back), back);
        if (capability != null) {
            ItemStack item = ItemStack.EMPTY;

            for (int slot = 0; slot < capability.getSlots(); slot++) {
                if (item.isEmpty()) {
                    item = capability.extractItem(slot, item.getMaxStackSize(), false);
                    continue;
                }

                if (item.getCount() >= item.getMaxStackSize())
                    break;

                ItemStack stackInSlot = capability.getStackInSlot(slot);

                if (stackInSlot.isEmpty())
                    continue;

                if (ItemStack.isSameItem(item, stackInSlot)) {
                    ItemStack result = capability.extractItem(slot, item.getMaxStackSize() - item.getCount(), false);
                    item.setCount(item.getCount() + result.getCount());
                }
            }

            if (!item.isEmpty())
                TransactionTracker.registerTransaction(level, new ItemTransaction(
                        item, front, pos, false
                ));
        }

        level.scheduleTick(pos, this, COOLDOWN);
    }

    @Override
    protected void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                           @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, COOLDOWN);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(BlockStateProperties.FACING, context.getClickedFace());
    }
}
