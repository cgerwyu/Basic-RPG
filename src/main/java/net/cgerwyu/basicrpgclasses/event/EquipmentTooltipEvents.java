package net.cgerwyu.basicrpgclasses.event;

import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.RpgClass;
import net.cgerwyu.basicrpgclasses.equipment.EquipmentDescriptor;
import net.cgerwyu.basicrpgclasses.equipment.EquipmentRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = BasicRPGClasses.MODID)
public final class EquipmentTooltipEvents {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        EquipmentDescriptor descriptor = EquipmentRules.describe(event.getItemStack());
        if (descriptor == null) {
            return;
        }

        int insertion = Math.min(1, event.getToolTip().size());
        MutableComponent type = Component.translatable(descriptor.typeTranslationKey());
        if (descriptor.kind() == EquipmentDescriptor.Kind.WEAPON) {
            String handednessKey = EquipmentRules.handednessTranslationKey(descriptor);
            if (handednessKey != null) {
                type.append(", ").append(Component.translatable(handednessKey));
            }
        }
        event.getToolTip().add(insertion++, Component.translatable(
                "tooltip.basicrpgclasses.equipment_type", type.withStyle(ChatFormatting.GRAY)
        ).withStyle(ChatFormatting.DARK_GRAY));

        if (descriptor.kind() == EquipmentDescriptor.Kind.ARMOR) {
            MutableComponent armor = Component.translatable(descriptor.armorWeight().translationKey());
            if (descriptor.holy()) {
                armor.append(", ").append(Component.translatable("tooltip.basicrpgclasses.armor.holy"));
            }
            event.getToolTip().add(insertion++, Component.translatable(
                    "tooltip.basicrpgclasses.armor_type", armor.withStyle(ChatFormatting.GRAY)
            ).withStyle(ChatFormatting.DARK_GRAY));
        }

        event.getToolTip().add(insertion, Component.translatable(
                "tooltip.basicrpgclasses.required_class",
                classNames(descriptor).withStyle(classColor(event.getEntity(), descriptor))
        ).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static MutableComponent classNames(EquipmentDescriptor descriptor) {
        if (descriptor.allowedClasses().size() == 5) {
            return Component.translatable("tooltip.basicrpgclasses.all_classes");
        }

        MutableComponent result = Component.empty();
        boolean first = true;
        for (RpgClass rpgClass : RpgClass.values()) {
            if (!descriptor.allowedClasses().contains(rpgClass)) {
                continue;
            }
            if (!first) {
                result.append(", ");
            }
            result.append(Component.translatable(rpgClass.translationKey()));
            first = false;
        }
        return result;
    }

    private static ChatFormatting classColor(Player player, EquipmentDescriptor descriptor) {
        if (player == null) {
            return ChatFormatting.GRAY;
        }
        RpgClass actual = player.getData(ModAttachments.PLAYER_CLASS).rpgClass();
        return descriptor.allowedClasses().contains(actual) ? ChatFormatting.GREEN : ChatFormatting.RED;
    }

    private EquipmentTooltipEvents() {
    }
}
