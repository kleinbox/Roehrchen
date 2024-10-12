package dev.kleinbox.roehrchen.feature.pipe.glass;

import dev.kleinbox.roehrchen.Registries;
import net.minecraft.world.item.BlockItem;

public class GlassPipeItem extends BlockItem {
    public static final Properties PROPERTIES = new Properties();

    public GlassPipeItem() {
        super(Registries.GLASS_PIPE_BLOCK.get(), PROPERTIES);
    }
}
