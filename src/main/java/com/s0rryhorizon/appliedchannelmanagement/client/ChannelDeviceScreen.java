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
    private static final int DETAIL_ROWS = 4;

    private EditBox primaryInput;
    private EditBox secondaryInput;
    private EditBox priorityInput;
    private List<String> selectablePlayers = List.of();
    private List<String> selectableHubs = List.of();
    private List<String> details = List.of();
    private Dropdown dropdown = Dropdown.NONE;
    private int dropdownScroll;
    private int detailScroll;

    public ChannelDeviceScreen(ChannelDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 260;
        imageHeight = 218;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos + 12;
        int y = topPos + 34;
        selectablePlayers = splitOptions(menu.availablePlayers());
        selectableHubs = splitOptions(menu.authorizedHubs());
        details = splitLines(menu.details());
        primaryInput = new EditBox(font, x, y, 162, 20,
                tr(menu.isHub() ? "field.hub_name" : "field.distributor_name"));
        primaryInput.setMaxLength(128);
        primaryInput.setValue(menu.initialName());
        addRenderableWidget(primaryInput);

        secondaryInput = new EditBox(font, x, y + 44, 162, 20,
                tr(menu.isHub() ? "field.whitelist" : "field.target_hub"));
        secondaryInput.setMaxLength(menu.isHub() ? 4096 : 128);
        secondaryInput.setValue(menu.isHub() ? menu.initialWhitelist() : menu.initialTarget());
        addRenderableWidget(secondaryInput);

        addRenderableWidget(Button.builder(tr("button.apply"), button -> applyChanges())
                .bounds(leftPos + 186, topPos + imageHeight - 30, 62, 20).build());
        if (menu.isHub()) {
            addRenderableWidget(Button.builder(tr("button.players"), button -> toggleDropdown(Dropdown.PLAYERS))
                    .bounds(leftPos + 186, y + 44, 62, 20).build());
        } else {
            priorityInput = new EditBox(font, x, y + 88, 162, 20, tr("field.priority"));
            priorityInput.setMaxLength(16);
            priorityInput.setValue(Integer.toString(menu.initialPriority()));
            addRenderableWidget(priorityInput);

            addRenderableWidget(Button.builder(tr("button.hubs"), button -> toggleDropdown(Dropdown.HUBS))
                    .bounds(leftPos + 186, y + 44, 62, 20).build());
            addRenderableWidget(Button.builder(tr("button.unbind"), button -> {
                secondaryInput.setValue("");
                applyChanges();
            }).bounds(leftPos + 186, y + 88, 62, 20).build());
        }
    }

    private static Component tr(String key) {
        return Component.translatable("screen.applied_channel_management." + key);
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

    private static List<String> splitLines(String value) {
        if (value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private void toggleDropdown(Dropdown target) {
        dropdown = dropdown == target ? Dropdown.NONE : target;
        dropdownScroll = 0;
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
        secondaryInput.setValue(hubName);
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
        return topPos + 99;
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
            PacketDistributor.sendToServer(new DeviceActionPayload(menu.position(), "distributor_name",
                    primaryInput.getValue()));
            PacketDistributor.sendToServer(new DeviceActionPayload(menu.position(), "distributor_target",
                    secondaryInput.getValue()));
            PacketDistributor.sendToServer(new DeviceActionPayload(menu.position(), "distributor_priority",
                    priorityInput.getValue()));
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 8, topPos + 20, leftPos + imageWidth - 8, topPos + 22, 0xFF4CB8D8);
        drawInset(graphics, leftPos + 9, topPos + 31, 168, 26);
        drawInset(graphics, leftPos + 9, topPos + 75, 168, 26);
        if (menu.isHub()) {
            drawInset(graphics, leftPos + 9, topPos + 116, imageWidth - 18, 64);
        } else {
            drawInset(graphics, leftPos + 9, topPos + 119, 168, 26);
            drawInset(graphics, leftPos + 9, topPos + 153, imageWidth - 18, 28);
        }
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
            graphics.drawString(font, tr("label.name"), 12, 24, 0x304850, false);
            graphics.drawString(font, tr("label.whitelist"), 12, 68, 0x304850, false);
            graphics.drawString(font, Component.translatable("screen.applied_channel_management.stats",
                    menu.totalCapacity(), menu.wiredUsed(), menu.wirelessUsed()), 12, 105, 0x304850, false);
            graphics.drawString(font, Component.translatable("screen.applied_channel_management.connected_distributors"),
                    12, 119, 0xE6F7FF, false);
            renderDetailList(graphics, 12, 131, imageWidth - 28);
            graphics.drawString(font, Component.translatable("screen.applied_channel_management.status",
                    menu.online() ? tr("online") : tr("offline")), 12, 185,
                    menu.online() ? 0x167D42 : 0x9B2D2D, false);
        } else {
            graphics.drawString(font, tr("label.distributor_name"), 12, 24, 0x304850, false);
            graphics.drawString(font, tr("label.target_hub"), 12, 68, 0x304850, false);
            graphics.drawString(font, tr("label.priority"), 12, 112, 0x304850, false);
            graphics.drawString(font, Component.translatable("screen.applied_channel_management.status",
                    menu.online() ? tr("online") : tr("offline")), 12, 158,
                    menu.online() ? 0x6EFFA5 : 0xFF7777, false);
            graphics.drawString(font, Component.translatable("screen.applied_channel_management.pool",
                    menu.wiredUsed(), menu.wirelessUsed(), menu.totalCapacity()), 12, 170, 0xE6F7FF, false);
        }
        graphics.drawString(font, tr("validated_by_server"), 12, 203, 0x56636A, false);
    }

    private void renderDetailList(GuiGraphics graphics, int x, int y, int width) {
        if (details.isEmpty()) {
            graphics.drawString(font, tr("none"), x, y, 0x8CA0A8, false);
            return;
        }
        detailScroll = clamp(detailScroll, 0, Math.max(0, details.size() - DETAIL_ROWS));
        int visible = Math.min(DETAIL_ROWS, details.size() - detailScroll);
        for (int index = 0; index < visible; index++) {
            String detail = details.get(detailScroll + index);
            graphics.drawString(font, font.plainSubstrByWidth(detail, width), x, y + index * 12, 0xD8E8EE, false);
        }
        if (details.size() > DETAIL_ROWS) {
            graphics.drawString(font, (detailScroll + 1) + "/" + details.size(), width - 18, y + 43, 0x8CA0A8,
                    false);
        }
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
        dropdownScroll = clamp(dropdownScroll, 0, Math.max(0, options.size() - DROPDOWN_MAX_OPTIONS));
        int visible = Math.min(DROPDOWN_MAX_OPTIONS, options.size() - dropdownScroll);
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
            String option = options.get(dropdownScroll + index);
            boolean selected = dropdown == Dropdown.PLAYERS
                    && splitOptions(secondaryInput.getValue()).stream().anyMatch(option::equalsIgnoreCase);
            int color = selected ? 0xFF1D5B47 : hovered ? 0xFF2E4A55 : 0xFF182028;
            graphics.fill(x + 2, optionY, x + width - 2, optionY + DROPDOWN_OPTION_HEIGHT, color);
            String marker = selected ? "* " : "";
            graphics.drawString(font, marker + font.plainSubstrByWidth(option, width - 18),
                    x + 6, optionY + 3, hovered ? 0xA8F0FF : 0xD8E8EE, false);
        }
        if (options.size() > visible) {
            graphics.drawString(font, (dropdownScroll + 1) + "/" + options.size(), x + width - 42,
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
            int visible = Math.min(DROPDOWN_MAX_OPTIONS, options.size() - dropdownScroll);
            int height = visible * DROPDOWN_OPTION_HEIGHT + 2;
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                int index = (int) ((mouseY - y - 1) / DROPDOWN_OPTION_HEIGHT);
                if (index >= 0 && index < visible) {
                    chooseDropdownOption(options.get(dropdownScroll + index));
                    return true;
                }
            } else {
                dropdown = Dropdown.NONE;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<String> options = dropdownOptions();
        if (!options.isEmpty() && isMouseOverDropdown(mouseX, mouseY) && options.size() > DROPDOWN_MAX_OPTIONS) {
            dropdownScroll = clamp(dropdownScroll - (int) Math.signum(scrollY), 0,
                    options.size() - DROPDOWN_MAX_OPTIONS);
            return true;
        }
        if (menu.isHub() && isMouseOverDetails(mouseX, mouseY) && details.size() > DETAIL_ROWS) {
            detailScroll = clamp(detailScroll - (int) Math.signum(scrollY), 0, details.size() - DETAIL_ROWS);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean isMouseOverDropdown(double mouseX, double mouseY) {
        int x = dropdownLeft();
        int y = dropdownTop();
        int visible = Math.min(DROPDOWN_MAX_OPTIONS, dropdownOptions().size());
        return mouseX >= x && mouseX < x + dropdownWidth()
                && mouseY >= y && mouseY < y + visible * DROPDOWN_OPTION_HEIGHT + 2;
    }

    private boolean isMouseOverDetails(double mouseX, double mouseY) {
        return mouseX >= leftPos + 9 && mouseX < leftPos + imageWidth - 9
                && mouseY >= topPos + 116 && mouseY < topPos + 180;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Dropdown {
        NONE,
        PLAYERS,
        HUBS
    }
}
