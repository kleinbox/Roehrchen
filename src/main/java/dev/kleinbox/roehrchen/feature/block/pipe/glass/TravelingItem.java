package dev.kleinbox.roehrchen.feature.block.pipe.glass;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents an item that is traveling through a pipe.
 */
public class TravelingItem {
    public static final int MAX_STEPS = 7; // Three on each side + a center

    private final ItemStack product;
    private final Direction side;
    private int progress;

    /**
     * Creates a new instance for an item that is entering the glass pipe network.
     *
     * @param product The item to be transported.
     * @param side The side from which the product came from. E.g. if the item is being pushed to a pipe relative
     *             to NORTH, then the origin direction for the new pipe would be SOUTH.
     *             We are storing the origin and not the heading direction because pipes in-front of us might be
     *             bent another way.
     */
    public TravelingItem(ItemStack product, Direction side) {
        this(product, side, MAX_STEPS);
    }

    private TravelingItem(ItemStack product, Direction side, int progress) {
        this.product = product;
        this.side = side;
        this.progress = progress;
    }

    /**
     * Makes the item go another step.
     *
     * @return Will return true whenever the item is ready to move on.
     */
    public boolean step() {
        progress--;
        return progress <= 0;
    }

    public ItemStack getProduct() {
        return product;
    }

    /**
     * The direction this product came from.
     * Notice that this is not the traveling direction but from which side it came from.
     * In order to figure out which side to move next, a connector that does not match this should be used.
     */
    public Direction cameFrom() {
        return side;
    }

    public CompoundTag save(HolderLookup.@NotNull Provider registries) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put("product", product.save(registries));
        compoundTag.putString("side", side.getName());
        compoundTag.putInt("progress", Math.max(progress, 0));

        return compoundTag;
    }

    /**
     * Attempts to parse a traveling item.
     * If the product cannot be reassembled, null will be returned, as this makes
     * this instance redundant.
     */
    @Nullable
    public static TravelingItem load(CompoundTag compoundTag, HolderLookup.@NotNull Provider registries) {
        Optional<ItemStack> product = ItemStack.parse(registries, compoundTag);
        if (product.isEmpty())
            return null;

        Direction side = Direction.byName(compoundTag.getString("side"));
        if (side == null)
            side = Direction.NORTH;

        int progress = compoundTag.getInt("progress");

        return new TravelingItem(product.get(), side, progress);
    }
}