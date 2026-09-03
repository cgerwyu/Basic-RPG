package net.cgerwyu.basicrpgclasses.item;

import net.cgerwyu.basicrpgclasses.equipment.AccessoryType;
import net.cgerwyu.basicrpgclasses.equipment.RpgAccessory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class AccessoryItem extends Item implements RpgAccessory {
    private final AccessoryType accessoryType;
    private final String loreId;

    public AccessoryItem(AccessoryType accessoryType, String loreId, Properties properties) {
        super(properties);
        this.accessoryType = accessoryType;
        this.loreId = loreId;
    }

    @Override
    public AccessoryType accessoryType() {
        return accessoryType;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        ItemLore.append(loreId, tooltip);
    }
}
