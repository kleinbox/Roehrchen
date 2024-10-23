package dev.kleinbox.roehrchen.core;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

import static dev.kleinbox.roehrchen.Initialization.LOGGER;


import static dev.kleinbox.roehrchen.Registries.TRAVELING_ITEMS;

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
        // Data Attachments for transactions
    }

    @SubscribeEvent
    public void onChunkLoaded(ChunkEvent.Load event) {
        if (!event.isNewChunk())
            makeLevelAwareOfChunk(event.getChunk());
    }

    // TODO: ChunkWatchEvent.Send to make clients aware of items

    /**
     * <p>Used to make the level aware of a chunk with transactions.</p>
     *
     * <p>This will do nothing, if the chunk does not have any transactions.</p>
     *
     * @param chunk The chunk that needs to be ticked.
     */
    public static void makeLevelAwareOfChunk(ChunkAccess chunk) {
        if (!chunk.hasData(TRAVELING_ITEMS))
            return;

        LinkedList<TravelingItem> transactions = chunk.getData(TRAVELING_ITEMS);
        if (transactions.isEmpty())
            return;

        Level level = chunk.getLevel();
    }

    /**
     * <p>Represents an item that is traveling through pipes.</p>
     *
     * @param product The item to transport.
     * @param source The direction it came from.
     * @param progress Indicates how long it takes until it can move another step. If it is equal or smaller than 0,
     *                 then it needs to move another step.
     */
    public record TravelingItem(ItemStack product, Direction source, int progress) {
        public static final Codec<TravelingItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("product").forGetter(TravelingItem::product),
                Direction.CODEC.fieldOf("source").forGetter(TravelingItem::source),
                Codec.INT.fieldOf("progress").forGetter(TravelingItem::progress)
        ).apply(instance, TravelingItem::new));
    }

    /**
     * SD for all chunks that should be watched for transactions.
     */
    public static class ChunkTransactionsSavedData extends SavedData {
        public static final String NBT_FILENAME = "roehrchen_chunk_transactions";
        public static final SavedData.Factory<ChunkTransactionsSavedData> FACTORY = new SavedData.Factory<>(ChunkTransactionsSavedData::create, ChunkTransactionsSavedData::load);

        public LinkedList<Pair<Integer, Integer>> watchlist = new LinkedList<>();

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
            watchlist.forEach((chunkPos) -> {
                int[] coordinates = {chunkPos.getFirst(), chunkPos.getSecond()};
                compoundTag.putIntArray(String.valueOf(chunkPos.hashCode()), coordinates);
            });

            return compoundTag;
        }

        public static ChunkTransactionsSavedData load(CompoundTag compoundTag, HolderLookup.Provider lookupProvider) {
            ChunkTransactionsSavedData data = create();

            for (String key : compoundTag.getAllKeys()) {
                int[] coordinates = compoundTag.getIntArray(key);

                if (coordinates.length == 2) {
                    Pair<Integer, Integer> chunkPos = new Pair<>(coordinates[0], coordinates[1]);

                    if (String.valueOf(chunkPos.hashCode()) != key)
                        LOGGER.warn("Data of " + NBT_FILENAME + " seems to have invalid sections (Chunk [{},{}] do not match the hashcode).", coordinates[0], coordinates[1]);

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
