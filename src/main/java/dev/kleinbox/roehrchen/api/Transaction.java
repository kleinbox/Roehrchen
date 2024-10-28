package dev.kleinbox.roehrchen.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.kleinbox.roehrchen.core.tracker.TransactionTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

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
public abstract class Transaction<P, T extends Transaction<P, T>> {
    public static Codec<Transaction<?, ?>> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("type").forGetter(Transaction::type),
                    Codec.STRING.fieldOf("product").forGetter(Transaction::serializeProduct),
                    Direction.CODEC.fieldOf("origin").forGetter(t -> t.origin),
                    BlockPos.CODEC.fieldOf("blockPos").forGetter(t -> t.blockPos),
                    Codec.BOOL.fieldOf("leaving").forGetter(t -> t.leaving)
            ).apply(instance, (Transaction::load))
    );

    private static Transaction<?, ?> load(ResourceLocation type, String serializedProduct, Direction origin,
                                          BlockPos blockPos, Boolean leaving) {
        Transaction<?, ?> transaction = RoehrchenRegistries.TRANSACTION_REGISTRY.get(type);
        if (transaction == null)
            return null;

        Codec<?> codec = transaction.codec();

        JsonElement json = JsonParser.parseString(serializedProduct);
        DataResult<? extends Pair<?, JsonElement>> result = codec.decode(JsonOps.COMPRESSED, json);

        return transaction.with(result.getOrThrow().getFirst(), origin, blockPos, leaving);
    }

    private static <SHARED> String serializeProduct(Transaction<SHARED, ?> transaction) {
        Codec<SHARED> codec = transaction.codec();
        DataResult<JsonElement> result = codec.encodeStart(JsonOps.COMPRESSED, transaction.product);

        return result.getOrThrow().getAsString();
    }

    public P product;
    public Direction origin;
    public BlockPos blockPos;
    public boolean leaving;

    /**
     * Used during registration as singleton.
     */
    public Transaction() {}

    /**
     * Tries to create a new instance.
     *
     * @param potentialProduct The product to transfer.
     * @param origin The direction it came from.
     * @param blockPos The position in the world.
     * @param leaving Whenever the product is inside the block or about to leave.
     */
    @Nullable
    public abstract T with(Object potentialProduct, Direction origin, BlockPos blockPos, boolean leaving);

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
}
