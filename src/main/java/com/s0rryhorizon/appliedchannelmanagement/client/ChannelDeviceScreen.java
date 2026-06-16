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
    private static final int DROPDOWN_OPTION_HEIGHT = 14;
    private static final int DROPDOWN_MAX_OPTIONS = 6;

    private EditBox primaryInput;
    private EditBox secondaryInput;
    private List<String> selectablePlayers = List.of();
    private List<String> selectableHubs = List.of();
    private Dropdown dropdown = Dropdown.NONE;

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
                .bounds(leftPos + 174, topPos + 146, 62, 20).build());
        if (menu.isHub()) {
            addRenderableWidget(Button.builder(Component.literal("Players v"), button -> toggleDropdown(Dropdown.PLAYERS))
                    .bounds(leftPos + 174, y + 42, 62, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.literal("Hubs v"), button -> toggleDropdown(Dropdown.HUBS))
                    .bounds(leftPos + 174, y, 62, 20).build());
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

    private void toggleDropdown(Dropdown target) {
        dropdown = dropdown == target ? Dropdown.NONE : target;
    }

    private void appendPlayer(String playerName) {
        List<String> current = splitOptions(secondaryInput.getValue());
        if (current.stream().noneMatch(playerName::equalsIgnoreCase)) {
            secondaryInput.setValue(secondaryInput.getValue().isBlank()
                    ? playerName
                    : secondaryInput.getValue() + "," + playerName);
        }
    }

    private void selectHub(String hubName) {
        primaryInput.setValue(hubName);
    }

    private List<String> dropdownOptions() {
        return switch (dropdown) {
            case PLAYERS -> selectablePlayers;
            case HUBS -> selectableHubs;
            case NONE -> List.of();
        };
    }

    private int dropdownLeft() {
        return leftPos + 12;
    }

    private int dropdownTop() {
        int inputY = topPos + 34;
        return dropdown == Dropdown.PLAYERS ? inputY + 63 : inputY + 21;
    }

    private int dropdownWidth() {
        return imageWidth - 24;
    }

    private void chooseDropdownOption(String option) {
        if (dropdown == Dropdown.PLAYERS) {
            appendPlayer(option);
        } else if (dropdown == Dropdown.HUBS) {
            selectHub(option);
        }
        dropdown = Dropdown.NONE;
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
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 8, topPos + 20, leftPos + imageWidth - 8, topPos + 22, 0xFF4CB8D8);
        drawInset(graphics, leftPos + 9, topPos + 31, 160, 26);
        drawInset(graphics, leftPos + 9, topPos + 73, 160, 26);
        drawInset(graphics, leftPos + 9, topPos + 101, imageWidth - 18, 40);
    }

    private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF363D42);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFE1E4E5);
        graphics.fill(x + 3, y + 3, x + width - 3, y + height - 3, 0xFFBBC3C7);
        graphics.fill(x + 5, y + 5, x + width - 5, y + height - 5, 0xFFD3D8DA);
        graphics.fill(x + 5, y + height - 6, x + width - 5, y + height - 5, 0xFF8A969D);
        graphics.fill(x + width - 6, y + 5, x + width - 5, y + height - 5, 0xFF8A969D);
    }

    private static void drawInset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF7D8990);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF31383E);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF111820);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 10, 0x263238, false);
        if (menu.isHub()) {
            graphics.drawString(font, "Name", 12, 24, 0x304850, false);
            graphics.drawString(font, "Whitelist: choose online players or type UUIDs", 12, 66, 0x304850,
                    false);
            graphics.drawString(font, "Capacity " + menu.totalCapacity() + " | Wired " + menu.wiredUsed()
                    + " | Wireless " + menu.wirelessUsed(), 12, 104, 0xE6F7FF, false);
            graphics.drawString(font, "Status: " + (menu.online() ? "online" : "offline"), 12, 116,
                    menu.online() ? 0x6EFFA5 : 0xFF7777, false);
            graphics.drawString(font, "Distributors: " + (menu.details().isBlank() ? "none" : menu.details()),
                    12, 128, 0xE6F7FF, false);
            graphics.drawString(font, "Online players: " + (menu.availablePlayers().isBlank() ? "none"
                    : menu.availablePlayers()), 12, 140, 0x304850, false);
        } else {
            graphics.drawString(font, "Target hub", 12, 24, 0x304850, false);
            graphics.drawString(font, "Integer priority (higher first)", 12, 66, 0x304850, false);
            graphics.drawString(font, "Authorized hubs: " + menu.authorizedHubs(), 12, 104, 0xE6F7FF, false);
            graphics.drawString(font, "Status: " + (menu.online() ? "online" : "offline")
                    + " | Pool " + menu.wiredUsed() + "+" + menu.wirelessUsed() + "/" + menu.totalCapacity(),
                    12, 116, menu.online() ? 0x6EFFA5 : 0xFF7777, false);
        }
        graphics.drawString(font, "Changes are validated by the server.", 12, 154, 0x56636A, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderDropdown(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        List<String> options = dropdownOptions();
        if (options.isEmpty()) {
            return;
        }
        int visible = Math.min(DROPDOWN_MAX_OPTIONS, options.size());
        int x = dropdownLeft();
        int y = dropdownTop();
        int width = dropdownWidth();
        int height = visible * DROPDOWN_OPTION_HEIGHT + 2;
        graphics.fill(x, y, x + width, y + height, 0xFF263238);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF111820);
        for (int index = 0; index < visible; index++) {
            int optionY = y + 1 + index * DROPDOWN_OPTION_HEIGHT;
            boolean hovered = mouseX >= x + 1 && mouseX < x + width - 1
                    && mouseY >= optionY && mouseY < optionY + DROPDOWN_OPTION_HEIGHT;
            String option = options.get(index);
            boolean selected = dropdown == Dropdown.PLAYERS
                    && splitOptions(secondaryInput.getValue()).stream().anyMatch(option::equalsIgnoreCase);
            int color = selected ? 0xFF1D5B47 : hovered ? 0xFF2E4A55 : 0xFF182028;
            graphics.fill(x + 2, optionY, x + width - 2, optionY + DROPDOWN_OPTION_HEIGHT, color);
            String marker = selected ? "* " : "";
            graphics.drawString(font, marker + font.plainSubstrByWidth(option, width - 18),
                    x + 6, optionY + 3, hovered ? 0xA8F0FF : 0xD8E8EE, false);
        }
        if (options.size() > visible) {
            graphics.drawString(font, "+" + (options.size() - visible) + " more", x + width - 56,
                    y + height - 11, 0x8CA0A8, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<String> options = dropdownOptions();
        if (!options.isEmpty()) {
            int x = dropdownLeft();
            int y = dropdownTop();
            int width = dropdownWidth();
            int visible = Math.min(DROPDOWN_MAX_OPTIONS, options.size());
            int height = visible * DROPDOWN_OPTION_HEIGHT + 2;
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                int index = (int) ((mouseY - y - 1) / DROPDOWN_OPTION_HEIGHT);
                if (index >= 0 && index < visible) {
                    chooseDropdownOption(options.get(index));
                    return true;
                }
            } else {
                dropdown = Dropdown.NONE;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private enum Dropdown {
        NONE,
        PLAYERS,
        HUBS
    }
}
