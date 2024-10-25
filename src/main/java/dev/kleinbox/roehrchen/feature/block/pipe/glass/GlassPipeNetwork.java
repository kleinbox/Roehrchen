package dev.kleinbox.roehrchen.feature.block.pipe.glass;

import dev.kleinbox.roehrchen.Registries;
import net.minecraft.core.BlockPos;
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

import java.util.HashSet;

import static dev.kleinbox.roehrchen.Initialization.LOGGER;


import static dev.kleinbox.roehrchen.Registries.WATCHED_GLASS_PIPES;

// TODO: I should generalize this for all pipes and adjust TravelingItem to an abstract Transaction and make all pipes use it.

/**
 * <p>Responsible for all items that are traveling within glass pipes.</p>
 *
 * <h3>Handling Traveling Items</h3>
 *
 * <p>Each level has an SD of a list of chunks that need to be processed
 * during each tick.</p>
 *
 * <p>Chunks are saving the items that are traveling. An item that is traveling will be moved to another chunk,
 * if it also moves to another chunk during a tick.</p>
 *
 * <h3>Entering and Leaving the Network</h3>
 *
 * <p>If an item enters an pipe, the item capability from GlassPipe is responsible for making the chunk and level
 * aware of the new transaction. If an item leaves the pipe, the level ticker will automatically close the transaction
 * and perhaps the to be watched chunk too.</p>
 */
public class GlassPipeNetwork {
    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Pre event) {
        Level level = event.getLevel();
        ChunkTransactionsSavedData chunkTransactions = ChunkTransactionsSavedData.getFromLevel(level);

        for (ChunkPos chunkPos : chunkTransactions.watchlist) {
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            HashSet<BlockPos> pipes = chunk.getData(WATCHED_GLASS_PIPES);

            for (BlockPos pipe : pipes) {
                // TODO: Pipe logic
            }
        }
    }

    @SubscribeEvent
    public void onChunkLoaded(ChunkEvent.Load event) {
        if (!event.isNewChunk())
            makeLevelAwareOfChunk(event.getChunk());
    }

    @SubscribeEvent
    public void onChunkUnloaded(ChunkEvent.Unload event) {
        ChunkAccess chunk = event.getChunk();
        ChunkTransactionsSavedData chunkTransactions = ChunkTransactionsSavedData.getFromLevel(chunk.getLevel());

        if (chunkTransactions.watchlist.remove(chunk.getPos()))
            chunkTransactions.setDirty(true);
    }

    // TODO: ChunkWatchEvent.Send etc. to make clients aware of items

    /**
     * <p>Used to make the chunk aware of a block with transactions.</p>
     *
     * <p>This will do nothing, if the chunk does not have any transactions.</p>
     * @param level The level containing the chunk
     * @param blockPos The position of the block to be watched.
     */
    public static void makeChunkAwareOfPipe(Level level, BlockPos blockPos) {
        ChunkAccess chunk = level.getChunk(blockPos);
        HashSet<BlockPos> data = chunk.getData(Registries.WATCHED_GLASS_PIPES.get());

        data.add(blockPos);
        chunk.setUnsaved(true);

        GlassPipeNetwork.makeLevelAwareOfChunk(chunk);
    }

    /**
     * Will update a chunk to stop watching a block.
     * If the chunk has no blocks to watch left, the level will stop ticking for it too.
     *
     * @param level The level containing the chunk and block.
     * @param blockPos The position of the block to be watched.
     */
    public static void unwatchBlock(Level level, BlockPos blockPos) {
        ChunkAccess chunk = level.getChunk(blockPos);
        HashSet<BlockPos> data = chunk.getData(Registries.WATCHED_GLASS_PIPES.get());

        if (data.remove(blockPos)) {
            chunk.setUnsaved(true);

            if (data.isEmpty()) {
                ChunkTransactionsSavedData chunkTransactions = ChunkTransactionsSavedData.getFromLevel(level);
                ChunkPos chunkPos = chunk.getPos();

                if (chunkTransactions.watchlist.remove(chunkPos))
                    chunkTransactions.setDirty(true);
            }
        }
    }

    /**
     * <p>Used to make the level aware of a chunk with blocks to be watched.</p>
     *
     * <p>This will do nothing, if the chunk does not have any listed blocks.</p>
     *
     * @param chunk The chunk that needs to be ticked.
     */
    public static void makeLevelAwareOfChunk(ChunkAccess chunk) {
        if (!chunk.hasData(WATCHED_GLASS_PIPES))
            return;

        HashSet<BlockPos> transactions = chunk.getData(WATCHED_GLASS_PIPES);
        if (transactions.isEmpty())
            return;

        ChunkPos chunkPos = chunk.getPos();
        Level level = chunk.getLevel();

        ChunkTransactionsSavedData chunkTransactions = ChunkTransactionsSavedData.getFromLevel(level);

        if (!chunkTransactions.watchlist.contains(chunkPos)) {
            chunkTransactions.watchlist.add(chunkPos);
            chunkTransactions.setDirty(true);
        }
    }

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
