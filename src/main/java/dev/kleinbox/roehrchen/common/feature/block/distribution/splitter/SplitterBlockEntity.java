package dev.kleinbox.roehrchen.common.feature.block.distribution.splitter;

import dev.kleinbox.roehrchen.Roehrchen;
import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.common.core.tracker.TransactionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;


public class SplitterBlockEntity extends BlockEntity {

    private Side towards = Side.CENTER;

    public SplitterBlockEntity( BlockPos pos, BlockState blockState) {
        super(Roehrchen.REGISTERED.SPLITTER.getBlockEntityType(), pos, blockState);
    }

    public <P> void redirectTransaction(Transaction<P,?> transaction) {
        if (level == null || level.isClientSide)
            return;

        Transaction<P,?> redirected = transaction.createEmpty();

        Direction inputDir = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction outputDir = towards.fromOrigin(inputDir);

        redirected.product = transaction.product;
        redirected.origin = outputDir.getOpposite();
        redirected.blockPos = transaction.blockPos.relative(outputDir);

        TransactionTracker.registerTransaction(level, redirected);

        towards = towards.next();
        setChanged();
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        towards = Side.fromValue(tag.getString("towards"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("towards", towards.toString());
    }

    private enum Side {
        LEFT("left"),
        CENTER("center"),
        RIGHT("right");

        private final String literal;

        Side(String literal) {
            this.literal = literal;
        }

        @Override
        public String toString() {
            return literal;
        }

        public Side next() {
            return switch (this) {
                case LEFT -> Side.CENTER;
                case CENTER -> Side.RIGHT;
                case RIGHT -> Side.LEFT;
            };
        }

        public Direction fromOrigin(Direction origin) {
            return switch (this) {
                case LEFT -> origin.getClockWise();
                case CENTER -> origin.getOpposite();
                case RIGHT -> origin.getCounterClockWise();
            };
        }

        public static Side fromValue(String literal) {
            return switch (literal) {
                case ("left") -> Side.LEFT;
                case ("right") -> Side.RIGHT;
                default -> Side.CENTER;
            };
        }
    }
}
