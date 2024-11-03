package dev.kleinbox.roehrchen.common.core.payload;

import dev.kleinbox.roehrchen.api.RoehrchenRegistries;
import dev.kleinbox.roehrchen.api.Transaction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import static dev.kleinbox.roehrchen.Roehrchen.LOGGER;
import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

/**
 * @param transaction Can be null on client side in case it cannot be properly decoded.
 */
public record AnnounceTransactionPayload(Transaction<?,?> transaction) implements CustomPacketPayload {

    public static void handleClientDataOnMain(final AnnounceTransactionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            LOGGER.debug("Rendering new transaction");
            //Minecraft instance = Minecraft.getInstance();
        });
    }

    public static final StreamCodec<FriendlyByteBuf, AnnounceTransactionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(@NotNull FriendlyByteBuf byteBuf, @NotNull AnnounceTransactionPayload payload) {
            Transaction<?, ?> transaction = payload.transaction;

            byteBuf.writeResourceLocation(transaction.type());
            byteBuf.writeNbt(transaction.toNBT());
        }

        @Override
        @NotNull
        public AnnounceTransactionPayload decode(@NotNull FriendlyByteBuf byteBuf) {
            ResourceLocation type = byteBuf.readResourceLocation();
            CompoundTag compoundTag = byteBuf.readNbt();

            if (compoundTag == null)
                throw new IllegalStateException("Received transaction is empty or invalid");

            Transaction<?, ?> singleton = RoehrchenRegistries.TRANSACTION_REGISTRY.get(type);
            if (singleton == null)
                throw new IllegalStateException("Requested transaction type " + type + " is unregistered");

            Transaction<?, ?> transaction = singleton.createEmpty();
            transaction.fromNBT(compoundTag);

            return new AnnounceTransactionPayload(transaction);
        }
    };

    public static final Type<AnnounceTransactionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "announce_new_transactions"));

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
