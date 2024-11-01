package dev.kleinbox.roehrchen.core.tracker;

import com.mojang.datafixers.util.Pair;
import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.api.TransactionConsumerHandler;
import dev.kleinbox.roehrchen.api.TransactionRedirectHandler;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;

import static dev.kleinbox.roehrchen.Roehrchen.REGISTERED;

/**
 * <p>Keeps track of all pipes that need to be processed in level.
 * We are doing this to save on a bit of performance in case our pipes are being used as decorations (this way, we do
 * not tick them).</p>
 *
 * <p>It only is a small performance boost, though.</p>
 */
public class TransactionTracker {

    public static final int MAX_COOLDOWN = 20; // TODO: Make it take longer

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide)
            return;

        ArrayList<Pair<Transaction<?,?>, LevelChunk>> relocate = new ArrayList<>();
        ArrayList<ChunkPos> removedChunks = new ArrayList<>();

        LevelTransactionChunksSD chunkTransactions = LevelTransactionChunksSD.getFromLevel(level);
        chunkTransactions.setDirty();

        if (chunkTransactions.cooldown > 0) {
            chunkTransactions.cooldown--;
            return;
        }

        chunkTransactions.cooldown = 1;//MAX_COOLDOWN;

        for (ChunkPos chunkPos : chunkTransactions.watchlist) {
            // Check each chunk
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            ChunkTransactionsAttachment transactions = chunk.getData(REGISTERED.CHUNK_TRANSACTIONS);
            
            ArrayList<Transaction<?, ?>> removedTransactions = new ArrayList<>();

            // Fix java.util.ConcurrentModificationException: null
            for (Transaction<?, ?> transaction : transactions) {
                // Check each transaction in chunk
                chunk.setUnsaved(true);

                // Redirect transaction
                TransactionRedirectHandler capability_redirect = level.getCapability(
                        TransactionRedirectHandler.TRANSACTION_REDIRECT_HANDLER,
                        transaction.blockPos,
                        null);

                if (capability_redirect != null) {
                    Direction next = capability_redirect.next(transaction.origin);

                    if (!capability_redirect.request(transaction) || next == null) {
                        // Capability refuses to take the transaction; Terminate
                        transaction.terminate(level);
                        removedTransactions.add(transaction);
                    } else {
                        // Move transaction one step
                        transaction.origin = next.getOpposite();
                        transaction.blockPos = transaction.blockPos.relative(next);

                        // Changed chunk
                        if (level.getChunk(transaction.blockPos) != chunk)
                            relocate.add(new Pair<>(transaction, chunk));
                    }
                } else {
                    // Transaction reached end

                    // Get consumer for transaction
                    TransactionConsumerHandler capability_consume = level.getCapability(
                            TransactionConsumerHandler.TRANSACTION_CONSUME_HANDLER,
                            transaction.blockPos,
                            null);

                    if (capability_consume == null || !capability_consume.consume(transaction))
                        // No consumer found, throw it out of the system
                        if (!transaction.unwind(level))
                            transaction.terminate(level);

                    removedTransactions.add(transaction);
                }
            }
            
            for (Transaction<?,?> transaction : removedTransactions) {
            	transactions.remove(transaction);
            	// We already marked the chunk as unsaved from the loop before
            }

            if (transactions.isEmpty())
            	removedChunks.add(chunkPos);
        }

        // Relocate all transactions that changed chunks
        for (Pair<Transaction<?, ?>, LevelChunk> data : relocate) {
            Transaction<?, ?> transaction = data.getFirst();

            LevelChunk oldChunk = data.getSecond();
            ChunkTransactionsAttachment oldTransactions = oldChunk.getData(REGISTERED.CHUNK_TRANSACTIONS);
            oldTransactions.remove(transaction);
            //oldChunk.setUnsaved(true); // We already marked the chunk

            if (oldTransactions.isEmpty())
                chunkTransactions.watchlist.remove(oldChunk.getPos());

            ChunkAccess newChunk = level.getChunk(transaction.blockPos);
            ChunkTransactionsAttachment newTransactions = newChunk.getData(REGISTERED.CHUNK_TRANSACTIONS);
            newTransactions.add(transaction);
            newChunk.setUnsaved(true); // This one maybe not yet

            // Make sure to watch it too now
            chunkTransactions.watchlist.add(newChunk.getPos());
        }
        
        // Remove chunks
        for (ChunkPos chunkPos : removedChunks)
        	chunkTransactions.watchlist.remove(chunkPos);
    }

    /**
     * Check if the chunk contains pipes that we need to process.
     */
    @SubscribeEvent
    public void onChunkLoaded(ChunkEvent.Load event) {
        if (!event.isNewChunk())
            makeLevelAwareOfChunk(event.getChunk());
    }

    /**
     * Unloaded chunks do not need to be processed.
     */
    @SubscribeEvent
    public void onChunkUnloaded(ChunkEvent.Unload event) {
        ChunkAccess chunk = event.getChunk();
        LevelTransactionChunksSD chunkTransactions = LevelTransactionChunksSD.getFromLevel(chunk.getLevel());

        if (chunkTransactions.watchlist.remove(chunk.getPos()))
            chunkTransactions.setDirty();
    }

    /**
     * Synchronize data towards clients for already existing transactions.
     *
     * @param event
     */
    public void onPlayerGettingChunk(ChunkWatchEvent.Sent event) {

    }

    /**
     * <p>Registers a new transaction in the chunk.</p>
     *
     * @param level The level containing the chunk.
     * @param transaction The transaction to register.
     */
    public static void registerTransaction(Level level, Transaction<?, ?> transaction) {
        ChunkAccess chunk = level.getChunk(transaction.blockPos);
        ChunkTransactionsAttachment data = chunk.getData(REGISTERED.CHUNK_TRANSACTIONS);

        data.add(transaction);
        chunk.setUnsaved(true);

        makeLevelAwareOfChunk(chunk);
    }

    /**
     * <p>Used to make the level aware of a chunk with blocks to be watched.</p>
     *
     * <p>This will do nothing, if the chunk does not have any transactions.</p>
     *
     * @param chunk The chunk that needs to be ticked.
     */
    private static void makeLevelAwareOfChunk(ChunkAccess chunk) {
        if (!chunk.hasData(REGISTERED.CHUNK_TRANSACTIONS))
            return;

        ChunkTransactionsAttachment transactions = chunk.getData(REGISTERED.CHUNK_TRANSACTIONS);
        if (transactions.isEmpty())
            return;

        ChunkPos chunkPos = chunk.getPos();
        Level level = chunk.getLevel();

        LevelTransactionChunksSD chunkTransactions = LevelTransactionChunksSD.getFromLevel(level);

        if (chunkTransactions.watchlist.add(chunkPos))
            chunkTransactions.setDirty();
    }

    // TODO: Adjust NBT data for levels and blockentities to be easier to modify(!)
}
