package dev.kleinbox.roehrchen;

import com.mojang.logging.LogUtils;
import dev.kleinbox.roehrchen.api.RoehrchenRegistries;
import dev.kleinbox.roehrchen.common.core.Config;
import dev.kleinbox.roehrchen.common.core.tracker.TransactionTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(Roehrchen.MOD_ID)
public class Roehrchen {
    public static final String MOD_ID = "roehrchen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Registries REGISTERED = new Registries();

    public Roehrchen(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(RoehrchenRegistries::registerRegistries);
        Registries.registerToBus(modEventBus);
        modEventBus.addListener(Registries::registerCapabilities);
        modEventBus.addListener(Registries::registerPayloads);
        //NeoForge.EVENT_BUS.register(ClientModEvents.class);

        NeoForge.EVENT_BUS.register(new TransactionTracker());
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}