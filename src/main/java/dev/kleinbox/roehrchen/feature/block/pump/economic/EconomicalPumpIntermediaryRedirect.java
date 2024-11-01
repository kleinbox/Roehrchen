package dev.kleinbox.roehrchen.feature.block.pump.economic;

import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.api.TransactionRedirectHandler;
import dev.kleinbox.roehrchen.feature.transaction.ItemTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class EconomicalPumpIntermediaryRedirect implements TransactionRedirectHandler {

    private final Level level;
    private final BlockPos blockPos;

    private EconomicalPumpIntermediaryRedirect(Level level, BlockPos blockPos) {
        this.level = level;
        this.blockPos = blockPos;
    }

    @Nullable
    public static EconomicalPumpIntermediaryRedirect create(Level level, BlockPos blockPos, BlockState blockSate, @Nullable Direction side) {
        if (!(blockSate.getBlock() instanceof EconomicalPumpBlock))
            return null;

        // If a direction is specified, it must be correct;
        // However, we also take non-sides for cases like the TransactionTracker
        if (side != null && side != blockSate.getValue(BlockStateProperties.FACING))
            return null;

        return new EconomicalPumpIntermediaryRedirect(level, blockPos);
    }

    @Override
    public boolean request(Transaction<?, ?> transaction) {
        return transaction instanceof ItemTransaction;
    }

    @Override
    public @Nullable Direction next(Direction origin) {
        BlockState blockState = level.getBlockState(blockPos);

        if (blockState.getBlock() instanceof EconomicalPumpBlock)
            return blockState.getValue(BlockStateProperties.FACING);

        return null;
    }
}
