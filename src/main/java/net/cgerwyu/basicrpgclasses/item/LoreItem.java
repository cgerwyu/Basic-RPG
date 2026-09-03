package net.cgerwyu.basicrpgclasses.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class LoreItem extends Item {
    private final String loreId;

    public LoreItem(String loreId, Properties properties) {
        super(properties);
        this.loreId = loreId;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        ItemLore.append(loreId, tooltip);
    }
}
