package dev.kleinbox.roehrchen;

import com.mojang.serialization.Codec;
import dev.kleinbox.roehrchen.api.RoehrchenRegistries;
import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.api.TransactionHandler;
import dev.kleinbox.roehrchen.core.Register;
import dev.kleinbox.roehrchen.core.Register.Registered;
import dev.kleinbox.roehrchen.core.tracker.transaction.ItemTransaction;
import dev.kleinbox.roehrchen.feature.block.pipe.glass.GlassPipeBlock;
import dev.kleinbox.roehrchen.feature.block.pipe.glass.GlassPipeIntermediaryHandler;
import dev.kleinbox.roehrchen.feature.item.ComplexBlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Supplier;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

public class Registries {
    private static final DeferredRegister<Transaction<?, ?>> TRANSACTIONS = DeferredRegister.create(RoehrchenRegistries.TRANSACTION_REGISTRY, MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    public final Supplier<ItemTransaction> ITEM_TRANSACTION;
    public final Supplier<AttachmentType<HashSet<Transaction<?,?>>>> WATCHED_GLASS_PIPES;
    public final Registered GLASS_PIPE;

    public Registries() {
        this.ITEM_TRANSACTION = TRANSACTIONS.register(
                "item",
                ItemTransaction::new
        );

        this.WATCHED_GLASS_PIPES = ATTACHMENT_TYPES.register(
                "watched_glass_pipes",
                () -> AttachmentType.<HashSet<Transaction<?, ?>>>builder(() -> new HashSet<>())
                        .serialize(Codec.list(Transaction.CODEC).xmap(HashSet::new, ArrayList::new))
                        .build()
        );

        this.GLASS_PIPE = new Register("glass_pipe")
                .block(GlassPipeBlock::new)
                .item((block) -> new ComplexBlockItem<>(block, new Item.Properties()))
                .build();
    }

    protected static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                TransactionHandler.TRANSACTION_HANDLER_BLOCK,
                (level, pos, state, be, side) -> GlassPipeIntermediaryHandler.create(level, pos, state, side)

        );
    }

    protected static void registerToBus(IEventBus modEventBus) {
        Register.register(modEventBus);
        TRANSACTIONS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
