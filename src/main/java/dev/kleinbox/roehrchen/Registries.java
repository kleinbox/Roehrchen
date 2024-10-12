package dev.kleinbox.roehrchen;

import dev.kleinbox.roehrchen.feature.pipe.glass.GlassPipeBlock;
import dev.kleinbox.roehrchen.feature.pipe.glass.GlassPipeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.kleinbox.roehrchen.Initialization.MODID;

public class Registries {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);

    public static final DeferredHolder<Block, GlassPipeBlock> GLASS_PIPE_BLOCK = BLOCKS.register("glass_pipe", GlassPipeBlock::new);
    public static final DeferredHolder<Item, GlassPipeItem> GLASS_PIPE_ITEM = ITEMS.register("glass_pipe", GlassPipeItem::new);
}
