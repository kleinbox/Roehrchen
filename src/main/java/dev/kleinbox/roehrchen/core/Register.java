package dev.kleinbox.roehrchen.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

public class Register {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MOD_ID);

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    private final String name;

    private Supplier<Block> blockSupplier = () -> null;
    private Supplier<Item> itemSupplier = () -> null;

    public Register(String name) {
        this.name = name;
    }

    public Register block(Supplier<Block> supplier) {
        this.blockSupplier = BLOCKS.register(name, supplier);
        return this;
    }

    public Register item(Function<@Nullable Block, Item> supplier) {
        itemSupplier = ITEMS.register(name, () -> supplier.apply(this.blockSupplier.get()));
        return this;
    }

    public Registered build() {
        return new Registered(
                blockSupplier,
                itemSupplier);
    }

    /**
     * Container for registered suppliers of a feature.
     */
    public static class Registered {
        private final Supplier<Block> blockSupplier;
        private final Supplier<Item> itemSupplier;

        public Registered(@Nullable Supplier<Block> blockSupplier,
                          @Nullable Supplier<Item> itemSupplier) {
            this.blockSupplier = blockSupplier;
            this.itemSupplier = itemSupplier;
        }

        /**
         * Returns the block of this feature.
         *
         * @throws IllegalAccessException Caused if this feature does not have a block registered.
         */
        public Block getBlock() throws IllegalAccessException {
            if (blockSupplier == null)
                throw new IllegalAccessException("No block has been registered");

            return blockSupplier.get();
        }

        /**
         * Returns the item of this feature.
         *
         * @throws IllegalAccessException Caused if this feature does not have an item registered.
         */
        public Item getItem() throws IllegalAccessException {
            if (itemSupplier == null)
                throw new IllegalAccessException("No item has been registered");

            return itemSupplier.get();
        }
    }
}
