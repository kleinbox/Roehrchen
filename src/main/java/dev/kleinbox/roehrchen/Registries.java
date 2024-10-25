package dev.kleinbox.roehrchen;

import com.mojang.serialization.Codec;
import dev.kleinbox.roehrchen.feature.block.pipe.glass.GlassPipeItemHandlerProvider;
import dev.kleinbox.roehrchen.feature.item.ComplexBlockItem;
import dev.kleinbox.roehrchen.feature.block.pipe.glass.GlassPipeBlockEntity;
import dev.kleinbox.roehrchen.feature.block.pipe.glass.GlassPipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Supplier;

import static dev.kleinbox.roehrchen.Initialization.MODID;

public class Registries {
    protected static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    protected static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, MODID);
    protected static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES  = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    protected static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, MODID);

    protected static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                GLASS_PIPE_BLOCK_ENTITY.get(),
                GlassPipeItemHandlerProvider::requestCapability
        );
    }

    public static final Supplier<AttachmentType<HashSet<BlockPos>>> WATCHED_GLASS_PIPES = ATTACHMENT_TYPES.register(
            "watched_glass_pipes",
            () -> AttachmentType.<HashSet<BlockPos>>builder(() -> new HashSet<>())
                    .serialize(Codec.list(BlockPos.CODEC).xmap(HashSet::new, ArrayList::new))
                    .build()
    );

    public static final DeferredHolder<Block, GlassPipeBlock> GLASS_PIPE_BLOCK = BLOCKS.register(
            "glass_pipe",
            GlassPipeBlock::new
    );

    public static final Supplier<BlockEntityType<GlassPipeBlockEntity>> GLASS_PIPE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "glass_pipe_block_entity",
            () -> BlockEntityType.Builder.of(GlassPipeBlockEntity::new, GLASS_PIPE_BLOCK.get()).build(null)
    );

    public static final DeferredHolder<Item, ComplexBlockItem<GlassPipeBlock>> GLASS_PIPE_ITEM = ITEMS.register(
            "glass_pipe",
            () -> new ComplexBlockItem<>(Registries.GLASS_PIPE_BLOCK.get(), new Item.Properties())
    );
}
