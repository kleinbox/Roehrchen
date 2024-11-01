package dev.kleinbox.roehrchen.feature.block.distribution.splitter;

import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.api.TransactionConsumerHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

public class SplitterTransactionConsumer implements TransactionConsumerHandler {

    private final SplitterBlockEntity blockEntity;

    private SplitterTransactionConsumer(SplitterBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Nullable
    public static SplitterTransactionConsumer create(BlockEntity anyBlockEntity, @Nullable Direction side) {
        if (!(anyBlockEntity instanceof SplitterBlockEntity blockEntity))
            return null;


        if (getInputDir(blockEntity) == side && side != null)
            return null;

        return new SplitterTransactionConsumer(blockEntity);
    }

    @Nullable
    private static Direction getInputDir(SplitterBlockEntity blockEntity) {
        BlockState blockState = blockEntity.getBlockState();
        if (!(blockState.getBlock() instanceof SplitterBlock))
            return null;

        return blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public boolean consume(Transaction<?, ?> transaction) {
        blockEntity.redirectTransaction(transaction);
        return true;
    }
}
