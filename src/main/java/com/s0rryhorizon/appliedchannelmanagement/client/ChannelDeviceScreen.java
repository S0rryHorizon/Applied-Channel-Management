package com.s0rryhorizon.appliedchannelmanagement.client;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import com.s0rryhorizon.appliedchannelmanagement.menu.ChannelDeviceMenu;
import com.s0rryhorizon.appliedchannelmanagement.network.DeviceActionPayload;

public final class ChannelDeviceScreen extends AbstractContainerScreen<ChannelDeviceMenu> {
    private EditBox primaryInput;
    private EditBox secondaryInput;
    private List<String> selectablePlayers = List.of();
    private List<String> selectableHubs = List.of();
    private int selectedPlayerIndex;
    private int selectedHubIndex;

    public ChannelDeviceScreen(ChannelDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 176;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 12;
        int y = topPos + 34;
        selectablePlayers = splitOptions(menu.availablePlayers());
        selectableHubs = splitOptions(menu.authorizedHubs());
        primaryInput = new EditBox(font, x, y, 154, 20,
                Component.literal(menu.isHub() ? "Hub name" : "Target hub"));
        primaryInput.setMaxLength(128);
        primaryInput.setValue(menu.isHub() ? menu.initialName() : menu.initialTarget());
        addRenderableWidget(primaryInput);

        secondaryInput = new EditBox(font, x, y + 42, 154, 20,
                Component.literal(menu.isHub() ? "Whitelist" : "Priority"));
        secondaryInput.setMaxLength(menu.isHub() ? 4096 : 16);
        secondaryInput.setValue(menu.isHub() ? menu.initialWhitelist() : Integer.toString(menu.initialPriority()));
        addRenderableWidget(secondaryInput);

        addRenderableWidget(Button.builder(Component.literal("Apply"), button -> applyChanges())
                .bounds(leftPos + 174, y, 62, 20).build());
        if (menu.isHub()) {
            addRenderableWidget(Button.builder(Component.literal("Player +"), button -> appendNextPlayer())
                    .bounds(leftPos + 174, y + 42, 62, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Hub +"), button -> selectNextHub())
                    .bounds(leftPos + 174, y + 21, 62, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Unbind"), button -> {
                primaryInput.setValue("");
                applyChanges();
            }).bounds(leftPos + 174, y + 42, 62, 20).build());
        }
    }

    private static List<String> splitOptions(String value) {
        if (value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(option -> !option.isBlank())
                .toList();
    }

    private void appendNextPlayer() {
        if (selectablePlayers.isEmpty()) {
            return;
        }
        String playerName = selectablePlayers.get(selectedPlayerIndex++ % selectablePlayers.size());
        List<String> current = splitOptions(secondaryInput.getValue());
        if (current.stream().noneMatch(playerName::equalsIgnoreCase)) {
            secondaryInput.setValue(secondaryInput.getValue().isBlank()
                    ? playerName
                    : secondaryInput.getValue() + "," + playerName);
        }
    }

    private void selectNextHub() {
        if (!selectableHubs.isEmpty()) {
            primaryInput.setValue(selectableHubs.get(selectedHubIndex++ % selectableHubs.size()));
        }
    }

    private void applyChanges() {
        if (menu.isHub()) {
            PacketDistributor.sendToServer(new DeviceActionPayload(menu.position(), "hub_name", primaryInput.getValue()));
            PacketDistributor.sendToServer(new DeviceActionPayload(menu.position(), "hub_acl", secondaryInput.getValue()));
        } else {
            PacketDistributor.sendToServer(new DeviceActionPayload(menu.position(), "distributor_target",
                    primaryInput.getValue()));
            PacketDistributor.sendToServer(new DeviceActionPayload(menu.position(), "distributor_priority",
                    secondaryInput.getValue()));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202830);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF10161C);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 10, 0xFFFFFF, false);
        if (menu.isHub()) {
            graphics.drawString(font, "Name", 12, 24, 0xA8D8FF, false);
            graphics.drawString(font, "Whitelist: online names or UUIDs, comma separated", 12, 66, 0xA8D8FF,
                    false);
            graphics.drawString(font, "Capacity " + menu.totalCapacity() + " | Wired " + menu.wiredUsed()
                    + " | Wireless " + menu.wirelessUsed(), 12, 104, 0xFFFFFF, false);
            graphics.drawString(font, "Status: " + (menu.online() ? "online" : "offline"), 12, 116,
                    menu.online() ? 0x66FF99 : 0xFF7777, false);
            graphics.drawString(font, "Distributors: " + (menu.details().isBlank() ? "none" : menu.details()),
                    12, 128, 0xFFFFFF, false);
            graphics.drawString(font, "Online players: " + (menu.availablePlayers().isBlank() ? "none"
                    : menu.availablePlayers()), 12, 140, 0xFFFFFF, false);
        } else {
            graphics.drawString(font, "Target hub name", 12, 24, 0xA8D8FF, false);
            graphics.drawString(font, "Integer priority (higher first)", 12, 66, 0xA8D8FF, false);
            graphics.drawString(font, "Authorized hubs: " + menu.authorizedHubs(), 12, 104, 0xFFFFFF, false);
            graphics.drawString(font, "Status: " + (menu.online() ? "online" : "offline")
                    + " | Pool " + menu.wiredUsed() + "+" + menu.wirelessUsed() + "/" + menu.totalCapacity(),
                    12, 116, menu.online() ? 0x66FF99 : 0xFF7777, false);
        }
        graphics.drawString(font, "Changes are validated by the server.", 12, 146, 0x8C98A4, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
