package dev.kleinbox.roehrchen;

import dev.kleinbox.roehrchen.api.RoehrchenRegistries;
import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.api.TransactionConsumerHandler;
import dev.kleinbox.roehrchen.api.TransactionRedirectHandler;
import dev.kleinbox.roehrchen.common.core.Register;
import dev.kleinbox.roehrchen.common.core.Register.Registered;
import dev.kleinbox.roehrchen.common.core.payload.AnnounceTransactionPayload;
import dev.kleinbox.roehrchen.common.core.payload.ChunkTransactionsPayload;
import dev.kleinbox.roehrchen.common.core.tracker.ChunkTransactionsAttachment;
import dev.kleinbox.roehrchen.common.feature.block.distribution.merger.MergerBlock;
import dev.kleinbox.roehrchen.common.feature.block.distribution.merger.MergerTransactionRedirect;
import dev.kleinbox.roehrchen.common.feature.block.distribution.splitter.SplitterBlock;
import dev.kleinbox.roehrchen.common.feature.block.distribution.splitter.SplitterBlockEntity;
import dev.kleinbox.roehrchen.common.feature.block.distribution.splitter.SplitterTransactionConsumer;
import dev.kleinbox.roehrchen.common.feature.block.pipe.glass.GlassPipeBlock;
import dev.kleinbox.roehrchen.common.feature.block.pipe.glass.GlassPipeTransactionRedirect;
import dev.kleinbox.roehrchen.common.feature.block.pump.economic.EconomicalPumpBlock;
import dev.kleinbox.roehrchen.common.feature.block.pump.economic.EconomicalPumpIntermediaryRedirect;
import dev.kleinbox.roehrchen.common.feature.item.ComplexBlockItem;
import dev.kleinbox.roehrchen.common.feature.transaction.ItemTransaction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

public class Registries {
    private static final DeferredRegister<Transaction<?, ?>> TRANSACTIONS = DeferredRegister.create(RoehrchenRegistries.TRANSACTION_REGISTRY, MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    // Core
    public final Supplier<ItemTransaction> ITEM_TRANSACTION;
    public final Supplier<AttachmentType<ChunkTransactionsAttachment>> CHUNK_TRANSACTIONS;

    // Feature
    public final Registered GLASS_PIPE;
    public final Registered ECONOMICAL_PUMP;
    public final Registered MERGER;
    public final Registered SPLITTER;

    public Registries() {
        this.ITEM_TRANSACTION = TRANSACTIONS.register(
                "item",
                ItemTransaction::new
        );

        this.CHUNK_TRANSACTIONS = ATTACHMENT_TYPES.register(
                "chunk_transactions",
                () -> AttachmentType.serializable(ChunkTransactionsAttachment::new).build()
        );

        this.GLASS_PIPE = new Register("glass_pipe")
                .block(GlassPipeBlock::new)
                .item((block) -> new ComplexBlockItem<>(block, new Item.Properties()))
                .build();

        this.ECONOMICAL_PUMP = new Register("economical_pump")
                .block(EconomicalPumpBlock::new)
                .item((block) -> new ComplexBlockItem<>(block, new Item.Properties()))
                .build();

        this.MERGER = new Register("merger")
                .block(MergerBlock::new)
                .item((block) -> new ComplexBlockItem<>(block, new Item.Properties()))
                .build();

        //noinspection DataFlowIssue
        this.SPLITTER = new Register("splitter")
                .block(SplitterBlock::new)
                .blockEntity((block) -> BlockEntityType.Builder.of(
                        SplitterBlockEntity::new,
                        block
                        ).build(null)
                )
                .item((block) -> new ComplexBlockItem<>(block, new Item.Properties()))
                .build();
    }

    @SubscribeEvent
    protected static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(
                TransactionRedirectHandler.TRANSACTION_REDIRECT_HANDLER,
                (level, pos, state, be, side) -> GlassPipeTransactionRedirect.create(level, pos, state, side),
                Roehrchen.REGISTERED.GLASS_PIPE.getBlock()
        );
        event.registerBlock(
                TransactionRedirectHandler.TRANSACTION_REDIRECT_HANDLER,
                (level, pos, state, be, side) -> EconomicalPumpIntermediaryRedirect.create(level, pos, state, side),
                Roehrchen.REGISTERED.ECONOMICAL_PUMP.getBlock()
        );
        event.registerBlock(
                TransactionRedirectHandler.TRANSACTION_REDIRECT_HANDLER,
                (level, pos, state, be, side) -> MergerTransactionRedirect.create(level, pos, state, side),
                Roehrchen.REGISTERED.MERGER.getBlock()
        );
        event.registerBlockEntity(
                TransactionConsumerHandler.TRANSACTION_CONSUME_HANDLER,
                Roehrchen.REGISTERED.SPLITTER.getBlockEntityType(),
                SplitterTransactionConsumer::create
        );
    }

    @SubscribeEvent
    protected static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar register = event.registrar("1")
                        .executesOn(HandlerThread.NETWORK);

        register.playToClient(
                ChunkTransactionsPayload.TYPE,
                ChunkTransactionsPayload.STREAM_CODEC,
                ChunkTransactionsPayload::handleClientDataOnMain
        );

        register.playToClient(
                AnnounceTransactionPayload.TYPE,
                AnnounceTransactionPayload.STREAM_CODEC,
                AnnounceTransactionPayload::handleClientDataOnMain
        );
    }

    protected static void registerToBus(IEventBus modEventBus) {
        Register.register(modEventBus);
        TRANSACTIONS.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
