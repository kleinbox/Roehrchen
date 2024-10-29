package dev.kleinbox.roehrchen.api;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.kleinbox.roehrchen.core.transaction.tracker.TransactionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

/**
 * <p>A transaction can be anything that is traveling through the world and has
 * therefore not reached a destination where it can safely be stored.</p>
 *
 * <h3>When to Choose Between an Transaction and a Capability</h3>
 *
 * <p>Instead of having to ask every pipe, for example, on what is going to happen next,
 * one pipe only is being asked once if a transaction may be accepted, while the rest is then only
 * used for orientation. Only the actual transactions are being actually processed by the {@link TransactionTracker}.</p>
 *
 * <p>Meaning that not every block stands for itself but rather many blocks act as one network.</p>
 *
 * <h3>Data Storing and Syncing Between Sides</h3>
 *
 * <p>A transaction may be modified through time on each side separately.
 * Changes will always be rewritten on save, as a Transaction rapidly changes its
 * state due to their nature of moving all the time in it's lifespan.</p>
 *
 * <p>A transaction is only being synced towards the client if the chunk is being loaded or if
 * a new one is being made. The rest happens on each side isolated, saving on network traffic.</p>
 *
 * @param <P> The product that is being transferred.
 * @param <T> Self reference.
 */
public abstract class Transaction<P, T extends Transaction<P, T>> implements INBTSerializable<CompoundTag> {

    public P product;
    public Direction origin;
    public BlockPos blockPos;
    public boolean leaving;

    /**
     * Used during registration as singleton.
     */
    public Transaction() {}

    /**
     * Creates a new and empty instance.
     * Used during serialization where the class is unknown.
     */
    public abstract T createEmpty();

    /**
     * Will be called when the transaction reaches
     * its destination and before the transaction is being dropped.
     *
     * @return Whenever it was successful or not
     */
    public abstract boolean unwind(Level level);

    /**
     * Will be called if the container
     * it's inside gets moved or destroyed.
     */
    public abstract void terminate(Level level);

    /**
     * The unique type of this kind of transaction.
     * Must match the registered ResourceLocation.
     */
    public abstract ResourceLocation type();

    public abstract Codec<P> codec();

    @Override
    public final @UnknownNullability CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag data = new CompoundTag();
        DataResult<Tag> result = codec().encodeStart(NbtOps.INSTANCE, product);

        data.put("product", result.getOrThrow());
        data.putString("origin", origin.toString());
        data.putIntArray("blockPos", new int[]{blockPos.getX(), blockPos.getY(), blockPos.getZ()});
        data.putBoolean("leaving", leaving);

        return data;
    }

    @Override
    public final void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag data) {
        DataResult<Pair<P, Tag>> result = codec().decode(NbtOps.INSTANCE, data.get("product"));
        product = result.getOrThrow().getFirst();
        origin = Direction.byName(data.getString("origin"));
        int[] posArray = data.getIntArray("blockPos");
        blockPos = new BlockPos(posArray[0], posArray[1], posArray[2]);
        leaving = data.getBoolean("leaving");
    }
}
