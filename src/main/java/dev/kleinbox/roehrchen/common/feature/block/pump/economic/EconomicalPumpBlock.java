package dev.kleinbox.roehrchen.common.feature.block.pump.economic;

import dev.kleinbox.roehrchen.api.RoehrchenRegistries;
import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.common.core.tracker.TransactionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

        if (level.isClientSide)
            return;

        Direction back = state.getValue(BlockStateProperties.FACING).getOpposite();

        for (Transaction<?, ?> singleton : RoehrchenRegistries.TRANSACTION_REGISTRY) {
            Transaction<?, ?> transaction = singleton.extractFrom(level, pos, back);
            if (transaction != null)
                TransactionTracker.registerTransaction(level, transaction);
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
                .setValue(BlockStateProperties.FACING, context.getNearestLookingDirection().getOpposite());
    }
}
