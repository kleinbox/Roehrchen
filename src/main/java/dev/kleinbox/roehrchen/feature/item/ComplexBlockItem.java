package dev.kleinbox.roehrchen.feature.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class ComplexBlockItem<T extends Block> extends BlockItem {
    public ComplexBlockItem(T block, Properties properties) {
        super(block, properties);
    }
}
