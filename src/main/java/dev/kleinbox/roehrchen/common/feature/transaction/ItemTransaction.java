package dev.kleinbox.roehrchen.common.feature.transaction;

import com.mojang.serialization.Codec;
import dev.kleinbox.roehrchen.api.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

public class ItemTransaction extends Transaction<ItemStack, ItemTransaction> {

    public ItemTransaction(ItemStack item, Direction origin, BlockPos blockPos) {
        this.product = item;
        this.origin = origin;
        this.blockPos = blockPos;
    }

    public ItemTransaction() { }

    @Override
    public ItemTransaction createEmpty() {
        return new ItemTransaction();
    }

    @Override
    public @Nullable ItemTransaction extractFrom(Level level, BlockPos blockPos, Direction origin) {
        IItemHandler handler = getHandler(level, blockPos.relative(origin), origin.getOpposite());
        if (handler == null)
            return null;

        ItemStack extractedStack = ItemStack.EMPTY;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stackInSlot = handler.getStackInSlot(i);
            if (extractedStack.isEmpty() && !stackInSlot.isEmpty())
                extractedStack = handler.extractItem(i, stackInSlot.getCount(), false);
            else if (ItemStack.isSameItem(extractedStack, stackInSlot) && !stackInSlot.isEmpty()) {
                int combinedCount = extractedStack.getCount() + stackInSlot.getCount();
                int maxStackSize = extractedStack.getMaxStackSize();

                if (combinedCount <= maxStackSize) {
                    extractedStack.grow(stackInSlot.getCount());
                    handler.extractItem(i, stackInSlot.getCount(), false);
                } else {
                    int spaceLeft = maxStackSize - extractedStack.getCount();
                    extractedStack.setCount(maxStackSize);
                    handler.extractItem(i, spaceLeft, false);
                }
            }

            if (extractedStack.getCount() >= extractedStack.getMaxStackSize())
                break;
        }

        if (extractedStack.isEmpty())
            return null;

        return new ItemTransaction(extractedStack, origin, blockPos);
    }

    @Override
    public boolean unwind(Level level) {
        IItemHandler handler = getHandler(level, this.blockPos, this.origin);
        if (handler == null)
            return false;

        if (!this.product.isEmpty())
            for (int i = 0; i < handler.getSlots(); i++) {
                this.product = handler.insertItem(i, this.product, false);

                if (this.product.isEmpty())
                    return true;
            }

        return false;
    }

    @Override
    public void terminate(Level level) {
        ItemStack item = this.product;
        Vec3 center = this.blockPos.getCenter();
        Vec3i direction = this.origin.getOpposite().getNormal();

        ItemEntity itemEntity = new ItemEntity(level,
                center.x - (double)direction.getX()*0.4,
                center.y - (double)direction.getY()*0.4,
                center.z - (double)direction.getZ()*0.4,
                item,
                (double)direction.getX()*0.2,
                (double)direction.getY()*0.2,
                (double)direction.getZ()*0.2);

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

    @Nullable
    private static IItemHandler getHandler(Level level, BlockPos blockPos, Direction origin) {
        IItemHandler output;

        IItemHandler capability = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                blockPos,
                origin
        );

        if (capability != null) {
            output = capability;
        } else {
            // Container
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (!(blockEntity instanceof Container container))
                return null;

            output = new InvWrapper(container);
        }

        return output;
    }
}
