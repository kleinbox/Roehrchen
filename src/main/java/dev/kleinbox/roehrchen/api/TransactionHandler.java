package dev.kleinbox.roehrchen.api;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

/**
 * <p>Blocks implementing this interface will be used by the
 * {@link dev.kleinbox.roehrchen.core.tracker.TransactionTracker}.</p>
 */
public interface TransactionHandler {
    BlockCapability<TransactionHandler, @Nullable Direction> TRANSACTION_HANDLER_BLOCK =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, "transaction_handler"),
                    TransactionHandler.class
            );

    /**
     * <p>Can be called with any kind of transaction for validation
     * if it can use this or not.</p>
     *
     * @return Whenever it is being accepted or not.
     */
    boolean request(Transaction<?, ?> transaction);

    /**
     * <p>Will be called for existing transactions to get to know
     * where to move it next.</p>
     *
     * @return The direction relative to this block, or null if no fitting one has been found.
     */
    @Nullable
    Direction next(Transaction<?, ?> transaction);
}
