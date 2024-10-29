package dev.kleinbox.roehrchen.api;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

public class RoehrchenRegistries {
    public static final ResourceKey<Registry<Transaction<?, ?>>> TRANSACTION_REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(MOD_ID, "transaction"));

    public static final Registry<Transaction<?, ?>> TRANSACTION_REGISTRY =
            new RegistryBuilder<>(TRANSACTION_REGISTRY_KEY).create();

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(TRANSACTION_REGISTRY);
    }
}
