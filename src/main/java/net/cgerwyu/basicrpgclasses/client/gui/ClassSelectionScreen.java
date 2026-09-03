package net.cgerwyu.basicrpgclasses.client.gui;

import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.RpgClass;
import net.cgerwyu.basicrpgclasses.network.payload.ChooseClassPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class ClassSelectionScreen extends Screen {
    private final List<Button> classButtons = new ArrayList<>();
    private boolean waitingForServer;

    public ClassSelectionScreen() {
        super(Component.translatable("screen.basicrpgclasses.class_selection.title"));
    }

    @Override
    protected void init() {
        classButtons.clear();
        int buttonWidth = 150;
        int startX = width / 2 - buttonWidth / 2;
        int startY = height / 2 - 61;
        addClassButton(RpgClass.WARRIOR, startX, startY, buttonWidth);
        addClassButton(RpgClass.MAGE, startX, startY + 26, buttonWidth);
        addClassButton(RpgClass.HUNTER, startX, startY + 52, buttonWidth);
        addClassButton(RpgClass.PRIEST, startX, startY + 78, buttonWidth);
        addClassButton(RpgClass.PALADIN, startX, startY + 104, buttonWidth);
    }

    private void addClassButton(RpgClass rpgClass, int x, int y, int width) {
        Button button = Button.builder(Component.translatable(rpgClass.translationKey()), ignored -> choose(rpgClass))
                .bounds(x, y, width, 20)
                .build();
        classButtons.add(addRenderableWidget(button));
    }

    private void choose(RpgClass rpgClass) {
        if (waitingForServer) {
            return;
        }
        waitingForServer = true;
        classButtons.forEach(button -> button.active = false);
        ClientPacketDistributor.sendToServer(new ChooseClassPayload(rpgClass.numericId()));
    }

    @Override
    public void tick() {
        if (minecraft.player != null && minecraft.player.getData(ModAttachments.PLAYER_CLASS).hasClass()) {
            minecraft.gui.setScreen(null);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = 250;
        int panelHeight = 212;
        int x = width / 2 - panelWidth / 2;
        int y = height / 2 - panelHeight / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xD0101018);
        graphics.outline(x, y, panelWidth, panelHeight, 0xFF8B734B);
        graphics.centeredText(font, title, width / 2, y + 16, 0xFFFFFFFF);
        graphics.centeredText(font, Component.translatable("screen.basicrpgclasses.class_selection.warning"), width / 2, y + 31, 0xFFFFB85C);
        graphics.centeredText(font, Component.translatable(waitingForServer
                ? "screen.basicrpgclasses.class_selection.waiting"
                : "screen.basicrpgclasses.class_selection.hint"), width / 2, y + 189, 0xFFAAAAAA);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
