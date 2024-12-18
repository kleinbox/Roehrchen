package dev.kleinbox.roehrchen.common.core;

import dev.kleinbox.roehrchen.Roehrchen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

public class Register {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPE = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MOD_ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, MOD_ID);

    private static final ArrayList<Supplier<Item>> main_tab_content = new ArrayList<>();

    private static final Supplier<CreativeModeTab> ROEHRCHEN_TAB = CREATIVE_MODE_TABS.register("roehrchen", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + MOD_ID + ".main"))
            .icon(() -> new ItemStack(Roehrchen.REGISTERED.GLASS_PIPE.getItem()))
            .displayItems((params, output) -> {
                for (Supplier<Item> item : main_tab_content) {
                    output.accept(new ItemStack(item.get()));
                }
            })
            .build()
    );

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPE.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private final String name;

    private Supplier<Block> blockSupplier = () -> null;
    private Supplier<BlockEntityType<?>> blockEntitySupplier = () -> null;
    private Supplier<Item> itemSupplier = () -> null;

    public Register(String name) {
        this.name = name;
    }

    public Register block(Supplier<Block> supplier) {
        this.blockSupplier = BLOCKS.register(name, supplier);
        return this;
    }

    public Register blockEntity(Function<@Nullable Block, BlockEntityType<?>> supplier) {
        this.blockEntitySupplier = BLOCK_ENTITY_TYPE.register(name, () -> supplier.apply(this.blockSupplier.get()));
        return this;
    }

    public Register item(Function<@Nullable Block, Item> supplier) {
        itemSupplier = ITEMS.register(name, () -> supplier.apply(this.blockSupplier.get()));
        return this;
    }

    public Registered build() {
        main_tab_content.add(itemSupplier);

        return new Registered(
                blockSupplier,
                blockEntitySupplier,
                itemSupplier);
    }

    /**
     * Container for registered suppliers of a feature.
     */
    public static class Registered {
        private final Supplier<Block> blockSupplier;
        private final Supplier<BlockEntityType<?>> blockEntitySupplier ;
        private final Supplier<Item> itemSupplier;

        public Registered(@Nullable Supplier<Block> blockSupplier,
                          @Nullable Supplier<BlockEntityType<?>> blockEntitySupplier,
                          @Nullable Supplier<Item> itemSupplier) {
            this.blockSupplier = blockSupplier;
            this.blockEntitySupplier = blockEntitySupplier;
            this.itemSupplier = itemSupplier;
        }

        /**
         * Returns the block of this feature.
         */
        public Block getBlock() {
            if (blockSupplier == null) {
                Roehrchen.LOGGER.error("Illegal access to block that has not been registered!");
                return null;
            }

            return blockSupplier.get();
        }

        /**
         * Returns the blockEntityType of this feature.
         */
        public BlockEntityType<?> getBlockEntityType() {
            if (blockEntitySupplier == null) {
                Roehrchen.LOGGER.error("Illegal access to blockEntityType that has not been registered!");
                return null;
            }

            return blockEntitySupplier.get();
        }

        /**
         * Returns the item of this feature.
         */
        public Item getItem() {
            if (itemSupplier == null) {
                Roehrchen.LOGGER.error("Illegal access to item that has not been registered!");
                return null;
            }

            return itemSupplier.get();
        }
    }
}
