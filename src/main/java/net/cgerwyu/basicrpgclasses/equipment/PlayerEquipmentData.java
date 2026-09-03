package net.cgerwyu.basicrpgclasses.equipment;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record PlayerEquipmentData(List<ItemStack> stacks) {
    public static final MapCodec<PlayerEquipmentData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("slots", List.of()).forGetter(PlayerEquipmentData::stacks)
    ).apply(instance, PlayerEquipmentData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerEquipmentData> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_LIST_STREAM_CODEC,
            PlayerEquipmentData::stacks,
            PlayerEquipmentData::new
    );

    public PlayerEquipmentData {
        stacks = normalize(stacks);
    }

    public static PlayerEquipmentData empty() {
        return new PlayerEquipmentData(List.of());
    }

    public ItemStack get(RpgEquipmentSlotType slotType) {
        return stacks.get(slotType.index());
    }

    public PlayerEquipmentData with(RpgEquipmentSlotType slotType, ItemStack stack) {
        List<ItemStack> updated = new ArrayList<>(stacks);
        updated.set(slotType.index(), normalizeStack(stack));
        return new PlayerEquipmentData(updated);
    }

    public PlayerEquipmentData copied() {
        return new PlayerEquipmentData(stacks.stream().map(ItemStack::copy).toList());
    }

    private static List<ItemStack> normalize(List<ItemStack> source) {
        List<ItemStack> normalized = new ArrayList<>(RpgEquipmentSlotType.COUNT);
        for (int index = 0; index < RpgEquipmentSlotType.COUNT; index++) {
            normalized.add(index < source.size() ? normalizeStack(source.get(index)) : ItemStack.EMPTY);
        }
        return List.copyOf(normalized);
    }

    private static ItemStack normalizeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.getCount() == 1 ? stack : stack.copyWithCount(1);
    }
}
