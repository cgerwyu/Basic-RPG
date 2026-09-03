package net.cgerwyu.basicrpgclasses.client.hud;

import net.cgerwyu.basicrpgclasses.client.CombatModeController;
import net.cgerwyu.basicrpgclasses.client.ClientToggleSkillStates;
import net.cgerwyu.basicrpgclasses.client.ClientCastState;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.ClassResourceRules;
import net.cgerwyu.basicrpgclasses.data.PlayerClassData;
import net.cgerwyu.basicrpgclasses.data.PlayerCombatData;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinition;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinitions;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.cgerwyu.basicrpgclasses.skill.SkillScaling;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Locale;

public final class RpgHud {
    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gui.hud.isHidden()) {
            return;
        }

        renderPlayerFrame(graphics, minecraft, player);
        renderTargetFrame(graphics, minecraft);
        if (CombatModeController.active()) {
            renderLockedItemHotbar(graphics, minecraft);
            renderSkillBar(graphics, minecraft);
            renderCastBar(graphics, minecraft);
        }
    }

    private static void renderPlayerFrame(GuiGraphicsExtractor graphics, Minecraft minecraft, LocalPlayer player) {
        PlayerClassData data = player.getData(ModAttachments.PLAYER_CLASS);
        PlayerCombatData combat = player.getData(ModAttachments.PLAYER_COMBAT);
        int x = 8;
        int y = 8;
        int width = 152;
        int height = 50;
        graphics.fill(x, y, x + width, y + height, 0xB0101018);
        graphics.outline(x, y, width, height, 0xCC8B734B);
        PlayerFaceExtractor.extractRenderState(graphics, player.getSkin(), x + 5, y + 7, 34);
        graphics.text(minecraft.font, player.getName(), x + 44, y + 4, 0xFFFFFFFF);
        graphics.text(
                minecraft.font,
                Component.translatable("hud.basicrpgclasses.class_level", data.classLevel()),
                x + 44,
                y + 14,
                0xFFC9B98D
        );

        float healthRatio = Math.clamp(player.getHealth() / Math.max(1.0F, player.getMaxHealth()), 0.0F, 1.0F);
        long gameTime = player.level().getGameTime();
        drawAnimatedBar(graphics, x + 44, y + 26, 101, 8, healthRatio, 0xFF8E2222, 0xFF2A1111, 0xFFE95C5C, gameTime, 0);
        graphics.text(minecraft.font, Math.round(player.getHealth()) + " / " + Math.round(player.getMaxHealth()), x + 47, y + 26, 0xFFFFFFFF, false);

        int maximumResource = ClassResourceRules.maxResource(data);
        float resourceRatio = Math.clamp(combat.manaTenths() / (float) Math.max(1, maximumResource * 10), 0.0F, 1.0F);
        int resourceHighlight = ClassResourceRules.usesFury(data.rpgClass()) ? 0xFFFFB14A : 0xFF82E6FF;
        drawAnimatedBar(
                graphics,
                x + 44,
                y + 38,
                101,
                8,
                resourceRatio,
                ClassResourceRules.barColor(data.rpgClass()),
                ClassResourceRules.barBackground(data.rpgClass()),
                resourceHighlight,
                gameTime,
                7
        );
        graphics.text(minecraft.font, Component.translatable(
                "hud.basicrpgclasses.resource",
                Component.translatable(ClassResourceRules.nameTranslationKey(data.rpgClass())),
                combat.resource(),
                maximumResource
        ), x + 47, y + 38, 0xFFD8EBFF, false);
    }

    private static void renderSkillBar(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
        PlayerCombatData combat = minecraft.player.getData(ModAttachments.PLAYER_COMBAT);
        long gameTime = minecraft.player.level().getGameTime();

        int slot = 30;
        int gap = 2;
        int totalWidth = slot * CombatModeController.SKILL_SLOT_COUNT
                + gap * (CombatModeController.SKILL_SLOT_COUNT - 1);
        int startX = minecraft.getWindow().getGuiScaledWidth() / 2 - totalWidth / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() - 58;

        for (int index = 0; index < CombatModeController.SKILL_SLOT_COUNT; index++) {
            int x = startX + index * (slot + gap);
            boolean selected = index == CombatModeController.selectedSkillSlot();
            SkillId skillId = data.skillAtSlot(index);
            SkillDefinition definition = SkillDefinitions.get(skillId);

            graphics.fill(x, y, x + slot, y + slot, selected ? 0xE0483824 : 0xD016161C);
            graphics.outline(x, y, slot, slot, selected ? 0xFFFFD36A : 0xFF777777);
            graphics.text(minecraft.font, Integer.toString(index + 1), x + 2, y + 2, selected ? 0xFFFFFFFF : 0xFFAAAAAA, false);

            if (definition == null) {
                graphics.centeredText(minecraft.font, "—", x + slot / 2, y + 11, 0xFF666666);
                continue;
            }

            int rank = data.skillRank(skillId);
            graphics.fill(x + 1, y + 1, x + slot - 1, y + 4, definition.color());
            if (ClientToggleSkillStates.active(skillId)) {
                drawActiveToggle(graphics, x, y, slot, gameTime);
            }
            graphics.centeredText(
                    minecraft.font,
                    Component.translatable(skillId.shortTranslationKey()),
                    x + slot / 2,
                    y + 9,
                    0xFFFFFFFF
            );

            int maxCharges = definition.maxCharges(rank);
            boolean creative = minecraft.player.getAbilities().instabuild;
            long remainingTicks = creative ? 0L : combat.remainingRechargeTicks(definition, rank, gameTime);
            int availableCharges = creative
                    ? maxCharges
                    : combat.availableCharges(definition, rank, gameTime);
            if (remainingTicks > 0L) {
                drawCooldownRadar(
                        graphics,
                        minecraft,
                        x,
                        y,
                        slot,
                        remainingTicks,
                        definition.cooldownTicks(rank),
                        availableCharges == 0
                );
            }

            if (maxCharges > 1) {
                graphics.text(
                        minecraft.font,
                        availableCharges + "/" + maxCharges,
                        x + 2,
                        y + slot - 10,
                        availableCharges > 0 ? 0xFFFFFFFF : 0xFFAAAAAA,
                        false
                );
            }

            int displayedCost = switch (skillId) {
                case FROST_ARROWS -> SkillScaling.frostArrowManaCost(rank);
                case MULTISHOT -> SkillScaling.multishotManaCost(rank);
                case DIVINE_SLASH -> SkillScaling.divineSlashManaCost(rank);
                default -> definition.manaCost(rank);
            };
            String manaCost = Integer.toString(displayedCost);
            int manaColor = combat.canAfford(displayedCost)
                    ? ClassResourceRules.usesFury(data.rpgClass()) ? 0xFFFFA35C : 0xFF64B5FF
                    : 0xFFFF5555;
            graphics.text(
                    minecraft.font,
                    manaCost,
                    x + slot - minecraft.font.width(manaCost) - 2,
                    y + slot - 10,
                    manaColor,
                    false
            );

            float flash = SkillHudAnimations.readyFlash(skillId, availableCharges);
            if (flash > 0.0F) {
                drawReadyFlash(graphics, x, y, slot, flash);
            }
        }
    }

    private static void renderCastBar(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        if (!ClientCastState.active()) {
            return;
        }
        int width = 184;
        int height = 13;
        int x = minecraft.getWindow().getGuiScaledWidth() / 2 - width / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() - 78;
        float progress = Math.clamp(ClientCastState.progress(), 0.0F, 1.0F);
        graphics.fill(x, y, x + width, y + height, 0xD0101018);
        graphics.fill(x + 2, y + 2, x + 2 + Math.round((width - 4) * progress), y + height - 2, 0xFFDFB84B);
        graphics.outline(x, y, width, height, 0xFFFFE7A0);
        String seconds = String.format(Locale.ROOT, "%.1f", ClientCastState.remainingTicks() / 20.0);
        Component label = Component.translatable("hud.basicrpgclasses.casting",
                Component.translatable(ClientCastState.skill().translationKey()), seconds);
        graphics.centeredText(minecraft.font, label, x + width / 2, y + 2, 0xFFFFFFFF);
    }

    private static void drawCooldownRadar(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            int x,
            int y,
            int size,
            long remainingTicks,
            int totalTicks,
            boolean locked
    ) {
        graphics.fill(x + 1, y + 4, x + size - 1, y + size - 1, locked ? 0xA8000000 : 0x48000000);
        double ratio = Math.clamp(remainingTicks / (double) Math.max(1, totalTicks), 0.0, 1.0);
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int radius = size / 2 - 3;
        int segmentCount = 16;
        int activeSegments = (int) Math.ceil(ratio * segmentCount);

        for (int segment = 0; segment < activeSegments; segment++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * segment / segmentCount;
            int markerX = centerX + (int) Math.round(Math.cos(angle) * radius);
            int markerY = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(markerX - 1, markerY - 1, markerX + 1, markerY + 1, 0xFF69A9D8);
        }

        double handAngle = -Math.PI / 2.0 + Math.PI * 2.0 * (1.0 - ratio);
        for (int step = 1; step <= radius; step++) {
            int handX = centerX + (int) Math.round(Math.cos(handAngle) * step);
            int handY = centerY + (int) Math.round(Math.sin(handAngle) * step);
            graphics.fill(handX, handY, handX + 1, handY + 1, 0xFFE6F4FF);
        }

        String seconds = String.format(Locale.ROOT, "%.1f", Math.ceil(remainingTicks / 2.0) / 10.0);
        graphics.centeredText(minecraft.font, seconds, centerX, centerY - 4, 0xFFFFFFFF);
    }

    private static void drawReadyFlash(GuiGraphicsExtractor graphics, int x, int y, int size, float flash) {
        float pulse = 0.55F + 0.45F * (float) Math.sin(flash * Math.PI * 8.0F);
        int alpha = Math.clamp(Math.round(255.0F * flash * pulse), 0, 255);
        int color = alpha << 24 | 0x00FFF0A0;
        graphics.outline(x - 1, y - 1, size + 2, size + 2, color);
        graphics.fill(x - 2, y - 2, x + 2, y + 2, color);
        graphics.fill(x + size - 2, y - 2, x + size + 2, y + 2, color);
        graphics.fill(x - 2, y + size - 2, x + 2, y + size + 2, color);
        graphics.fill(x + size - 2, y + size - 2, x + size + 2, y + size + 2, color);
    }

    private static void drawActiveToggle(GuiGraphicsExtractor graphics, int x, int y, int size, long gameTime) {
        float pulse = 0.55F + 0.45F * (float) Math.sin(gameTime * 0.32);
        int alpha = 150 + Math.round(90.0F * pulse);
        int glow = alpha << 24 | 0x006FEAFF;
        graphics.outline(x - 2, y - 2, size + 4, size + 4, glow);
        graphics.outline(x - 1, y - 1, size + 2, size + 2, 0xFFE6FFFF);
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int radius = size / 2 + 2;
        double phase = gameTime * 0.18;
        for (int marker = 0; marker < 8; marker++) {
            double angle = phase + marker * Math.PI / 4.0;
            int markerX = centerX + (int) Math.round(Math.cos(angle) * radius);
            int markerY = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(markerX - 1, markerY - 1, markerX + 1, markerY + 1, 0xFFB8F4FF);
        }
    }

    private static void renderLockedItemHotbar(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        int width = 182;
        int height = 22;
        int x = minecraft.getWindow().getGuiScaledWidth() / 2 - width / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() - height;

        graphics.fill(x, y, x + width, y + height, 0xB8000000);
        graphics.outline(x, y, width, height, 0xCC555555);
    }

    private static void renderTargetFrame(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        if (minecraft.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            int width = 190;
            int height = 48;
            int x = minecraft.getWindow().getGuiScaledWidth() / 2 - width / 2;
            int y = 8;
            graphics.fill(x, y, x + width, y + height, 0xB0101018);
            graphics.outline(x, y, width, height, 0xCC8B734B);
            if (living instanceof AbstractClientPlayer targetPlayer) {
                PlayerFaceExtractor.extractRenderState(graphics, targetPlayer.getSkin(), x + 7, y + 8, 32);
            } else {
                InventoryScreen.renderEntityInInventoryFollowsAngle(
                        graphics,
                        x + 2,
                        y + 2,
                        x + 44,
                        y + 46,
                        18,
                        0.0F,
                        0.15F,
                        0.0F,
                        living
                );
            }
            graphics.text(minecraft.font, living.getName(), x + 50, y + 8, 0xFFFFFFFF);
            float ratio = Math.clamp(living.getHealth() / Math.max(1.0F, living.getMaxHealth()), 0.0F, 1.0F);
            drawAnimatedBar(graphics, x + 50, y + 25, width - 58, 9, ratio, 0xFF8E2222, 0xFF2A1111, 0xFFE95C5C, living.level().getGameTime(), 3);
            graphics.centeredText(minecraft.font, Math.round(living.getHealth()) + " / " + Math.round(living.getMaxHealth()), x + 50 + (width - 58) / 2, y + 25, 0xFFFFFFFF);
        } else if (minecraft.hitResult instanceof BlockHitResult blockHit && minecraft.level != null) {
            var state = minecraft.level.getBlockState(blockHit.getBlockPos());
            if (state.isAir()) {
                return;
            }
            ItemStack icon = new ItemStack(state.getBlock().asItem());
            int width = 150;
            int x = minecraft.getWindow().getGuiScaledWidth() / 2 - width / 2;
            int y = 8;
            graphics.fill(x, y, x + width, y + 26, 0xA0101018);
            graphics.outline(x, y, width, 26, 0xBB777777);
            if (!icon.isEmpty()) {
                graphics.item(icon, x + 6, y + 5);
            }
            graphics.text(minecraft.font, state.getBlock().getName(), x + 27, y + 9, 0xFFFFFFFF);
        }
    }

    private static void drawAnimatedBar(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float ratio,
            int color,
            int background,
            int highlight,
            long gameTime,
            int phaseOffset
    ) {
        graphics.fill(x, y, x + width, y + height, background);
        int fillWidth = Math.clamp(Math.round(width * ratio), 0, width);
        graphics.fill(x, y, x + fillWidth, y + height, color);
        int phase = Math.floorMod((int) (gameTime / 2L) + phaseOffset, 14);
        for (int stripeX = x - phase; stripeX < x + fillWidth; stripeX += 14) {
            int stripeStart = Math.max(x, stripeX);
            int stripeEnd = Math.min(x + fillWidth, stripeX + 3);
            if (stripeEnd > stripeStart) {
                graphics.fill(stripeStart, y + 1, stripeEnd, y + 3, highlight);
            }
        }
        graphics.outline(x, y, width, height, 0xCC000000);
    }

    private RpgHud() {
    }
}
