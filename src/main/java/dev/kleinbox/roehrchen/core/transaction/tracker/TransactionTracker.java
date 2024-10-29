package dev.kleinbox.roehrchen.core.transaction.tracker;

import com.mojang.datafixers.util.Pair;
import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.api.TransactionHandler;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;

import static dev.kleinbox.roehrchen.Roehrchen.LOGGER;
import static dev.kleinbox.roehrchen.Roehrchen.REGISTERED;

/**
 * <p>Keeps track of all pipes that need to be processed in level.
 * We are doing this to save on a bit of performance in case our pipes are being used as decorations (this way, we do
 * not tick them).</p>
 *
 * <p>It only is a small performance boost, though.</p>
 */
public class TransactionTracker {
    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();

        ArrayList<Pair<Transaction<?,?>, LevelChunk>> relocate = new ArrayList<>();
        ArrayList<ChunkPos> removedChunks = new ArrayList<>();

        LevelTransactionChunksSD chunkTransactions = LevelTransactionChunksSD.getFromLevel(level);
        for (ChunkPos chunkPos : chunkTransactions.watchlist) {
            // Check each chunk
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            ChunkTransactionsAttachment transactions = chunk.getData(REGISTERED.CHUNK_TRANSACTIONS);
            
            ArrayList<Transaction<?, ?>> removedTransactions = new ArrayList<>();
            
            for (Transaction<?, ?> transaction : transactions) {
                // Check each transaction in chunk
                TransactionHandler capability = level.getCapability(
                        TransactionHandler.TRANSACTION_HANDLER_BLOCK,
                        transaction.blockPos,
                        null);

                if (capability == null) {
                    // Transaction reached end
                    if (!transaction.unwind(level))
                        transaction.terminate(level);

                    removedTransactions.add(transaction);
                } else {
                    if (!transaction.leaving) {
                        // Tiny step within same block
                        transaction.leaving = true;
                        chunk.setUnsaved(true);
                        continue;
                    }

                    Direction next = capability.next(transaction);

                    if (!capability.request(transaction) || next == null) {
                        // Capability refuses to take the transaction
                        transaction.terminate(level);
                        removedTransactions.add(transaction);
                        chunk.setUnsaved(true);
                        continue;
                    }

                    // Move transaction one step
                    transaction.origin = next.getOpposite();
                    transaction.blockPos = transaction.blockPos.relative(next);
                    transaction.leaving = false;

                    // Changed chunk
                    if (level.getChunk(transaction.blockPos) != chunk)
                        relocate.add(new Pair<>(transaction, chunk));
                }

                chunk.setUnsaved(true);
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
                if (chunkTransactions.watchlist.remove(oldChunk.getPos()))
                    chunkTransactions.setDirty();

            ChunkAccess newChunk = level.getChunk(transaction.blockPos);
            ChunkTransactionsAttachment newTransactions = newChunk.getData(REGISTERED.CHUNK_TRANSACTIONS);
            newTransactions.add(transaction);
            newChunk.setUnsaved(true); // This one maybe not yet

            // Make sure to watch it too now
            if(chunkTransactions.watchlist.add(newChunk.getPos()))
                chunkTransactions.setDirty();
        }
        
        // Remove chunks
        for (ChunkPos chunkPos : removedChunks) {
        	if (chunkTransactions.watchlist.remove(chunkPos))
                chunkTransactions.setDirty();
        }
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

    // TODO: ChunkWatchEvent.Send etc. to make clients aware of items

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
