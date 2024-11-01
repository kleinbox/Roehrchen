package dev.kleinbox.roehrchen.api;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

/**
 * Blocks implementing this Capability represent the end of the way.
 * Transactions will be removed from the tracker after being consumed.
 */
public interface TransactionConsumerHandler {
    BlockCapability<TransactionConsumerHandler, @Nullable Direction> TRANSACTION_CONSUME_HANDLER =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "transaction_consume_handler"),
                    TransactionConsumerHandler.class
            );

    /**
     * Whenever a transaction reaches an end, this method is being called.
     * It is up to the block or blockEntity to handle and store it.
     *
     * The transaction will be deleted from the tacker after calling this method.
     *
     * @param transaction The transaction to consume.
     * @return Whenever it consumed the transaction or not.
     */
    boolean consume(Transaction<?,?> transaction);
}
