package dev.kleinbox.roehrchen.core.tracker;

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
 * Keeps track of all pipes that need to be processed in level.
 * We are doing this to save on a bit of performance in case our pipes are being used as decorations (this way, we do
 * not tick them).
 *
 * It only is a small performance boost, though.
 */
public class TransactionTracker {
    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Pre event) {
        Level level = event.getLevel();

        ArrayList<Pair<Transaction<?,?>, LevelChunk>> relocate = new ArrayList<>();

        ChunkTransactionsSavedData chunkTransactions = ChunkTransactionsSavedData.getFromLevel(level);
        for (ChunkPos chunkPos : chunkTransactions.watchlist) {
            // Check each chunk
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            HashSet<Transaction<?, ?>> transactions = chunk.getData(REGISTERED.WATCHED_GLASS_PIPES);

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

                    transactions.remove(transaction);
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
                        transactions.remove(transaction);
                        chunk.setUnsaved(true);
                        continue;
                    }

                    // Move transaction one step
                    transaction.origin = next;
                    transaction.blockPos = transaction.blockPos.relative(next);
                    transaction.leaving = false;

                    // Changed chunk
                    if (level.getChunk(transaction.blockPos) != chunk)
                        relocate.add(new Pair<>(transaction, chunk));
                }

                chunk.setUnsaved(true);
            }

            if (transactions.isEmpty())
                if (chunkTransactions.watchlist.remove(chunkPos))
                    chunkTransactions.setDirty();
        }

        // Relocate all transactions that changed chunks
        for (Pair<Transaction<?, ?>, LevelChunk> data : relocate) {
            Transaction<?, ?> transaction = data.getFirst();

            LevelChunk oldChunk = data.getSecond();
            HashSet<Transaction<?, ?>> oldTransactions = oldChunk.getData(REGISTERED.WATCHED_GLASS_PIPES);
            oldTransactions.remove(transaction);
            //oldChunk.setUnsaved(true); // We already marked the chunk

            if (oldTransactions.isEmpty())
                if (chunkTransactions.watchlist.remove(oldChunk.getPos()))
                    chunkTransactions.setDirty();

            ChunkAccess newChunk = level.getChunk(transaction.blockPos);
            HashSet<Transaction<?, ?>> newTransactions = newChunk.getData(REGISTERED.WATCHED_GLASS_PIPES);
            newTransactions.add(transaction);
            newChunk.setUnsaved(true); // This one maybe not yet

            // Make sure to watch it too now
            if(chunkTransactions.watchlist.add(newChunk.getPos()))
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
        ChunkTransactionsSavedData chunkTransactions = ChunkTransactionsSavedData.getFromLevel(chunk.getLevel());

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
        HashSet<Transaction<?, ?>> data = chunk.getData(REGISTERED.WATCHED_GLASS_PIPES);

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
        if (!chunk.hasData(REGISTERED.WATCHED_GLASS_PIPES))
            return;

        HashSet<Transaction<?, ?>> transactions = chunk.getData(REGISTERED.WATCHED_GLASS_PIPES);
        if (transactions.isEmpty())
            return;

        ChunkPos chunkPos = chunk.getPos();
        Level level = chunk.getLevel();

        ChunkTransactionsSavedData chunkTransactions = ChunkTransactionsSavedData.getFromLevel(level);

        if (!chunkTransactions.watchlist.contains(chunkPos))
            if (chunkTransactions.watchlist.add(chunkPos))
                chunkTransactions.setDirty();
    }

    // TODO: Adjust NBT data for levels and blockentities to be easier to modify(!)

    /**
     * SD for all chunks that should be watched for transactions.
     */
    public static class ChunkTransactionsSavedData extends SavedData {
        public static final String NBT_FILENAME = "roehrchen_chunk_transactions";
        public static final SavedData.Factory<ChunkTransactionsSavedData> FACTORY =
                new SavedData.Factory<>(ChunkTransactionsSavedData::create, ChunkTransactionsSavedData::load);

        public HashSet<ChunkPos> watchlist = new HashSet<>();

        public static ChunkTransactionsSavedData getFromLevel(Level level) {

            if (level instanceof ServerLevel serverLevel)
                return serverLevel.getDataStorage().computeIfAbsent(FACTORY, NBT_FILENAME);
            else
                return new ChunkTransactionsSavedData();
            //return CLIENT_DATA.computeIfAbsent(level.dimension(), l -> new TravelTargetSavedData());
            // TODO: Track transactions on client too
        }

        @Override
        public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
            for (ChunkPos chunkPos : watchlist) {
                int[] coordinates = {chunkPos.x, chunkPos.z};
                compoundTag.putIntArray(String.valueOf(chunkPos.hashCode()), coordinates);
            }

            return compoundTag;
        }

        public static ChunkTransactionsSavedData load(CompoundTag compoundTag, HolderLookup.Provider lookupProvider) {
            ChunkTransactionsSavedData data = create();

            for (String key : compoundTag.getAllKeys()) {
                int[] coordinates = compoundTag.getIntArray(key);

                if (coordinates.length == 2) {
                    ChunkPos chunkPos = new ChunkPos(coordinates[0], coordinates[1]);

                    if (!String.valueOf(chunkPos.hashCode()).equals(key))
                        LOGGER.warn("Data of {} seems to have invalid sections (Chunk [{},{}] do not match the hashcode).",
                                NBT_FILENAME, coordinates[0], coordinates[1]);

                    data.watchlist.add(chunkPos);
                }
            }

            return data;
        }

        public static ChunkTransactionsSavedData create() {
            return new ChunkTransactionsSavedData();
        }
    }
}
