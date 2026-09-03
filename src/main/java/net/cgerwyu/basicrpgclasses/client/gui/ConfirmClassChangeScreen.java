package net.cgerwyu.basicrpgclasses.client.gui;

import net.cgerwyu.basicrpgclasses.data.ClassChangeRules;
import net.cgerwyu.basicrpgclasses.data.ModAttachments;
import net.cgerwyu.basicrpgclasses.data.PlayerClassData;
import net.cgerwyu.basicrpgclasses.network.payload.RequestClassChangePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ConfirmClassChangeScreen extends Screen {
    private Button confirmButton;
    private Button cancelButton;
    private boolean waitingForServer;

    public ConfirmClassChangeScreen() {
        super(Component.translatable("screen.basicrpgclasses.class_change.title"));
    }

    @Override
    protected void init() {
        confirmButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.basicrpgclasses.class_change.confirm").withStyle(ChatFormatting.RED),
                        ignored -> confirmChange()
                )
                .bounds(width / 2 - 105, height / 2 + 48, 100, 20)
                .build());
        cancelButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.cancel"),
                        ignored -> minecraft.gui.setScreen(new ClassProgressionScreen())
                )
                .bounds(width / 2 + 5, height / 2 + 48, 100, 20)
                .build());
    }

    private void confirmChange() {
        if (waitingForServer) {
            return;
        }
        waitingForServer = true;
        confirmButton.active = false;
        cancelButton.active = false;
        ClientPacketDistributor.sendToServer(RequestClassChangePayload.INSTANCE);
    }

    @Override
    public void tick() {
        if (waitingForServer
                && minecraft.player != null
                && !minecraft.player.getData(ModAttachments.PLAYER_CLASS).hasClass()) {
            minecraft.gui.setScreen(new ClassSelectionScreen());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int panelWidth = 330;
        int panelHeight = 190;
        int x = width / 2 - panelWidth / 2;
        int y = height / 2 - panelHeight / 2;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xE0120D0D);
        graphics.outline(x, y, panelWidth, panelHeight, 0xFFB34E3F);
        graphics.centeredText(font, title, width / 2, y + 16, 0xFFFFB6A8);

        if (minecraft.player != null) {
            PlayerClassData data = minecraft.player.getData(ModAttachments.PLAYER_CLASS);
            int totalSpent = ClassChangeRules.totalTrackedCost(data);
            int refund = ClassChangeRules.refundLevels(data);
            graphics.centeredText(font, Component.translatable(
                    "screen.basicrpgclasses.class_change.current",
                    Component.translatable(data.rpgClass().translationKey()),
                    data.classLevel()
            ), width / 2, y + 43, 0xFFFFFFFF);
            graphics.centeredText(font, Component.translatable(
                    "screen.basicrpgclasses.class_change.spent",
                    totalSpent
            ), width / 2, y + 61, 0xFFFFD36A);
            graphics.centeredText(font, Component.translatable(
                    "screen.basicrpgclasses.class_change.refund",
                    refund
            ), width / 2, y + 79, 0xFF80FF80);
        }

        graphics.centeredText(font, Component.translatable("screen.basicrpgclasses.class_change.warning_reset"), width / 2, y + 103, 0xFFFF8E7A);
        graphics.centeredText(font, Component.translatable("screen.basicrpgclasses.class_change.warning_rounding"), width / 2, y + 119, 0xFFAAAAAA);
        if (waitingForServer) {
            graphics.centeredText(font, Component.translatable("screen.basicrpgclasses.class_change.waiting"), width / 2, y + 164, 0xFFAAAAAA);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (!waitingForServer) {
            minecraft.gui.setScreen(new ClassProgressionScreen());
        }
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
