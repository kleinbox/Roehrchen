package dev.kleinbox.roehrchen.common.core.block;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * This abstract block ensures the same logic for all pipes.
 */
public abstract class GenericPipeBlock extends Block {
    public static final EnumProperty<Direction> END_1 = EnumProperty.create("end_1", Direction.class);
    public static final EnumProperty<Direction> END_2 = EnumProperty.create("end_2", Direction.class);

    public GenericPipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(END_1, Direction.NORTH)
                .setValue(END_2, Direction.SOUTH));
    }

    /**
     * Returns the next pipe relative to the given block position.
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

        if (neighbor.getBlock() == this)
            return neighborPos;

        return null;
    }

    /**
     * Returns both connector values of the given pipe.
     *
     * @param state BlockState of the pipe.
     * @return Pair of the directions of both connectors.
     *
     * @throws IllegalArgumentException Whenever the BlockState does not have both connector values.
     */
    public Pair<Direction, Direction> getConnectors(BlockState state) {
        return new Pair<>(
                state.getValue(END_1),
                state.getValue(END_2));
    }

    /**
     * Returns the Axis of the pipe or null, if it is a corner.
     *
     * @param state BlockState of the pipe.
     * @return The axis of the pipe or null, if it is a corner.
     *
     * @throws IllegalArgumentException Whenever the BlockState does not have both connector values.
     */
    @Nullable
    public Axis getOrientation(BlockState state) {
        Axis end_1 = state.getValue(END_1).getAxis();
        Axis end_2 = state.getValue(END_2).getAxis();

        if (end_1 != end_2)
            return null;

        return end_1;
    }

    // Block related methods ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos,
                                           @NotNull CollisionContext context) {
        VoxelShape shape;

        shape = switch (getOrientation(state)) {
            case X -> Shapes.box(0, 0.25, 0.25, 1, 0.75, 0.75);
            case Y -> Shapes.box(0.25, 0, 0.25, 0.75, 1, 0.75);
            case Z -> Shapes.box(0.25, 0.25, 0, 0.75, 0.75, 1);
            case null -> {
                VoxelShape corner = Shapes.box(0.21875, 0.21875, 0.21875, 0.78125, 0.78125, 0.78125);

                for (Direction direction : Arrays.asList(state.getValue(END_1), state.getValue(END_2))) {
                    VoxelShape part = switch (direction) {
                        case NORTH -> Shapes.box(0.25, 0.25, 0, 0.75, 0.75, 0.21875);
                        case EAST -> Shapes.box(0.78125, 0.25, 0.25, 1, 0.75, 0.75);
                        case SOUTH -> Shapes.box(0.25, 0.25, 0.78125, 0.75, 0.75, 1);
                        case WEST -> Shapes.box(0, 0.25, 0.25, 0.21875, 0.75, 0.75);
                        case UP -> Shapes.box(0.25, 0.78125, 0.25, 0.75, 1, 0.75);
                        case DOWN -> Shapes.box(0.25, 0, 0.25, 0.75, 0.21875, 0.75);
                    };

                    corner = Shapes.join(corner, part, BooleanOp.OR);
                }

                yield corner;
            }
        };

        return shape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(END_1).add(END_2);
    }

    @Override
    protected void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                           @NotNull BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (movedByPiston)
            return;

        Pair<Direction, Direction> connectors = getConnectors(state);

        // We need to check the environment for other pipes and bend those towards us, if possible.
        // We are checking for each end if it is free and bend one of them.
        for (Direction direction : new Direction[]{connectors.getFirst(), connectors.getSecond()}) {
            BlockPos neighborPos = getNextPipeFrom(level, pos, direction);
            if (neighborPos == null)
                continue;

            // Found a pipe in this pipes way.
            BlockState neighbor = level.getBlockState(neighborPos);

            // Check which connectors are free to bend.
            List<EnumProperty<Direction>> ends = Arrays.asList(END_1, END_2);
            for (int i = 0; i<=1; i++) {
                Direction neighborsDirection = neighbor.getValue(ends.get(i));
                if (neighborsDirection == connectors.getFirst() || neighborsDirection == connectors.getSecond())
                    break;

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
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Orientation of this pipe.
        Direction end_1 = context.getNearestLookingDirection();
        Direction end_2 = end_1.getOpposite();

        return this.defaultBlockState()
                .setValue(END_1, end_1)
                .setValue(END_2, end_2);
    }
}
