package net.cgerwyu.basicrpgclasses.client.gui;

import net.cgerwyu.basicrpgclasses.data.ClassResourceRules;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.PlayerClassData;
import net.cgerwyu.basicrpgclasses.data.SkillPointCosts;
import net.cgerwyu.basicrpgclasses.network.payload.ApplySkillUpgradesPayload;
import net.cgerwyu.basicrpgclasses.network.payload.PurchaseSkillPointPayload;
import net.cgerwyu.basicrpgclasses.network.payload.SetActionBarSlotPayload;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinition;
import net.cgerwyu.basicrpgclasses.skill.SkillDefinitions;
import net.cgerwyu.basicrpgclasses.skill.SkillId;
import net.cgerwyu.basicrpgclasses.skill.SkillScaling;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClassProgressionScreen extends Screen {
    private static final int ROW_HEIGHT = 64;
    private static final int ICON_SIZE = 34;
    private static final int SLOT_GAP = 2;

    private final Map<SkillId, Integer> stagedRanks = new EnumMap<>(SkillId.class);
    private final Map<SkillId, Button> minusButtons = new EnumMap<>(SkillId.class);
    private final Map<SkillId, Button> plusButtons = new EnumMap<>(SkillId.class);
    private List<SkillDefinition> allSkills = List.of();
    private List<SkillDefinition> visibleSkills = List.of();
    private SkillPage page = SkillPage.ACTIVE;
    private Button purchaseButton;
    private Button confirmButton;
    private Button changeClassButton;
    private Button activePageButton;
    private Button passivePageButton;
    private SkillDefinition hoveredSkill;
    private SkillId draggedSkill = SkillId.NONE;
    private List<Integer> submittedRanks;
    private int confirmationWaitTicks;
    private double scrollOffset;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public ClassProgressionScreen() {
        super(Component.translatable("screen.basicrpgclasses.progression.title"));
    }

    @Override
    protected void init() {
        clearWidgets();
        minusButtons.clear();
        plusButtons.clear();
        panelWidth = Math.min(480, width - 28);
        panelHeight = Math.min(330, height - 20);
        panelX = width / 2 - panelWidth / 2;
        panelY = height / 2 - panelHeight / 2;

        if (minecraft.player == null) {
            return;
        }
        PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
        allSkills = SkillDefinitions.forClass(data.rpgClass());
        rebuildVisibleSkills();
        scrollOffset = Math.clamp(scrollOffset, 0.0, maxScroll());

        int gap = 6;
        int buttonWidth = (panelWidth - 20 - gap) / 2;
        int buttonY = panelY + panelHeight - 26;
        purchaseButton = addRenderableWidget(Button.builder(
                        Component.empty(),
                        ignored -> ClientPacketDistributor.sendToServer(PurchaseSkillPointPayload.INSTANCE)
                )
                .bounds(panelX + 10, buttonY, buttonWidth, 20)
                .build());
        confirmButton = addRenderableWidget(Button.builder(
                        Component.empty(),
                        ignored -> confirmStagedUpgrades()
                )
                .bounds(panelX + 10 + buttonWidth + gap, buttonY, buttonWidth, 20)
                .build());
        changeClassButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.basicrpgclasses.progression.change_class"),
                        ignored -> minecraft.gui.setScreen(new ConfirmClassChangeScreen())
                )
                .bounds(panelX + panelWidth - 104, panelY + 5, 96, 20)
                .build());
        activePageButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.basicrpgclasses.progression.active_skills"),
                        ignored -> switchPage(SkillPage.ACTIVE)
                )
                .bounds(panelX + 10, panelY + 40, 108, 20)
                .build());
        passivePageButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.basicrpgclasses.progression.passive_skills"),
                        ignored -> switchPage(SkillPage.PASSIVE)
                )
                .bounds(panelX + 122, panelY + 40, 108, 20)
                .build());
        for (SkillDefinition definition : allSkills) {
            Button minus = addRenderableWidget(Button.builder(
                            Component.literal("−"),
                            ignored -> {
                                if (minecraft.player != null) {
                                    unstageUpgrade(minecraft.player.getData(ModAttachments.PLAYER_CLASS), definition);
                                    refreshButtons(minecraft.player.getData(ModAttachments.PLAYER_CLASS));
                                }
                            }
                    )
                    .bounds(0, 0, 20, 20)
                    .build());
            Button plus = addRenderableWidget(Button.builder(
                            Component.literal("+"),
                            ignored -> {
                                stageUpgrade(definition);
                                if (minecraft.player != null) {
                                    refreshButtons(minecraft.player.getData(ModAttachments.PLAYER_CLASS));
                                }
                            }
                    )
                    .bounds(0, 0, 20, 20)
                    .build());
            minusButtons.put(definition.id(), minus);
            plusButtons.put(definition.id(), plus);
        }
        refreshButtons(data);
    }

    @Override
    public void tick() {
        if (minecraft.player == null) {
            return;
        }
        PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
        if (submittedRanks != null) {
            boolean applied = true;
            for (int index = 0; index < SkillId.storageSize(); index++) {
                if (data.skillRank(SkillId.byId(index)) != submittedRanks.get(index)) {
                    applied = false;
                    break;
                }
            }
            if (applied) {
                stagedRanks.clear();
                submittedRanks = null;
                confirmationWaitTicks = 0;
            } else if (++confirmationWaitTicks > 200) {
                // Keep the planned upgrades if the server refused the request or its
                // synchronized attachment did not arrive. The player can try again.
                submittedRanks = null;
                confirmationWaitTicks = 0;
            }
        }
        stagedRanks.entrySet().removeIf(entry -> data.skillRank(entry.getKey()) + entry.getValue() > SkillDefinitions.get(entry.getKey()).maxRank());
        refreshButtons(data);
    }

    private void refreshButtons(PlayerClassData data) {
        int cost = SkillPointCosts.nextPointCost(data);
        purchaseButton.active = data.hasClass()
                && minecraft.player.experienceLevel >= cost;
        purchaseButton.setMessage(Component.translatable(
                "screen.basicrpgclasses.progression.buy_point_cost",
                cost
        ));
        int pending = pendingPoints();
        confirmButton.active = submittedRanks == null && pending > 0 && pending <= data.unspentSkillPoints();
        confirmButton.setMessage(Component.translatable(
                "screen.basicrpgclasses.progression.confirm_upgrades",
                pending
        ));
        changeClassButton.active = data.hasClass();
        activePageButton.active = page != SkillPage.ACTIVE;
        passivePageButton.active = page != SkillPage.PASSIVE;
        updateSkillButtons(data);
    }

    private void updateSkillButtons(PlayerClassData data) {
        for (Button button : minusButtons.values()) {
            button.visible = false;
        }
        for (Button button : plusButtons.values()) {
            button.visible = false;
        }
        for (int index = 0; index < visibleSkills.size(); index++) {
            SkillDefinition definition = visibleSkills.get(index);
            int y = rowY(index) + (ROW_HEIGHT - 20) / 2;
            boolean visible = rowY(index) >= listTop()
                    && rowY(index) + ROW_HEIGHT <= listTop() + listHeight();
            Button minus = minusButtons.get(definition.id());
            Button plus = plusButtons.get(definition.id());
            int rowRight = listLeft() + listWidth() - 10;
            boolean maximumRank = data.skillRank(definition.id()) >= definition.maxRank();
            minus.setX(rowRight - 44);
            minus.setY(y);
            minus.visible = visible && !maximumRank;
            minus.active = stagedRanks.getOrDefault(definition.id(), 0) > 0;
            plus.setX(rowRight - 20);
            plus.setY(y);
            plus.visible = visible && !maximumRank;
            plus.active = canStage(data, definition);
        }
    }

    private void confirmStagedUpgrades() {
        if (minecraft.player == null || pendingPoints() <= 0) {
            return;
        }
        PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
        List<Integer> desiredRanks = new ArrayList<>(SkillId.storageSize());
        for (int index = 0; index < SkillId.storageSize(); index++) {
            SkillId skillId = SkillId.byId(index);
            desiredRanks.add(data.skillRank(skillId) + stagedRanks.getOrDefault(skillId, 0));
        }
        ClientPacketDistributor.sendToServer(new ApplySkillUpgradesPayload(List.copyOf(desiredRanks)));
        submittedRanks = List.copyOf(desiredRanks);
        confirmationWaitTicks = 0;
    }

    public void handleUpgradeResult(boolean applied) {
        if (!applied) {
            // The server rejected the request: preserve the plan so the player can
            // adjust or retry it instead of silently losing all staged ranks.
            submittedRanks = null;
            confirmationWaitTicks = 0;
        }
        // On success the synchronized PlayerClassData is authoritative. tick()
        // clears the plan only after those exact ranks arrive on the client.
    }

    private void stageUpgrade(SkillDefinition definition) {
        if (minecraft.player == null) {
            return;
        }
        PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
        int effectiveRank = effectiveRank(data, definition.id());
        if (effectiveRank >= definition.maxRank() || pendingPoints() >= data.unspentSkillPoints()) {
            return;
        }
        if (data.classLevel() < definition.requiredClassLevelForRank(effectiveRank + 1)) {
            return;
        }
        if (definition.hasPrerequisite()
                && effectiveRank(data, definition.prerequisite()) < definition.prerequisiteRank()) {
            return;
        }
        stagedRanks.merge(definition.id(), 1, Integer::sum);
    }

    private void unstageUpgrade(PlayerClassData data, SkillDefinition definition) {
        int pending = stagedRanks.getOrDefault(definition.id(), 0);
        if (pending <= 1) {
            stagedRanks.remove(definition.id());
        } else {
            stagedRanks.put(definition.id(), pending - 1);
        }
        for (SkillDefinition dependent : allSkills) {
            if (dependent.hasPrerequisite()
                    && stagedRanks.getOrDefault(dependent.id(), 0) > 0
                    && effectiveRank(data, dependent.prerequisite()) < dependent.prerequisiteRank()) {
                stagedRanks.remove(dependent.id());
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (minecraft.player == null || event.button() != 0) {
            return false;
        }

        PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
        for (int index = 0; index < visibleSkills.size(); index++) {
            SkillDefinition definition = visibleSkills.get(index);
            int rowY = rowY(index);
            if (!rowVisible(rowY)) {
                continue;
            }
            if (inside(event.x(), event.y(), panelX + 15, rowY + 12, ICON_SIZE, ICON_SIZE)
                    && !definition.id().isPassive()
                    && data.skillRank(definition.id()) > 0) {
                draggedSkill = definition.id();
                setDragging(true);
                return true;
            }
        }

        int sourceSlot = slotAt(event.x(), event.y());
        if (sourceSlot >= 0) {
            SkillId assigned = data.skillAtSlot(sourceSlot);
            if (assigned != SkillId.NONE) {
                draggedSkill = assigned;
                setDragging(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return draggedSkill != SkillId.NONE || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggedSkill != SkillId.NONE) {
            int targetSlot = slotAt(event.x(), event.y());
            if (targetSlot >= 0) {
                ClientPacketDistributor.sendToServer(new SetActionBarSlotPayload(
                        targetSlot,
                        draggedSkill.numericId()
                ));
            }
            draggedSkill = SkillId.NONE;
            setDragging(false);
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (inside(mouseX, mouseY, listLeft(), listTop(), listWidth(), listHeight())) {
            scrollOffset = Math.clamp(scrollOffset - deltaY * 28.0, 0.0, maxScroll());
            if (minecraft.player != null) {
                updateSkillButtons(minecraft.player.getData(ModAttachments.PLAYER_CLASS));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hoveredSkill = null;
        extractTransparentBackground(graphics);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF0C6C6C6);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, 0xFF373737);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + 2, 0xFFFFFFFF);
        graphics.fill(panelX + 1, panelY + 1, panelX + 2, panelY + panelHeight - 1, 0xFFFFFFFF);
        graphics.fill(panelX + 1, panelY + panelHeight - 2, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF555555);
        graphics.fill(panelX + panelWidth - 2, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xFF555555);
        graphics.text(font, title, panelX + 10, panelY + 11, 0xFF303030, false);

        if (minecraft.player != null) {
            PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
            graphics.centeredText(font, Component.translatable(
                    "screen.basicrpgclasses.progression.summary_compact",
                    Component.translatable(data.rpgClass().translationKey()),
                    data.classLevel(),
                    minecraft.player.experienceLevel,
                    data.unspentSkillPoints()
            ), width / 2, panelY + 28, 0xFF404040);

            renderSkillList(graphics, data, mouseX, mouseY);
            renderActionBar(graphics, data);

            graphics.text(font, Component.translatable(
                    "screen.basicrpgclasses.progression.drag_instruction"
            ), panelX + 12, actionBarY() - 12, 0xFF444444, false);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (hoveredSkill != null && draggedSkill == SkillId.NONE) {
            renderSkillCard(
                    graphics,
                    minecraft.player.getData(ModAttachments.PLAYER_CLASS),
                    hoveredSkill,
                    mouseX,
                    mouseY
            );
        }
        if (draggedSkill != SkillId.NONE) {
            SkillDefinition definition = SkillDefinitions.get(draggedSkill);
            renderSkillIcon(graphics, definition, mouseX - ICON_SIZE / 2, mouseY - ICON_SIZE / 2, ICON_SIZE, true);
        }
    }

    private void renderSkillList(GuiGraphicsExtractor graphics, PlayerClassData data, int mouseX, int mouseY) {
        graphics.fill(listLeft(), listTop(), listLeft() + listWidth(), listTop() + listHeight(), 0xFF8B8B8B);
        graphics.outline(listLeft(), listTop(), listWidth(), listHeight(), 0xFF373737);
        graphics.enableScissor(listLeft() + 1, listTop() + 1, listLeft() + listWidth() - 1, listTop() + listHeight() - 1);

        for (int index = 0; index < visibleSkills.size(); index++) {
            SkillDefinition definition = visibleSkills.get(index);
            int rowY = rowY(index);
            if (!rowVisible(rowY)) {
                continue;
            }
            int rowX = listLeft() + 5;
            int rowWidth = listWidth() - 10;
            int currentRank = data.skillRank(definition.id());
            int pending = stagedRanks.getOrDefault(definition.id(), 0);
            int shownRank = currentRank + pending;
            boolean maximumRank = shownRank >= definition.maxRank();
            boolean confirmedMaximumRank = currentRank >= definition.maxRank();
            int nextRank = Math.min(definition.maxRank(), shownRank + 1);
            int requiredForNextRank = definition.requiredClassLevelForRank(nextRank);
            boolean levelLocked = !maximumRank && data.classLevel() < requiredForNextRank;
            boolean isHovered = inside(mouseX, mouseY, rowX, rowY + 3, rowWidth, ROW_HEIGHT - 6);
            graphics.fill(rowX, rowY + 3, rowX + rowWidth, rowY + ROW_HEIGHT - 3,
                    levelLocked ? 0xFFA0A0A0 : isHovered ? 0xFFD4D4D4 : 0xFFB8B8B8);
            graphics.outline(rowX, rowY + 3, rowWidth, ROW_HEIGHT - 6, isHovered ? 0xFFFFFFFF : 0xFF555555);
            renderSkillIcon(graphics, definition, panelX + 15, rowY + 12, ICON_SIZE, false);

            renderLargeName(graphics, Component.translatable(definition.id().translationKey()), panelX + 55, rowY + 8,
                    levelLocked ? 0xFF5B5B5B : definition.color());
            List<FormattedCharSequence> description = font.split(
                    Component.translatable(definition.id().descriptionKey()),
                    Math.max(80, panelWidth - 175)
            );
            for (int line = 0; line < Math.min(2, description.size()); line++) {
                graphics.text(font, description.get(line), panelX + 55, rowY + 25 + line * 10, 0xFF3F3F3F, false);
            }
            graphics.text(font, Component.translatable(
                    "screen.basicrpgclasses.progression.rank_pending",
                    shownRank,
                    definition.maxRank(),
                    pending
            ), panelX + 55, rowY + 49, pending > 0 ? 0xFF9A6500 : 0xFF245C80, false);

            Component requirement = null;
            if (levelLocked) {
                requirement = Component.translatable(
                        "screen.basicrpgclasses.progression.requires_level_for_rank_short",
                        requiredForNextRank,
                        nextRank
                );
            } else if (definition.hasPrerequisite()) {
                requirement = Component.translatable(
                        "screen.basicrpgclasses.progression.skill_requires_short",
                        Component.translatable(definition.prerequisite().translationKey()),
                        definition.prerequisiteRank()
                );
            }
            if (requirement != null) {
                graphics.text(font, requirement, panelX + panelWidth - 168, rowY + 49, levelLocked ? 0xFF9A2F2F : 0xFF5A5A5A, false);
            }
            boolean fullyVisible = rowY >= listTop() && rowY + ROW_HEIGHT <= listTop() + listHeight();
            if (confirmedMaximumRank && fullyVisible) {
                Component maximum = Component.translatable("screen.basicrpgclasses.progression.max_level");
                graphics.text(
                        font,
                        maximum,
                        rowX + rowWidth - font.width(maximum) - 8,
                        rowY + (ROW_HEIGHT - font.lineHeight) / 2,
                        0xFFE6B84F,
                        false
                );
            }
            if (isHovered) {
                hoveredSkill = definition;
            }
        }
        graphics.disableScissor();
    }

    private void renderActionBar(GuiGraphicsExtractor graphics, PlayerClassData data) {
        int slotWidth = actionSlotWidth();
        int startX = actionBarStartX();
        int y = actionBarY();
        for (int slot = 0; slot < PlayerClassData.ACTION_BAR_SLOT_COUNT; slot++) {
            int x = startX + slot * (slotWidth + SLOT_GAP);
            SkillId skillId = data.skillAtSlot(slot);
            SkillDefinition definition = SkillDefinitions.get(skillId);
            graphics.fill(x, y, x + slotWidth, y + 28, 0xFF8B8B8B);
            graphics.outline(x, y, slotWidth, 28, 0xFF373737);
            graphics.text(font, Integer.toString(slot + 1), x + 2, y + 2, 0xFFDDDDDD, false);
            if (definition != null) {
                graphics.fill(x + 2, y + 12, x + slotWidth - 2, y + 15, definition.color());
                graphics.centeredText(font, Component.translatable(skillId.shortTranslationKey()), x + slotWidth / 2, y + 16, 0xFFFFFFFF);
            } else {
                graphics.centeredText(font, "—", x + slotWidth / 2, y + 15, 0xFF666666);
            }
        }
    }

    private void renderSkillIcon(GuiGraphicsExtractor graphics, SkillDefinition definition, int x, int y, int size, boolean floating) {
        int background = floating ? 0xF05B5B5B : 0xFF777777;
        graphics.fill(x, y, x + size, y + size, background);
        graphics.outline(x, y, size, size, definition.color());
        graphics.fill(x + 3, y + 3, x + size - 3, y + 7, definition.color());
        graphics.centeredText(font, Component.translatable(definition.id().shortTranslationKey()), x + size / 2, y + size / 2 - 4, 0xFFFFFFFF);
    }

    private void renderLargeName(GuiGraphicsExtractor graphics, Component name, int x, int y, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(1.18F, 1.18F);
        graphics.text(font, name, 0, 0, color, false);
        graphics.pose().popMatrix();
    }

    private void renderSkillCard(
            GuiGraphicsExtractor graphics,
            PlayerClassData data,
            SkillDefinition definition,
            int mouseX,
            int mouseY
    ) {
        int cardWidth = Math.min(274, width - 16);
        int cardHeight = definition.id().isPassive() ? 174 : 194;
        int cardX = mouseX + 12;
        if (cardX + cardWidth > width - 8) {
            cardX = mouseX - cardWidth - 12;
        }
        cardX = Math.clamp(cardX, 8, Math.max(8, width - cardWidth - 8));
        int cardY = Math.clamp(mouseY - 8, 8, Math.max(8, height - cardHeight - 8));
        int effective = effectiveRank(data, definition.id());
        int rank = Math.max(1, effective);

        graphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0xF5101116);
        graphics.outline(cardX, cardY, cardWidth, cardHeight, definition.color());
        graphics.fill(cardX + 1, cardY + 1, cardX + cardWidth - 1, cardY + 4, definition.color());
        renderSkillIcon(graphics, definition, cardX + 9, cardY + 11, 38, false);
        renderLargeName(graphics, Component.translatable(definition.id().translationKey()), cardX + 56, cardY + 12, definition.color());
        graphics.text(font,
                Component.translatable(definition.id().isPassive()
                        ? "screen.basicrpgclasses.progression.type_passive"
                        : "screen.basicrpgclasses.progression.type_active"),
                cardX + 56, cardY + 29,
                definition.id().isPassive() ? 0xFF84D89B : 0xFF7CCBFF, false);
        graphics.text(font,
                Component.translatable("screen.basicrpgclasses.progression.tooltip_rank", effective, definition.maxRank()),
                cardX + 56, cardY + 40, 0xFFD5D5D5, false);
        int nextRank = Math.min(definition.maxRank(), effective + 1);
        int requiredLevel = definition.requiredClassLevelForRank(nextRank);
        if (requiredLevel > 0 && effective < definition.maxRank()) {
            boolean locked = data.classLevel() < requiredLevel;
            Component required = Component.translatable(
                    "screen.basicrpgclasses.progression.tooltip_required_level_for_rank",
                    requiredLevel,
                    nextRank
            );
            graphics.text(font, required, cardX + cardWidth - font.width(required) - 9, cardY + 40,
                    locked ? 0xFFFF5555 : 0xFF77DD88, false);
        }

        int textY = cardY + 57;
        List<FormattedCharSequence> description = font.split(
                Component.translatable(definition.id().descriptionKey()), cardWidth - 18);
        for (int line = 0; line < Math.min(3, description.size()); line++) {
            graphics.text(font, description.get(line), cardX + 9, textY + line * 10, 0xFFE8E8E8, false);
        }
        textY += Math.min(3, description.size()) * 10 + 7;
        graphics.fill(cardX + 8, textY, cardX + cardWidth - 8, textY + 1, 0xFF3B424A);
        textY += 7;
        graphics.text(font, Component.translatable("screen.basicrpgclasses.progression.current_effect"),
                cardX + 9, textY, 0xFF8A8F98, false);
        textY += 11;
        List<FormattedCharSequence> stats = font.split(effectStats(definition.id(), rank), cardWidth - 18);
        for (int line = 0; line < Math.min(2, stats.size()); line++) {
            graphics.text(font, stats.get(line), cardX + 9, textY + line * 10, 0xFF78D9FF, false);
        }
        textY += Math.min(2, stats.size()) * 10;
        if (definition.hasPrerequisite()) {
            graphics.text(font,
                    Component.translatable(
                            "screen.basicrpgclasses.progression.skill_requires_short",
                            Component.translatable(definition.prerequisite().translationKey()),
                            definition.prerequisiteRank()
                    ), cardX + 9, Math.min(textY + 3, cardY + cardHeight - 32), 0xFFFFC56E, false);
        } else if (effective > 0 && effective < definition.maxRank()) {
            graphics.text(font,
                    Component.translatable("screen.basicrpgclasses.progression.next_rank_short", effective + 1),
                    cardX + 9, Math.min(textY + 3, cardY + cardHeight - 32), 0xFF75DC8A, false);
        }

        int footerY = cardY + cardHeight - 20;
        graphics.fill(cardX + 1, footerY - 5, cardX + cardWidth - 1, footerY - 4, 0xFF30343A);
        if (definition.id().isPassive()) {
            graphics.text(font, Component.translatable("screen.basicrpgclasses.progression.passive_always_on"),
                    cardX + 9, footerY, 0xFF84D89B, false);
        } else {
            graphics.fill(cardX + 9, footerY - 1, cardX + 17, footerY + 7, 0xFFB8BDC3);
            graphics.fill(cardX + 11, footerY + 1, cardX + 15, footerY + 5, 0xFF616871);
            graphics.text(font, decimal(definition.cooldownTicks(rank) / 20.0) + " s",
                    cardX + 22, footerY, 0xFFD6D6D6, false);
            int resourceCost = switch (definition.id()) {
                case FROST_ARROWS -> SkillScaling.frostArrowManaCost(rank);
                case MULTISHOT -> SkillScaling.multishotManaCost(rank);
                case DIVINE_SLASH -> SkillScaling.divineSlashManaCost(rank);
                default -> definition.manaCost(rank);
            };
            Component resource = Component.translatable(
                    "screen.basicrpgclasses.progression.resource_cost_short",
                    resourceCost,
                    Component.translatable(ClassResourceRules.nameTranslationKey(data.rpgClass()))
            );
            int resourceX = cardX + cardWidth - font.width(resource) - 10;
            graphics.fill(resourceX - 13, footerY - 1, resourceX - 5, footerY + 7, 0xFF189FD8);
            graphics.fill(resourceX - 11, footerY + 1, resourceX - 7, footerY + 5, 0xFF57D3FF);
            graphics.text(font, resource, resourceX, footerY, 0xFF58B8FF, false);
        }
    }

    private void switchPage(SkillPage nextPage) {
        if (page == nextPage || minecraft == null || minecraft.player == null) {
            return;
        }
        page = nextPage;
        scrollOffset = 0.0;
        rebuildVisibleSkills();
        refreshButtons(minecraft.player.getData(ModAttachments.PLAYER_CLASS));
    }

    private void rebuildVisibleSkills() {
        visibleSkills = allSkills.stream()
                .filter(definition -> definition.id().isPassive() == (page == SkillPage.PASSIVE))
                .toList();
    }

    private List<Component> skillTooltip(PlayerClassData data, SkillDefinition definition) {
        int effective = effectiveRank(data, definition.id());
        int rank = Math.max(1, effective);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(definition.id().translationKey()));
        lines.add(Component.translatable(definition.id().descriptionKey()));
        lines.add(Component.translatable("screen.basicrpgclasses.progression.tooltip_rank", effective, definition.maxRank()));
        if (definition.requiredClassLevel() > 0) {
            lines.add(Component.translatable(
                    "screen.basicrpgclasses.progression.tooltip_required_level",
                    definition.requiredClassLevel()
            ));
        }
        if (!definition.id().isPassive()) {
            lines.add(Component.translatable(
                    "screen.basicrpgclasses.progression.tooltip_resources",
                    Component.translatable(ClassResourceRules.nameTranslationKey(data.rpgClass())),
                    definition.manaCost(rank),
                    decimal(definition.cooldownTicks(rank) / 20.0),
                    definition.maxCharges(rank)
            ));
        }
        lines.add(effectStats(definition.id(), rank));
        if (rank < definition.maxRank()) {
            int nextRank = rank + 1;
            lines.add(Component.translatable("screen.basicrpgclasses.progression.tooltip_next"));
            if (!definition.id().isPassive()) {
                lines.add(Component.translatable(
                        "screen.basicrpgclasses.progression.tooltip_resources",
                        Component.translatable(ClassResourceRules.nameTranslationKey(data.rpgClass())),
                        definition.manaCost(nextRank),
                        decimal(definition.cooldownTicks(nextRank) / 20.0),
                        definition.maxCharges(nextRank)
                ));
            }
            lines.add(effectStats(definition.id(), nextRank));
        }
        return lines;
    }

    private Component effectStats(SkillId skillId, int rank) {
        return switch (skillId) {
            case WHIRLWIND -> Component.translatable("skill.basicrpgclasses.whirlwind.stats", decimal(SkillScaling.whirlwindDamage(rank)), decimal(SkillScaling.whirlwindRadius(rank)));
            case FORTIFY -> Component.translatable("skill.basicrpgclasses.fortify.stats", decimal(SkillScaling.fortifyDurationTicks(rank) / 20.0), SkillScaling.fortifyResistanceAmplifier(rank) + 1, (SkillScaling.fortifyAbsorptionAmplifier(rank) + 1) * 2, decimal(SkillScaling.fortifyRadius(rank)));
            case PROVOKE -> Component.translatable("skill.basicrpgclasses.provoke.stats", decimal(SkillScaling.provokeRadius(rank)), decimal(SkillScaling.provokeDurationTicks(rank) / 20.0), rank >= 8 ? Component.translatable("screen.basicrpgclasses.yes") : Component.translatable("screen.basicrpgclasses.no"));
            case FIREBALL -> Component.translatable("skill.basicrpgclasses.fireball.stats", decimal(SkillScaling.fireballDamage(rank)), decimal(SkillScaling.fireballBurnSeconds(rank)), SkillScaling.fireballAreaRadius(rank), SkillScaling.fireballVolleyCount(rank), decimal(SkillScaling.fireballVisualScale(rank)));
            case HEAL -> Component.translatable("skill.basicrpgclasses.heal.stats", decimal(SkillScaling.healAmount(rank)), 20);
            case HOLY_BOLT -> Component.translatable("skill.basicrpgclasses.holy_bolt.stats", decimal(SkillScaling.holyBoltDamage(rank)), 28);
            case SOLAR_BEAM -> Component.translatable("skill.basicrpgclasses.solar_beam.stats", decimal(SkillScaling.solarBeamDamage(rank)), decimal(SkillScaling.solarBeamHeal(rank)), 3);
            case BLINK -> Component.translatable("skill.basicrpgclasses.blink.stats", decimal(SkillScaling.blinkDistance(rank)), decimal(SkillScaling.blinkProtectionTicks(rank) / 20.0));
            case MAGIC_SHIELD -> Component.translatable("skill.basicrpgclasses.magic_shield.stats", decimal(SkillScaling.magicShieldDurationTicks() / 20.0), decimal(SkillScaling.magicShieldCleanseRadius(rank)));
            case MAGE_GLIDE -> Component.translatable("skill.basicrpgclasses.mage_glide.stats", decimal(-SkillScaling.mageGlideDescentSpeed(rank)), decimal(SkillScaling.mageGlideAirControlSpeed(rank)), SkillScaling.mageGlideManaPerSecond(rank));
            case MAGE_MANA_REGEN -> Component.translatable("skill.basicrpgclasses.mage_mana_regen.stats", decimal(SkillScaling.mageManaRegenerationBonusPerSecond(rank)));
            case FROST_NOVA -> Component.translatable("skill.basicrpgclasses.frost_nova.stats", decimal(SkillScaling.frostNovaDamage(rank)), decimal(SkillScaling.frostNovaRadius(rank)), decimal(SkillScaling.frostNovaSlowTicks(rank) / 20.0), SkillScaling.frostNovaSlowAmplifier(rank) + 1);
            case METEOR -> Component.translatable("skill.basicrpgclasses.meteor.stats", decimal(SkillScaling.meteorDamage(rank)), decimal(SkillScaling.meteorRadius(rank)), decimal(SkillScaling.meteorCastRange(rank)));
            case SKY_RAYS -> Component.translatable("skill.basicrpgclasses.sky_rays.stats", decimal(SkillScaling.skyRaysDamage(rank)), SkillScaling.skyRaysTargets(rank), decimal(SkillScaling.skyRaysRadius(rank)));
            case CHAIN_LIGHTNING -> Component.translatable("skill.basicrpgclasses.chain_lightning.stats", decimal(SkillScaling.chainLightningDamage(rank)), SkillScaling.chainLightningTargets(rank), decimal(SkillScaling.chainLightningJumpRange(rank)), (int) Math.round(SkillScaling.chainLightningFalloff(rank) * 100.0));
            case DASH -> Component.translatable("skill.basicrpgclasses.dash.stats", decimal(SkillScaling.dashSpeed(rank)));
            case WINDRUN -> Component.translatable("skill.basicrpgclasses.windrun.stats", decimal(SkillScaling.windrunDurationTicks(rank) / 20.0), SkillScaling.windrunSpeedAmplifier(rank) + 1);
            case CAMOUFLAGE -> Component.translatable("skill.basicrpgclasses.camouflage.stats", decimal(SkillScaling.camouflageDurationTicks(rank) / 20.0));
            case MULTISHOT -> Component.translatable("skill.basicrpgclasses.multishot.stats", SkillScaling.multishotArrowCount(rank), decimal(SkillScaling.multishotArrowDamage(rank)));
            case ARROW_RAIN -> Component.translatable("skill.basicrpgclasses.arrow_rain.stats", SkillScaling.arrowRainArrowCount(rank), decimal(SkillScaling.arrowRainRadius(rank)), decimal(SkillScaling.arrowRainArrowDamage(rank)));
            case POWER_SHOT -> Component.translatable("skill.basicrpgclasses.power_shot.stats", decimal(SkillScaling.powerShotDamage(rank)), decimal(SkillScaling.powerShotRange(rank)), decimal(SkillScaling.powerShotWidth(rank)));
            case FROST_ARROWS -> Component.translatable("skill.basicrpgclasses.frost_arrows.stats", SkillScaling.frostArrowManaCost(rank), decimal(SkillScaling.frostArrowSlowTicks(rank) / 20.0), SkillScaling.frostArrowSlowAmplifier(rank) + 1);
            case GROUND_STUN -> Component.translatable("skill.basicrpgclasses.ground_stun.stats", decimal(SkillScaling.groundStunDamage(rank)), decimal(SkillScaling.groundStunRadius(rank)), decimal(SkillScaling.groundStunRange(rank)), decimal(SkillScaling.groundStunDurationTicks(rank) / 20.0));
            case SHIELD_BASH -> Component.translatable("skill.basicrpgclasses.shield_bash.stats", decimal(SkillScaling.shieldBashDamage(rank)), decimal(SkillScaling.shieldBashRange(rank)), decimal(SkillScaling.shieldBashStunTicks(rank) / 20.0));
            case BATTLE_CRY -> Component.translatable("skill.basicrpgclasses.battle_cry.stats", decimal(SkillScaling.battleCryDurationTicks(rank) / 20.0), SkillScaling.battleCryStrengthAmplifier(rank) + 1, SkillScaling.battleCryHasteAmplifier(rank) + 1);
            case HUNTER_FALL_TRAINING -> Component.translatable("skill.basicrpgclasses.hunter_fall_training.stats", (int) Math.round(SkillScaling.hunterFallDamageReduction(rank) * 100.0));
            case HUNTER_CLIMBING -> Component.translatable("skill.basicrpgclasses.hunter_climbing.stats", decimal(SkillScaling.hunterClimbSpeed(rank)));
            case HUNTER_MANA_REGEN -> Component.translatable("skill.basicrpgclasses.hunter_mana_regen.stats", decimal(SkillScaling.hunterManaRegenerationBonusPerSecond(rank)));
            case HUNTER_DRAW_SPEED -> Component.translatable("skill.basicrpgclasses.hunter_draw_speed.stats", (int) Math.round((SkillScaling.hunterDrawSpeedMultiplier(rank) - 1.0) * 100.0));
            case HUNTER_SHOT_POWER -> Component.translatable("skill.basicrpgclasses.hunter_shot_power.stats", (int) Math.round((SkillScaling.hunterShotDamageMultiplier(rank) - 1.0) * 100.0), (int) Math.round((SkillScaling.hunterShotVelocityMultiplier(rank) - 1.0) * 100.0));
            case WARRIOR_VAMPIRISM -> Component.translatable("skill.basicrpgclasses.warrior_vampirism.stats", (int) Math.round(SkillScaling.warriorVampirismFraction(rank) * 100.0));
            case WARRIOR_VITALITY -> vitalityStats(net.cgerwyu.basicrpgclasses.data.RpgClass.WARRIOR, rank);
            case MAGE_VITALITY -> vitalityStats(net.cgerwyu.basicrpgclasses.data.RpgClass.MAGE, rank);
            case HUNTER_VITALITY -> vitalityStats(net.cgerwyu.basicrpgclasses.data.RpgClass.HUNTER, rank);
            case RESTORATION -> Component.translatable("skill.basicrpgclasses.restoration.stats", decimal(SkillScaling.restorationHealPerPulse(rank)), decimal(SkillScaling.restorationCastTicks(rank) / 20.0));
            case HEALING_HALO -> Component.translatable("skill.basicrpgclasses.healing_halo.stats", decimal(SkillScaling.healingHaloAmount(rank)), decimal(SkillScaling.holyRadius(rank)));
            case RESURRECTION -> Component.translatable("skill.basicrpgclasses.resurrection.stats", 25 + rank);
            case BLESSING -> Component.translatable("skill.basicrpgclasses.blessing.stats", decimal(SkillScaling.priestBuffTicks(rank) / 20.0));
            case HOLY_SHIELD -> Component.translatable("skill.basicrpgclasses.holy_shield.stats", (SkillScaling.holyShieldAbsorptionAmplifier(rank) + 1) * 4, decimal(SkillScaling.paladinBuffTicks(rank) / 20.0));
            case CLEANSE -> Component.translatable("skill.basicrpgclasses.cleanse.stats");
            case HOLY_STORM -> Component.translatable("skill.basicrpgclasses.holy_storm.stats", decimal(SkillScaling.holyStormDamage(rank)), decimal(SkillScaling.holyRadius(rank)), decimal(SkillScaling.holyStormCastTicks(rank) / 20.0));
            case PRIEST_VITALITY -> vitalityStats(net.cgerwyu.basicrpgclasses.data.RpgClass.PRIEST, rank);
            case PRIEST_MANA_REGEN -> Component.translatable("skill.basicrpgclasses.priest_mana_regen.stats", decimal(rank * 0.15));
            case PALADIN_HEAL -> Component.translatable("skill.basicrpgclasses.paladin_heal.stats", decimal(SkillScaling.paladinHealAmount(rank)));
            case PALADIN_BLESSING -> Component.translatable("skill.basicrpgclasses.paladin_blessing.stats", decimal(SkillScaling.paladinBuffTicks(rank) / 20.0));
            case DIVINE_BULWARK -> Component.translatable("skill.basicrpgclasses.divine_bulwark.stats", decimal(SkillScaling.paladinBuffTicks(rank) / 20.0));
            case PALADIN_VITALITY -> vitalityStats(net.cgerwyu.basicrpgclasses.data.RpgClass.PALADIN, rank);
            case BERSERK -> Component.translatable("skill.basicrpgclasses.berserk.stats", decimal(SkillScaling.berserkDurationTicks(rank) / 20.0));
            case EXECUTION -> Component.translatable("skill.basicrpgclasses.execution.stats", decimal(SkillScaling.executionDamage(rank)));
            case ULTRA_THRUST -> Component.translatable("skill.basicrpgclasses.ultra_thrust.stats", decimal(SkillScaling.ultraThrustDamage(rank)), 60);
            case WARRIOR_LEAP -> Component.translatable("skill.basicrpgclasses.warrior_leap.stats", decimal(SkillScaling.warriorLeapDamage(rank)), decimal(SkillScaling.warriorLeapRange(rank)));
            case WARRIOR_WHIRLWIND -> Component.translatable("skill.basicrpgclasses.warrior_whirlwind.stats", decimal(SkillScaling.whirlwindDamage(rank)), decimal(SkillScaling.whirlwindRadius(rank)));
            case DIVINE_SLASH -> Component.translatable("skill.basicrpgclasses.divine_slash.stats", decimal(SkillScaling.divineSlashDamage(rank)), SkillScaling.divineSlashManaCost(rank));
            case PALADIN_ARMOR_TRAINING -> Component.translatable("skill.basicrpgclasses.paladin_armor_training.stats", 20 + rank * 3);
            case PALADIN_MANA_STRIKE -> Component.translatable("skill.basicrpgclasses.paladin_mana_strike.stats", Math.max(1, rank / 5));
            case NONE -> Component.empty();
        };
    }

    private Component vitalityStats(net.cgerwyu.basicrpgclasses.data.RpgClass rpgClass, int rank) {
        return Component.translatable(
                "skill.basicrpgclasses.vitality.stats",
                decimal(SkillScaling.vitalityHealthBonus(rpgClass, rank)),
                SkillScaling.vitalityResourceBonus(rpgClass, rank)
        );
    }

    private boolean canStage(PlayerClassData data, SkillDefinition definition) {
        int effective = effectiveRank(data, definition.id());
        if (effective >= definition.maxRank()
                || pendingPoints() >= data.unspentSkillPoints()
                || data.classLevel() < definition.requiredClassLevelForRank(effective + 1)) {
            return false;
        }
        return !definition.hasPrerequisite()
                || effectiveRank(data, definition.prerequisite()) >= definition.prerequisiteRank();
    }

    private int effectiveRank(PlayerClassData data, SkillId skillId) {
        return data.skillRank(skillId) + stagedRanks.getOrDefault(skillId, 0);
    }

    private int pendingPoints() {
        return stagedRanks.values().stream().mapToInt(Integer::intValue).sum();
    }

    private int listLeft() {
        return panelX + 9;
    }

    private int listTop() {
        return panelY + 64;
    }

    private int listWidth() {
        return panelWidth - 18;
    }

    private int listHeight() {
        return Math.max(40, actionBarY() - listTop() - 12);
    }

    private int actionBarY() {
        return panelY + panelHeight - 65;
    }

    private int actionBarStartX() {
        return panelX + 10;
    }

    private int actionSlotWidth() {
        return Math.max(20, (panelWidth - 20 - SLOT_GAP * 8) / PlayerClassData.ACTION_BAR_SLOT_COUNT);
    }

    private int rowY(int index) {
        return listTop() + (int) Math.round(index * ROW_HEIGHT - scrollOffset);
    }

    private boolean rowVisible(int rowY) {
        return rowY + ROW_HEIGHT > listTop() && rowY < listTop() + listHeight();
    }

    private double maxScroll() {
        return Math.max(0.0, visibleSkills.size() * ROW_HEIGHT - listHeight());
    }

    private int slotAt(double mouseX, double mouseY) {
        int slotWidth = actionSlotWidth();
        int startX = actionBarStartX();
        if (mouseY < actionBarY() || mouseY >= actionBarY() + 28) {
            return -1;
        }
        for (int slot = 0; slot < PlayerClassData.ACTION_BAR_SLOT_COUNT; slot++) {
            int x = startX + slot * (slotWidth + SLOT_GAP);
            if (inside(mouseX, mouseY, x, actionBarY(), slotWidth, 28)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private enum SkillPage {
        ACTIVE,
        PASSIVE
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}
