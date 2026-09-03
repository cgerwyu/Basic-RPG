package net.cgerwyu.basicrpgclasses.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class ItemLore {
    public static void append(String id, Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable("lore.basicrpgclasses." + id + ".flavor")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("lore.basicrpgclasses." + id + ".source")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private ItemLore() {
    }
}
