package dev.kleinbox.roehrchen.feature.block.pump.economic;

import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.api.TransactionHandler;
import dev.kleinbox.roehrchen.core.transaction.ItemTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class EconomicalPumpIntermediaryHandler implements TransactionHandler {

    private final Level level;

    private EconomicalPumpIntermediaryHandler(Level level) {
        this.level = level;
    }

    @Nullable
    public static EconomicalPumpIntermediaryHandler create(Level level, BlockPos blockPos, BlockState blockSate, @Nullable Direction side) {
        if (!(blockSate.getBlock() instanceof EconomicalPumpBlock))
            return null;

        // If a direction is specified, it must be correct;
        // However, we also take non-sides for cases like the TransactionTracker
        if (side != null && side != blockSate.getValue(BlockStateProperties.FACING))
            return null;

        return new EconomicalPumpIntermediaryHandler(level);
    }

    @Override
    public boolean request(Transaction<?, ?> transaction) {
        return transaction instanceof ItemTransaction;
    }

    @Override
    public @Nullable Direction next(Transaction<?, ?> transaction) {
        BlockState blockState = level.getBlockState(transaction.blockPos);

        if (blockState.getBlock() instanceof EconomicalPumpBlock)
            return blockState.getValue(BlockStateProperties.FACING);

        return null;
    }
}
