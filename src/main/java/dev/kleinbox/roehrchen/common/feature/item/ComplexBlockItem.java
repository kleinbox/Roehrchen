package dev.kleinbox.roehrchen.common.feature.item;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static dev.kleinbox.roehrchen.Roehrchen.MOD_ID;

public class ComplexBlockItem<T extends Block> extends BlockItem {
    public ComplexBlockItem(T block, Properties properties) {
        super(block, properties);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(MOD_ID + (Screen.hasShiftDown() ? ".item.description.full" : ".item.description.small")));

        if (Screen.hasShiftDown())
            tooltipComponents.add(Component.translatable(stack.getDescriptionId() + ".description"));
    }
}
