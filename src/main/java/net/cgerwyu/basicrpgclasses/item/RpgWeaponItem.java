package net.cgerwyu.basicrpgclasses.item;

import net.cgerwyu.basicrpgclasses.weapon.WeaponProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class RpgWeaponItem extends Item implements ProfiledWeapon {
    private final WeaponProfile profile;
    private final String loreId;

    public RpgWeaponItem(WeaponProfile profile, Properties properties) {
        this(profile, properties, null);
    }

    public RpgWeaponItem(WeaponProfile profile, Properties properties, String loreId) {
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
        if (loreId != null) {
            ItemLore.append(loreId, tooltip);
        }
    }
}
