package dev.kleinbox.roehrchen.common.core.tracker;

import dev.kleinbox.roehrchen.Roehrchen;
import dev.kleinbox.roehrchen.api.RoehrchenRegistries;
import dev.kleinbox.roehrchen.api.Transaction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkTransactionsAttachment implements INBTSerializable<ListTag>, Iterable<Transaction<?,?>> {
    public static final Object PRESENT = new Object();

    private ConcurrentHashMap<Transaction<?,?>, Object> transactions = new ConcurrentHashMap<>();

    public ChunkTransactionsAttachment() { }

    private ChunkTransactionsAttachment(ConcurrentHashMap<Transaction<?, ?>, Object> transactions) {
        this.transactions = transactions;
    }

    public boolean add(Transaction<?,?> transaction) {
        if (transactions.containsKey(transaction))
            return false;

        transactions.put(transaction, PRESENT);
        return true;
    }

    public void remove(Transaction<?,?> transaction) {
        transactions.remove(transaction);
    }

    @Nullable
    public Transaction<?,?> getSingle() {
        if (transactions.size() == 1)
            return transactions.keySet().iterator().next();

        return null;
    }

    public static ChunkTransactionsAttachment fromRaw(ConcurrentHashMap<Transaction<?,?>, Object> transactions) {
        return new ChunkTransactionsAttachment(transactions);
    }

    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    public ConcurrentHashMap<Transaction<?,?>, Object> getTransactions() {
        return transactions;
    }

    @Override
    public @NotNull Iterator<Transaction<?, ?>> iterator() {
        return transactions.keySet().iterator();
    }

    @Override
    public ListTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        ListTag tags = new ListTag();

        for (Transaction<?, ?> transaction : transactions.keySet()) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putString("type", transaction.type().toString());
            compoundTag.put("data", transaction.serializeNBT(provider));

            tags.add(compoundTag);
        }

        return tags;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull ListTag tags) {
        transactions = new ConcurrentHashMap<>();

        for (int i=0; i < tags.size(); i++) {
            CompoundTag compoundTag = tags.getCompound(i);
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
            transaction.deserializeNBT(provider, compoundTag.getCompound("data"));

            transactions.put(transaction, PRESENT);
        }
    }
}
