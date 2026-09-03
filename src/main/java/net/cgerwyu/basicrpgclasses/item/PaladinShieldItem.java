package net.cgerwyu.basicrpgclasses.item;

import net.cgerwyu.basicrpgclasses.weapon.WeaponProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class PaladinShieldItem extends ShieldItem implements ProfiledWeapon {
    private final WeaponProfile profile;
    private final String loreId;

    public PaladinShieldItem(WeaponProfile profile, String loreId, Properties properties) {
        super(properties);
        this.profile = profile;
        this.loreId = loreId;
    }

    @Override
    public WeaponProfile profile() {
        return profile;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        ItemLore.append(loreId, tooltip);
    }
}
