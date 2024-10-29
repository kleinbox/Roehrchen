package dev.kleinbox.roehrchen.core.tracker.transaction;

import com.mojang.serialization.Codec;
import dev.kleinbox.roehrchen.api.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

public class ItemTransaction extends Transaction<ItemStack, ItemTransaction> {

    public ItemTransaction(ItemStack item, Direction origin, BlockPos blockPos, boolean leaving) {
        this.product = item;
        this.origin = origin;
        this.blockPos = blockPos;
        this.leaving = leaving;
    }

    public ItemTransaction() {

    }

    @Override
    @Nullable
    public ItemTransaction with(Object product, Direction origin, BlockPos blockPos, boolean leaving) {
        if (!(product instanceof ItemStack item))
            return null;
        ItemTransaction transaction = new ItemTransaction();

        transaction.product = item;
        transaction.origin = origin;
        transaction.blockPos = blockPos;
        transaction.leaving = leaving;

        return transaction;
    }

    @Override
    public boolean unwind(Level level) {
        IItemHandler capability = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                this.blockPos,
                this.origin
        );

        ItemStack item = this.product;

        if (capability == null) {
            terminate(level);
            return false;
        }

        for (int slot = 0; slot < capability.getSlots(); slot++) {
            if (capability.isItemValid(slot, item)) {
                ItemStack remaining = capability.insertItem(slot, item, false);
                if (remaining.isEmpty())
                    return true;

                item = remaining;
            }
        }

        this.product = item;
        return false;
    }

    @Override
    public void terminate(Level level) {
        ItemStack item = this.product;
        BlockPos blockPos = this.blockPos;

        ItemEntity itemEntity = new ItemEntity(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), item);

        level.addFreshEntity(itemEntity);
    }

    @Override
    public ResourceLocation type() {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "item");
    }

    @Override
    public Codec<ItemStack> codec() {
        return ItemStack.CODEC;
    }
}
