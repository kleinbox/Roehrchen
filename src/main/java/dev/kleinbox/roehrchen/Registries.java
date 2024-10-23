package dev.kleinbox.roehrchen;

import com.mojang.serialization.Codec;
import dev.kleinbox.roehrchen.core.ComplexBlockItem;
import dev.kleinbox.roehrchen.core.GlassPipeNetwork;
import dev.kleinbox.roehrchen.feature.pipe.glass.GlassPipeBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedList;
import java.util.function.Supplier;

import static dev.kleinbox.roehrchen.Initialization.MODID;

public class Registries {
    protected static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    protected static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, MODID);
    protected static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);

    public static final Supplier<AttachmentType<LinkedList<GlassPipeNetwork.TravelingItem>>> TRAVELING_ITEMS = ATTACHMENT_TYPES.register(
            "traveling_items",
            () -> AttachmentType.<LinkedList<GlassPipeNetwork.TravelingItem>>builder(() -> new LinkedList<>())
                    .serialize(Codec.list(GlassPipeNetwork.TravelingItem.CODEC).xmap(LinkedList::new, list -> list))
                    .build()
    );

    public static final DeferredHolder<Block, GlassPipeBlock> GLASS_PIPE_BLOCK = BLOCKS.register(
            "glass_pipe",
            GlassPipeBlock::new
    );
    public static final DeferredHolder<Item, ComplexBlockItem<GlassPipeBlock>> GLASS_PIPE_ITEM = ITEMS.register(
            "glass_pipe",
            () -> new ComplexBlockItem<>(Registries.GLASS_PIPE_BLOCK.get(), new Item.Properties())
    );
}
