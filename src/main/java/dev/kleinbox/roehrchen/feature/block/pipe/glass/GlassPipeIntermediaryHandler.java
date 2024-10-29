package dev.kleinbox.roehrchen.feature.block.pipe.glass;

import com.mojang.datafixers.util.Pair;
import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.api.TransactionHandler;
import dev.kleinbox.roehrchen.core.tracker.transaction.ItemTransaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GlassPipeIntermediaryHandler implements TransactionHandler {

    private final Level level;

    private GlassPipeIntermediaryHandler(Level level) {
        this.level = level;
    }

    @Nullable
    public static GlassPipeIntermediaryHandler create(Level level, BlockPos blockPos, BlockState blockSate, @Nullable Direction side) {
        if (!(blockSate.getBlock() instanceof GlassPipeBlock block))
            return null;

        // If a direction is specified, it must be correct;
        // However, we also take non-sides for cases like the TransactionTracker
        Pair<Direction, Direction> connectors = block.getConnectors(blockSate);
        if (side != null && !(connectors.getFirst() == side || connectors.getSecond() == side))
            return null;

        return new GlassPipeIntermediaryHandler(level);
    }

    @Override
    public boolean request(Transaction<?, ?> transaction) {
        return transaction instanceof ItemTransaction;
    }

    @Override
    public Direction next(Transaction<?, ?> transaction) {
        BlockPos blockPos = transaction.blockPos;
        BlockState blockState = level.getBlockState(blockPos);
        GlassPipeBlock block = (GlassPipeBlock) blockState.getBlock();

        Pair<Direction, Direction> connectors = block.getConnectors(blockState);
        Direction origin = transaction.origin;

        if (origin == connectors.getFirst())
            return connectors.getSecond();
        else if (origin == connectors.getSecond())
            return connectors.getFirst();

        return null;
    }
}
