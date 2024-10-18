package dev.kleinbox.roehrchen.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

import static dev.kleinbox.roehrchen.Initialization.LOGGER;

public abstract class GenericPipe extends Block {
    public static final EnumProperty<Direction> END_1 = EnumProperty.create("end_1", Direction.class);
    public static final EnumProperty<Direction> END_2 = EnumProperty.create("end_2", Direction.class);

    public GenericPipe(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(END_1, Direction.NORTH)
                .setValue(END_2, Direction.SOUTH)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(END_1)
                .add(END_2);
    }

    /**
     * Returns the pipe next pipe of the given block position
     * where the side determines the direction to go.
     *
     * @param level The level to get the BlockStates from.
     * @param pos Block position of the current pipe _(does not have to be a pipe actually)_.
     * @param side The direction to look for the next pipe.
     *
     * @return Will either return a pipe or null, if anything else has been found.
     */
    @Nullable
    public BlockPos getNextPipeFrom(Level level, BlockPos pos, Direction side) {
        BlockPos neighborPos = pos.relative(side);
        BlockState neighbor = level.getBlockState(neighborPos);

        if (neighbor.getBlock() == this) {
            return neighborPos;
        }

        return null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // Orientation of this pipe.
        Direction end_1 = context.getNearestLookingDirection();
        Direction end_2 = end_1.getOpposite();

        // TODO: This following block does not belong in getStateForPlacement
        //       should be moved to... updateShape I think?

        // We need to check the environment for other pipes and bend those towards us, if possible.
        // We are checking for each end if it is free and bend one of them.
        for (Direction direction : Arrays.asList(end_1, end_2)) {
            BlockPos neighborPos = getNextPipeFrom(level, pos, direction);
            if (neighborPos == null)
                continue;

            // Found a pipe in this pipes way.
            BlockState neighbor = context.getLevel().getBlockState(neighborPos);

            List<EnumProperty<Direction>> ends = Arrays.asList(END_1, END_2);
            for (int i = 0; i<=1; i++) {
                Direction neighborsDirection = neighbor.getValue(ends.get(i));

                // Checking if the found pipe has a free connector to bend towards us.
                BlockPos neighborsNeighborPos = getNextPipeFrom(level, neighborPos, neighborsDirection);
                if (neighborsNeighborPos != null)
                    continue;

                // Making sure the other half is not already bent the same way.
                if (i == 0 && neighbor.getValue(ends.get(1)) == direction.getOpposite())
                    break;

                // One connector is free and therefore bendable.
                BlockState newNeighborsState = neighbor.setValue(ends.get(i), direction.getOpposite());
                level.setBlock(neighborPos, newNeighborsState, Block.UPDATE_CLIENTS);
                break;
            }
        }

        return this.defaultBlockState()
                .setValue(END_1, end_1)
                .setValue(END_2, end_2);
    }
}
