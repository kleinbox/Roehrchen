package dev.kleinbox.roehrchen.common.core.payload;

import dev.kleinbox.roehrchen.Roehrchen;
import dev.kleinbox.roehrchen.api.RoehrchenRegistries;
import dev.kleinbox.roehrchen.api.Transaction;
import dev.kleinbox.roehrchen.common.core.tracker.ChunkTransactionsAttachment;
import dev.kleinbox.roehrchen.common.core.tracker.TransactionTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;
import static dev.kleinbox.roehrchen.Roehrchen.REGISTERED;

/**
 * Synchronises the transactions of a chunk with a player client.
 *
 * @param chunkPos The chunk to override or extend with the new transactions.
 * @param transactions The transactions this chunk is supposed to have.
 */
public record ChunkTransactionsPayload(ChunkPos chunkPos, ConcurrentHashMap<Transaction<?, ?>, Object> transactions) implements CustomPacketPayload {

    public static void handleClientDataOnMain(final ChunkTransactionsPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft instance = Minecraft.getInstance();
            ClientLevel level = instance.level;
            if (level == null)
                return;

            ChunkAccess chunk = level.getChunk(payload.chunkPos.x, payload.chunkPos.z);
            chunk.setData(REGISTERED.CHUNK_TRANSACTIONS, ChunkTransactionsAttachment.fromRaw(payload.transactions));

            TransactionTracker.makeLevelAwareOfChunk(chunk);
        });
    }

    public static final Type<ChunkTransactionsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sync_chunk_transactions"));

    public static final StreamCodec<FriendlyByteBuf, ChunkTransactionsPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf byteBuf, ChunkTransactionsPayload payload) {
            byteBuf.writeChunkPos(payload.chunkPos);
            byteBuf.writeInt(payload.transactions.size());

            for (Transaction<?, ?> transaction : payload.transactions.keySet()) {
                CompoundTag compoundTag = new CompoundTag();

                compoundTag.putString("type", transaction.type().toString());
                compoundTag.put("data", transaction.toNBT());

                byteBuf.writeNbt(compoundTag);
            }
        }

        @Override
        public @NotNull ChunkTransactionsPayload decode(FriendlyByteBuf byteBuf) {
            ConcurrentHashMap<Transaction<?,?>, Object> transactions = new ConcurrentHashMap<>();

            ChunkPos chunkPos = byteBuf.readChunkPos();
            int size = byteBuf.readInt();

            for (int i=0; i<size; i++) {
                CompoundTag compoundTag = byteBuf.readNbt();
                if (compoundTag == null)
                    break;

                ResourceLocation type = ResourceLocation.tryParse(compoundTag.getString("type"));

                if (type == null) {
                    Roehrchen.LOGGER.error("Cannot deserialize transaction with invalid type for tag {}; Skipping", compoundTag);
                    continue;
                }

                Transaction<?, ?> singleton = RoehrchenRegistries.TRANSACTION_REGISTRY.get(type);
                if (singleton == null) {
                    Roehrchen.LOGGER.error("Found transaction of unregistered type {}; Skipping", type);
                    continue;
                }

                Transaction<?, ?> transaction = singleton.createEmpty();
                transaction.fromNBT(compoundTag.getCompound("data"));

                transactions.put(transaction, ChunkTransactionsAttachment.PRESENT);
            }

            return new ChunkTransactionsPayload(chunkPos, transactions);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
