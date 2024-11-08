package dev.kleinbox.roehrchen.common.core.tracker;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SD for all chunks that should be watched for transactions.
 */
public class LevelTransactionChunksSD extends SavedData {
    public static final Object PRESENT = new Object();

    public static final String NBT_FILENAME = "roehrchen_chunks_with_transactions";
    public static final SavedData.Factory<LevelTransactionChunksSD> FACTORY =
            new SavedData.Factory<>(LevelTransactionChunksSD::create, LevelTransactionChunksSD::load);

    private static final ConcurrentHashMap<ResourceKey<Level>, LevelTransactionChunksSD> CLIENT_DATA = new ConcurrentHashMap<>();

    public ConcurrentHashMap<ChunkPos, Object> watchlist = new ConcurrentHashMap<>();
    public int cooldown = 0;

    public static LevelTransactionChunksSD getFromLevel(Level level) {
        if (level instanceof ServerLevel serverLevel)
            return serverLevel.getDataStorage().computeIfAbsent(FACTORY, NBT_FILENAME);
        else
            return CLIENT_DATA.computeIfAbsent(level.dimension(), l -> new LevelTransactionChunksSD());
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        ListTag tags = new ListTag();

        for (ChunkPos chunkPos : watchlist.keySet()) {
            CompoundTag coordinates = new CompoundTag();
            coordinates.putInt("x", chunkPos.x);
            coordinates.putInt("z", chunkPos.z);

            tags.add(coordinates);
        }

        compoundTag.put("chunks", tags);
        return compoundTag;
    }

    public static LevelTransactionChunksSD load(CompoundTag compoundTag, HolderLookup.Provider lookupProvider) {
        LevelTransactionChunksSD data = create();
        ListTag tags = compoundTag.getList("chunks", ListTag.TAG_COMPOUND);

        for (int i=0; i<tags.size(); i++) {
            CompoundTag coordinates = tags.getCompound(i);
            int x = coordinates.getInt("x");
            int z = coordinates.getInt("z");

            ChunkPos chunkPos = new ChunkPos(x, z);
            data.watchlist.put(chunkPos, PRESENT);
        }

        return data;
    }

    public static LevelTransactionChunksSD create() {
        return new LevelTransactionChunksSD();
    }
}
