package dev.kleinbox.roehrchen;

import dev.kleinbox.roehrchen.core.GlassPipeNetwork;
import net.minecraft.server.level.ServerChunkCache;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(Initialization.MODID)
public class Initialization {
    public static final String MODID = "roehrchen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Initialization(IEventBus modEventBus, ModContainer modContainer) {
        // Register features
        Registries.BLOCKS.register(modEventBus);
        Registries.ITEMS.register(modEventBus);

        // Register ModConfigSpec
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Networks
        GlassPipeNetwork glassPipeNetwork = new GlassPipeNetwork();
        NeoForge.EVENT_BUS.register(glassPipeNetwork);
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
        }
    }
}