package net.cgerwyu.basicrpgclasses.client.hud;

import net.cgerwyu.basicrpgclasses.client.ClientCastState;
import net.cgerwyu.basicrpgclasses.client.ClientToggleSkillStates;
import net.cgerwyu.basicrpgclasses.client.CombatModeController;
import net.cgerwyu.basicrpgclasses.data.ClassResourceRules;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.PlayerClassData;
import net.cgerwyu.basicrpgclasses.data.PlayerCombatData;
import net.cgerwyu.basicrpgclasses.party.PartyService;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinition;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinitions;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.cgerwyu.basicrpgclasses.skill.SkillScaling;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** RPG HUD rendered from the supplied 256x256 gothic silver texture atlas. */
public final class RpgHud {
    private static final Identifier GOTHIC_HUD_TEXTURE = Identifier.fromNamespaceAndPath(
            "basicrpgclasses", "textures/gui/gothic_hud.png"
    );
    private static final int GOTHIC_ATLAS_SIZE = 256;
    private static final int PANEL_BACKGROUND = 0xD018171D;
    private static final int PANEL_INNER = 0xD00B0B10;
    private static final int TRIM_SILVER = 0xFFC7C1B8;
    private static final int TRIM_DARK = 0xFF5A5658;
    private static final int TRIM_SHADOW = 0xC6000000;
    private static final int TEXT_PRIMARY = 0xFFF5F1E8;
    private static final int TEXT_MUTED = 0xFFC5BCAD;
    private static final int HEALTH_FILL = 0xFF9D2634;
    private static final int HEALTH_SHINE = 0xFFFF8490;
    private static final int HEALTH_EMPTY = 0xFF2A1015;
    private static final int BOSS_BAR_WIDTH = 148;
    private static final int BOSS_BAR_HEIGHT = 54;

