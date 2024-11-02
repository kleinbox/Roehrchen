package dev.kleinbox.roehrchen.common.feature.block.distribution.merger;

import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.api.TransactionRedirectHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class MergerTransactionRedirect implements TransactionRedirectHandler {

    private final Level level;
    private final BlockPos blockPos;

    private MergerTransactionRedirect(Level level, BlockPos blockPos) {
        this.level = level;
        this.blockPos = blockPos;
    }

    @Nullable
    public static MergerTransactionRedirect create(Level level, BlockPos blockPos, BlockState blockSate, @Nullable Direction side) {
        if (!(blockSate.getBlock() instanceof MergerBlock))
            return null;

        if (side == Direction.UP || side == Direction.DOWN)
            return null;

        return new MergerTransactionRedirect(level, blockPos);
    }

    @Override
    public boolean request(Transaction<?, ?> transaction) {
        BlockState blockState = level.getBlockState(transaction.blockPos);
        Direction outputDir = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);

        // Do not take inputs from output direction
        return transaction.origin != outputDir;
    }

    @Override
    public @Nullable Direction next(Direction origin) {
        BlockState blockState = level.getBlockState(blockPos);

        return blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }
}
