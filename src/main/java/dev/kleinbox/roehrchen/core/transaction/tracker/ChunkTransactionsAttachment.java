package dev.kleinbox.roehrchen.core.transaction.tracker;

import dev.kleinbox.roehrchen.Roehrchen;
import dev.kleinbox.roehrchen.api.RoehrchenRegistries;
import dev.kleinbox.roehrchen.api.Transaction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Iterator;

public class ChunkTransactionsAttachment implements INBTSerializable<ListTag>, Iterable<Transaction<?,?>> {

    private HashSet<Transaction<?,?>> transactions = new HashSet<>();

    public boolean add(Transaction<?,?> transaction) {
        return transactions.add(transaction);
    }

    public boolean remove(Transaction<?,?> transaction) {
        return transactions.remove(transaction);
    }

    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    @Override
    public @NotNull Iterator<Transaction<?, ?>> iterator() {
        return transactions.iterator();
    }

    @Override
    public ListTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        ListTag tags = new ListTag();

        for (Transaction<?, ?> transaction : transactions) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putString("type", transaction.type().toString());
            compoundTag.put("data", transaction.serializeNBT(provider));

            tags.add(compoundTag);
        }
        
        return tags;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull ListTag tags) {
        transactions = new HashSet<>();

        for (int i = 0; i < tags.size(); i++) {
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

            transactions.add(transaction);
        }
    }
}