    private static GuiGraphicsExtractor bossGraphics;
    private static int bossBarBottom;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gui.hud.isHidden()) {
            return;
        }

        renderPlayerFrame(graphics, minecraft, player);
        renderPartyFrames(graphics, minecraft, player);
        renderTargetFrame(graphics, minecraft);
        if (CombatModeController.active()) {
            renderLockedItemHotbar(graphics, minecraft);
            renderSkillBar(graphics, minecraft);
            renderCastBar(graphics, minecraft);
        }
    }

    /** Draws one vanilla boss event with the same visual language as the RPG HUD. */
    public static void renderBossBar(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            LerpingBossEvent bossEvent,
            int y
    ) {
        if (bossGraphics != graphics) {
            bossGraphics = graphics;
            bossBarBottom = 0;
        }

        int width = Math.min(BOSS_BAR_WIDTH, graphics.guiWidth() - 12);
        int height = BOSS_BAR_HEIGHT;
        int x = graphics.guiWidth() / 2 - width / 2;
        int bossColor = bossColor(bossEvent.getColor());

        drawAtlasRegion(graphics, x, y, width, height, 98, 157, 148, 54);
        int barX = x + Math.round(width * 31.0F / BOSS_BAR_WIDTH);
        int barY = y + 16;
        int barWidth = Math.max(8, Math.round(width * 102.0F / BOSS_BAR_WIDTH));
        drawInsetBar(graphics, barX, barY, barWidth, 8,
                Math.clamp(bossEvent.getProgress(), 0.0F, 1.0F),
                bossColor, darken(bossColor, 0.22F), brighten(bossColor, 0.60F));
        String percent = Math.round(bossEvent.getProgress() * 100.0F) + "%";
        graphics.centeredText(minecraft.font, percent, barX + barWidth / 2, barY - 1, TEXT_PRIMARY);
        String bossName = fitText(minecraft, bossEvent.getName().getString(), width - 24);
        graphics.centeredText(minecraft.font, bossName, x + width / 2, y + 32, TEXT_PRIMARY);
        bossBarBottom = Math.max(bossBarBottom, y + height);
    }

    private static void renderPlayerFrame(GuiGraphicsExtractor graphics, Minecraft minecraft, LocalPlayer player) {
        PlayerClassData data = player.getData(ModAttachments.PLAYER_CLASS);
        PlayerCombatData combat = player.getData(ModAttachments.PLAYER_COMBAT);
        int x = 6;
        int y = 4;

        int resourceColor = ClassResourceRules.barColor(data.rpgClass());
        drawAtlasRegion(graphics, x, y, 160, 84, 1, 4, 160, 84);
        PlayerFaceExtractor.extractRenderState(graphics, player.getSkin(), x + 15, y + 15, 44);
        drawPlayerPortraitOverlay(graphics, x, y);

        graphics.text(minecraft.font, fitText(minecraft, player.getName().getString(), 80), x + 78, y + 2, TEXT_PRIMARY, false);
        graphics.text(
                minecraft.font,
                Component.translatable("hud.basicrpgclasses.class_level", data.classLevel()),
                x + 78,
                y + 12,
                TEXT_MUTED,
                false
        );

        float healthRatio = Math.clamp(player.getHealth() / Math.max(1.0F, player.getMaxHealth()), 0.0F, 1.0F);
        drawInsetBar(graphics, x + 84, y + 23, 72, 8, healthRatio,
                HEALTH_FILL, HEALTH_EMPTY, HEALTH_SHINE);
        graphics.centeredText(
                minecraft.font,
                Math.round(player.getHealth()) + " / " + Math.round(player.getMaxHealth()),
                x + 120,
                y + 22,
                TEXT_PRIMARY
        );

        int maximumResource = ClassResourceRules.maxResource(data);
        float resourceRatio = Math.clamp(combat.manaTenths() / (float) Math.max(1, maximumResource * 10), 0.0F, 1.0F);
        int resourceHighlight = ClassResourceRules.usesFury(data.rpgClass()) ? 0xFFFFCF7A : 0xFFA9EEFF;
        drawInsetBar(graphics, x + 85, y + 48, 71, 7, resourceRatio,
                resourceColor, ClassResourceRules.barBackground(data.rpgClass()), resourceHighlight);
        graphics.centeredText(
                minecraft.font,
                combat.resource() + " / " + maximumResource,
                x + 120,
                y + 47,
                resourceHighlight
        );
    }

    /** Client combat resources are private, so party cards show only synced class data and HP. */
    private static void renderPartyFrames(GuiGraphicsExtractor graphics, Minecraft minecraft, LocalPlayer player) {
        if (minecraft.level == null || !PartyService.inParty(player)) {
            return;
        }

        List<AbstractClientPlayer> members = minecraft.level.players().stream()
                .filter(member -> member != player)
                .filter(member -> PartyService.sameParty(player, member))
                .sorted(Comparator.comparing(member -> member.getName().getString(), String.CASE_INSENSITIVE_ORDER))
                .limit(4)
                .toList();
        if (members.isEmpty()) {
            return;
        }

        int x = 6;
        int y = 92;
        graphics.text(minecraft.font, Component.translatable("hud.basicrpgclasses.party"), x + 7, y, TEXT_MUTED, false);
        for (int index = 0; index < members.size(); index++) {
            renderPartyMember(graphics, minecraft, members.get(index), x, y + 10 + index * 58);
        }
    }

    private static void renderPartyMember(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            AbstractClientPlayer member,
            int x,
            int y
    ) {
        PlayerClassData data = member.getData(ModAttachments.PLAYER_CLASS);
        drawAtlasRegion(graphics, x, y, 119, 56, 5, 96, 119, 56);
        PlayerFaceExtractor.extractRenderState(graphics, member.getSkin(), x + 12, y + 12, 28);
        drawPartyPortraitOverlay(graphics, x, y);

        graphics.text(minecraft.font, fitText(minecraft, member.getName().getString(), 62), x + 55, y + 3, TEXT_PRIMARY, false);
        Component classLine = data.hasClass()
                ? Component.translatable(data.rpgClass().translationKey())
                : Component.translatable("class.basicrpgclasses.unassigned");
        graphics.text(minecraft.font, fitText(minecraft, classLine.getString(), 58), x + 59, y + 32, TEXT_MUTED, false);

        float healthRatio = Math.clamp(member.getHealth() / Math.max(1.0F, member.getMaxHealth()), 0.0F, 1.0F);
        drawInsetBar(graphics, x + 59, y + 17, 58, 7, healthRatio,
                HEALTH_FILL, HEALTH_EMPTY, HEALTH_SHINE);
        graphics.centeredText(minecraft.font,
                Math.round(member.getHealth()) + "/" + Math.round(member.getMaxHealth()),
                x + 88, y + 16, TEXT_PRIMARY);
    }

    private static void renderSkillBar(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
        PlayerCombatData combat = minecraft.player.getData(ModAttachments.PLAYER_COMBAT);
        long gameTime = minecraft.player.level().getGameTime();

        int slot = 22;
        int gap = 3;
        int totalWidth = slot * CombatModeController.SKILL_SLOT_COUNT + gap * (CombatModeController.SKILL_SLOT_COUNT - 1);
        int panelWidth = 230;
        int panelHeight = 46;
        int panelX = minecraft.getWindow().getGuiScaledWidth() / 2 - panelWidth / 2;
        int panelY = minecraft.getWindow().getGuiScaledHeight() - 72;
        int startX = panelX + (panelWidth - totalWidth) / 2;
        int y = panelY + 14;

        drawAtlasRegion(graphics, panelX, panelY, panelWidth, panelHeight, 6, 208, 230, 46);

        for (int index = 0; index < CombatModeController.SKILL_SLOT_COUNT; index++) {
            int x = startX + index * (slot + gap);
            boolean selected = index == CombatModeController.selectedSkillSlot();
            SkillId skillId = data.skillAtSlot(index);
            SkillDefinition definition = SkillDefinitions.get(skillId);
            int slotTrim = selected ? 0xFFFFD27B : 0xFF8D898A;

            graphics.fill(x + 1, y + 1, x + slot - 1, y + slot - 1, selected ? 0xF13A2024 : 0xF0101015);
            if (selected) {
                graphics.outline(x, y, slot, slot, slotTrim);
            }
            graphics.text(minecraft.font, Integer.toString(index + 1), x + 2, y + 1, slotTrim, false);

            if (definition == null) {
                graphics.centeredText(minecraft.font, "—", x + slot / 2, y + 7, 0xFF827D81);
                continue;
            }

            int rank = data.skillRank(skillId);
            graphics.fill(x + 2, y + 2, x + slot - 2, y + 3, definition.color());
            if (ClientToggleSkillStates.active(skillId)) {
                drawActiveToggle(graphics, x, y, slot, gameTime);
            }
            graphics.centeredText(
                    minecraft.font,
                    Component.translatable(skillId.shortTranslationKey()),
                    x + slot / 2,
                    y + 7,
                    TEXT_PRIMARY
            );

            int maxCharges = definition.maxCharges(rank);
            boolean creative = minecraft.player.getAbilities().instabuild;
            long remainingTicks = creative ? 0L : combat.remainingRechargeTicks(definition, rank, gameTime);
            int availableCharges = creative ? maxCharges : combat.availableCharges(definition, rank, gameTime);
            if (remainingTicks > 0L) {
                drawCooldownRadar(graphics, minecraft, x, y, slot, remainingTicks,
                        definition.cooldownTicks(rank), availableCharges == 0);
            }

            if (maxCharges > 1) {
                graphics.text(minecraft.font, availableCharges + "/" + maxCharges, x + 2, y + slot - 9,
                        availableCharges > 0 ? TEXT_PRIMARY : 0xFF9D979A, false);
            }

            int displayedCost = switch (skillId) {
                case FROST_ARROWS -> SkillScaling.frostArrowManaCost(rank);
                case MULTISHOT -> SkillScaling.multishotManaCost(rank);
                case DIVINE_SLASH -> SkillScaling.divineSlashManaCost(rank);
                default -> definition.manaCost(rank);
            };
            String resourceCost = Integer.toString(displayedCost);
            int resourceCostColor = combat.canAfford(displayedCost)
                    ? ClassResourceRules.usesFury(data.rpgClass()) ? 0xFFFFBF72 : 0xFF9DDEFF
                    : 0xFFFF6874;
            graphics.text(minecraft.font, resourceCost, x + slot - minecraft.font.width(resourceCost) - 2,
                    y + slot - 9, resourceCostColor, false);

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
        int width = 206;
        int height = 22;
        int x = minecraft.getWindow().getGuiScaledWidth() / 2 - width / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() - 99;
        float progress = Math.clamp(ClientCastState.progress(), 0.0F, 1.0F);
        drawGothicPanel(graphics, x, y, width, height, 0xFFF0C66A);
        drawGothicBar(graphics, x + 9, y + 12, width - 18, 6, progress,
                0xFFE0B849, 0xFF322716, 0xFFFFF0A3, 0L, 0);
        String seconds = String.format(Locale.ROOT, "%.1f", ClientCastState.remainingTicks() / 20.0);
        Component label = Component.translatable("hud.basicrpgclasses.casting",
                Component.translatable(ClientCastState.skill().translationKey()), seconds);
        graphics.centeredText(minecraft.font, label, x + width / 2, y + 3, TEXT_PRIMARY);
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
        graphics.fill(x + 2, y + 5, x + size - 2, y + size - 2, locked ? 0xBA08070A : 0x5A08070A);
        double ratio = Math.clamp(remainingTicks / (double) Math.max(1, totalTicks), 0.0, 1.0);
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int radius = size / 2 - 4;
        int segmentCount = 16;
        int activeSegments = (int) Math.ceil(ratio * segmentCount);

        for (int segment = 0; segment < activeSegments; segment++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * segment / segmentCount;
            int markerX = centerX + (int) Math.round(Math.cos(angle) * radius);
            int markerY = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(markerX - 1, markerY - 1, markerX + 1, markerY + 1, 0xFFACCADD);
        }

        double handAngle = -Math.PI / 2.0 + Math.PI * 2.0 * (1.0 - ratio);
        for (int step = 1; step <= radius; step++) {
            int handX = centerX + (int) Math.round(Math.cos(handAngle) * step);
            int handY = centerY + (int) Math.round(Math.sin(handAngle) * step);
            graphics.fill(handX, handY, handX + 1, handY + 1, 0xFFE9F6FF);
        }

        String seconds = String.format(Locale.ROOT, "%.1f", Math.ceil(remainingTicks / 2.0) / 10.0);
        graphics.centeredText(minecraft.font, seconds, centerX, centerY - 4, TEXT_PRIMARY);
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
        int width = 190;
        int height = 24;
        int x = minecraft.getWindow().getGuiScaledWidth() / 2 - width / 2;
        int y = minecraft.getWindow().getGuiScaledHeight() - height - 1;
        // This layer renders after vanilla's item icons, so keep it glassy rather than covering them.
        graphics.fill(x, y, x + width, y + height, 0x6E0B0B10);
        graphics.outline(x, y, width, height, TRIM_SHADOW);
        graphics.outline(x + 1, y + 1, width - 2, height - 2, TRIM_SILVER);
        drawCornerOrnament(graphics, x + 1, y + 1, 1, 1, 0xFF817D80);
        drawCornerOrnament(graphics, x + width - 2, y + 1, -1, 1, 0xFF817D80);
        drawCornerOrnament(graphics, x + 1, y + height - 2, 1, -1, 0xFF817D80);
        drawCornerOrnament(graphics, x + width - 2, y + height - 2, -1, -1, 0xFF817D80);
    }

    private static void renderTargetFrame(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        int bossOffset = bossGraphics == graphics ? bossBarBottom + 8 : 8;
        if (minecraft.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            int width = 210;
            int height = 54;
            int x = minecraft.getWindow().getGuiScaledWidth() / 2 - width / 2;
            int y = bossOffset;
            drawGothicPanel(graphics, x, y, width, height, 0xFFC5B7A7);
            if (living instanceof AbstractClientPlayer targetPlayer) {
                drawPortraitFrame(graphics, targetPlayer, x + 7, y + 10, 34, 0xFFC5B7A7);
            } else {
                InventoryScreen.renderEntityInInventoryFollowsAngle(
                        graphics, x + 2, y + 3, x + 45, y + 51, 18, 0.0F, 0.15F, 0.0F, living
                );
            }
            graphics.text(minecraft.font, fitText(minecraft, living.getName().getString(), width - 60), x + 50, y + 12, TEXT_PRIMARY, false);
            float ratio = Math.clamp(living.getHealth() / Math.max(1.0F, living.getMaxHealth()), 0.0F, 1.0F);
            drawGothicBar(graphics, x + 50, y + 30, width - 61, 10, ratio,
                    HEALTH_FILL, HEALTH_EMPTY, HEALTH_SHINE, living.level().getGameTime(), 3);
            graphics.centeredText(minecraft.font, Math.round(living.getHealth()) + " / " + Math.round(living.getMaxHealth()),
                    x + 50 + (width - 61) / 2, y + 31, TEXT_PRIMARY);
        } else if (minecraft.hitResult instanceof BlockHitResult blockHit && minecraft.level != null) {
            var state = minecraft.level.getBlockState(blockHit.getBlockPos());
            if (state.isAir()) {
                return;
            }
            ItemStack icon = new ItemStack(state.getBlock().asItem());
            int width = 170;
            int x = minecraft.getWindow().getGuiScaledWidth() / 2 - width / 2;
            int y = bossOffset;
            drawGothicPanel(graphics, x, y, width, 31, 0xFFC5B7A7);
            if (!icon.isEmpty()) {
                graphics.item(icon, x + 8, y + 8);
            }
            graphics.text(minecraft.font, fitText(minecraft, state.getBlock().getName().getString(), 132), x + 31, y + 11, TEXT_PRIMARY, false);
        }
    }

    private static void drawAtlasRegion(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                GOTHIC_HUD_TEXTURE,
                x,
                y,
                sourceX,
                sourceY,
                width,
                height,
                sourceWidth,
                sourceHeight,
                GOTHIC_ATLAS_SIZE,
                GOTHIC_ATLAS_SIZE
        );
    }

    /** Restores the silver border after the live player face covers the atlas portrait. */
    private static void drawPlayerPortraitOverlay(GuiGraphicsExtractor graphics, int x, int y) {
        drawAtlasRegion(graphics, x, y, 72, 17, 1, 4, 72, 17);
        drawAtlasRegion(graphics, x, y, 16, 66, 1, 4, 16, 66);
        drawAtlasRegion(graphics, x + 57, y, 16, 66, 58, 4, 16, 66);
        drawAtlasRegion(graphics, x, y + 58, 73, 26, 1, 62, 73, 26);
    }

    /** Smaller portrait border used by every party member card. */
    private static void drawPartyPortraitOverlay(GuiGraphicsExtractor graphics, int x, int y) {
        drawAtlasRegion(graphics, x, y, 50, 12, 5, 96, 50, 12);
        drawAtlasRegion(graphics, x, y, 14, 51, 5, 96, 14, 51);
        drawAtlasRegion(graphics, x + 37, y, 13, 51, 42, 96, 13, 51);
        drawAtlasRegion(graphics, x, y + 40, 50, 16, 5, 136, 50, 16);
    }

    /** Dynamic fill placed inside an ornate bar channel already present in the atlas. */
    private static void drawInsetBar(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            float ratio,
            int color,
            int background,
            int highlight
    ) {
        graphics.fill(x, y, x + width, y + height, background);
        int fillWidth = Math.clamp(Math.round(width * ratio), 0, width);
        if (fillWidth <= 0) {
            return;
        }
        graphics.fill(x, y, x + fillWidth, y + height, color);
        graphics.fill(x, y, x + fillWidth, y + 1, highlight);
    }

    private static void drawPortraitFrame(
            GuiGraphicsExtractor graphics,
            AbstractClientPlayer player,
            int x,
            int y,
            int size,
            int accent
    ) {
        graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, PANEL_INNER);
        graphics.outline(x - 2, y - 2, size + 4, size + 4, TRIM_DARK);
        graphics.outline(x - 1, y - 1, size + 2, size + 2, accent);
        PlayerFaceExtractor.extractRenderState(graphics, player.getSkin(), x, y, size);
        drawCornerOrnament(graphics, x - 2, y - 2, 1, 1, accent);
        drawCornerOrnament(graphics, x + size + 1, y - 2, -1, 1, accent);
        drawCornerOrnament(graphics, x - 2, y + size + 1, 1, -1, accent);
        drawCornerOrnament(graphics, x + size + 1, y + size + 1, -1, -1, accent);
    }

    private static void drawGothicPanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int accent
    ) {
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        graphics.outline(x, y, width, height, TRIM_SHADOW);
        graphics.outline(x + 1, y + 1, width - 2, height - 2, TRIM_SILVER);
        graphics.outline(x + 3, y + 3, width - 6, height - 6, TRIM_DARK);
        graphics.fill(x + 5, y + 4, x + width - 5, y + 5, 0xAAFFFFFF);
        graphics.fill(x + 5, y + height - 5, x + width - 5, y + height - 4, 0x88000000);
        drawCornerOrnament(graphics, x + 1, y + 1, 1, 1, accent);
        drawCornerOrnament(graphics, x + width - 2, y + 1, -1, 1, accent);
        drawCornerOrnament(graphics, x + 1, y + height - 2, 1, -1, accent);
        drawCornerOrnament(graphics, x + width - 2, y + height - 2, -1, -1, accent);
    }

    private static void drawCornerOrnament(GuiGraphicsExtractor graphics, int x, int y, int directionX, int directionY, int color) {
        fillBetween(graphics, x, y, x + directionX * 4, y + directionY, color);
        fillBetween(graphics, x, y, x + directionX, y + directionY * 4, color);
        fillBetween(graphics, x + directionX * 2, y + directionY * 2,
                x + directionX * 3, y + directionY * 3, TRIM_SILVER);
    }

    private static void fillBetween(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1) + 1, Math.max(y0, y1) + 1, color);
    }

    private static void drawCrest(GuiGraphicsExtractor graphics, int centerX, int y, int color) {
        graphics.fill(centerX - 6, y, centerX + 7, y + 1, TRIM_DARK);
        graphics.fill(centerX - 3, y, centerX + 4, y + 1, color);
        graphics.fill(centerX - 2, y + 1, centerX + 3, y + 2, color);
        graphics.fill(centerX - 1, y + 2, centerX + 2, y + 3, TRIM_SILVER);
    }

    private static void drawGothicBar(
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
        graphics.fill(x, y, x + width, y + height, 0xFF050509);
        graphics.outline(x, y, width, height, TRIM_DARK);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, background);
        int fillWidth = Math.clamp(Math.round((width - 2) * ratio), 0, width - 2);
        if (fillWidth <= 0) {
            return;
        }
        graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, color);
        graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + 2, highlight);
        int phase = Math.floorMod((int) (gameTime / 2L) + phaseOffset, 16);
        for (int stripeX = x + 1 - phase; stripeX < x + 1 + fillWidth; stripeX += 16) {
            int stripeStart = Math.max(x + 1, stripeX);
            int stripeEnd = Math.min(x + 1 + fillWidth, stripeX + 2);
            if (stripeEnd > stripeStart) {
                graphics.fill(stripeStart, y + 2, stripeEnd, y + height - 1, brighten(color, 0.30F));
            }
        }
    }

    private static int bossColor(BossEvent.BossBarColor color) {
        return switch (color) {
            case PINK -> 0xFFD96C9C;
            case BLUE -> 0xFF5785C8;
            case RED -> 0xFFB3313D;
            case GREEN -> 0xFF5D9E6B;
            case YELLOW -> 0xFFD2AA4D;
            case PURPLE -> 0xFF8D66B7;
            case WHITE -> 0xFFC7C1B8;
        };
    }

    private static int darken(int color, float factor) {
        int red = Math.round(((color >>> 16) & 0xFF) * factor);
        int green = Math.round(((color >>> 8) & 0xFF) * factor);
        int blue = Math.round((color & 0xFF) * factor);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int brighten(int color, float amount) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        red += Math.round((255 - red) * amount);
        green += Math.round((255 - green) * amount);
        blue += Math.round((255 - blue) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static String fitText(Minecraft minecraft, String value, int maximumWidth) {
        if (minecraft.font.width(value) <= maximumWidth) {
            return value;
        }
        String suffix = "…";
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            if (minecraft.font.width(builder.toString() + value.charAt(index) + suffix) > maximumWidth) {
                break;
            }
            builder.append(value.charAt(index));
        }
        return builder.length() == 0 ? suffix : builder + suffix;
    }

    private RpgHud() {
    }
}
