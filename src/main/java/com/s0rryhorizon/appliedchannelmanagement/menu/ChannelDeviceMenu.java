package com.s0rryhorizon.appliedchannelmanagement.menu;

import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import com.s0rryhorizon.appliedchannelmanagement.blockentity.AbstractChannelDeviceBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelDistributorBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelHubBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.data.HubRegistrySavedData;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmMenus;
import com.s0rryhorizon.appliedchannelmanagement.runtime.WirelessLinkManager;

public final class ChannelDeviceMenu extends AbstractContainerMenu {
    private final BlockPos position;
    private final boolean hub;
    private final String initialName;
    private final String initialWhitelist;
    private final String initialTarget;
    private final int initialPriority;
    private final int totalCapacity;
    private final int wiredUsed;
    private final int wirelessUsed;
    private final String authorizedHubs;
    private final String availablePlayers;
    private final boolean online;
    private final String details;

    public ChannelDeviceMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        super(AcmMenus.CHANNEL_DEVICE.get(), containerId);
        position = buffer.readBlockPos();
        hub = buffer.readBoolean();
        initialName = buffer.readUtf(128);
        initialWhitelist = buffer.readUtf(4096);
        initialTarget = buffer.readUtf(128);
        initialPriority = buffer.readInt();
        totalCapacity = buffer.readInt();
        wiredUsed = buffer.readInt();
        wirelessUsed = buffer.readInt();
        authorizedHubs = buffer.readUtf(4096);
        availablePlayers = buffer.readUtf(4096);
        online = buffer.readBoolean();
        details = buffer.readUtf(4096);
    }

    public ChannelDeviceMenu(int containerId, Inventory inventory, BlockPos position) {
        super(AcmMenus.CHANNEL_DEVICE.get(), containerId);
        this.position = position;
        var device = inventory.player.level().getBlockEntity(position);
        this.hub = device instanceof ChannelHubBlockEntity;
        this.initialName = hub ? ((ChannelHubBlockEntity) device).getNetworkName() : "";
        this.initialWhitelist = "";
        this.initialTarget = "";
        this.initialPriority = device instanceof ChannelDistributorBlockEntity distributor
                ? distributor.getPriority() : 0;
        this.totalCapacity = 0;
        this.wiredUsed = 0;
        this.wirelessUsed = 0;
        this.authorizedHubs = "";
        this.availablePlayers = "";
        this.online = false;
        this.details = "";
    }

    public static void writeOpeningData(RegistryFriendlyByteBuf buffer, AbstractChannelDeviceBlockEntity device,
            ServerPlayer player) {
        buffer.writeBlockPos(device.getBlockPos());
        boolean isHub = device instanceof ChannelHubBlockEntity;
        buffer.writeBoolean(isHub);
        if (device instanceof ChannelHubBlockEntity hub) {
            buffer.writeUtf(hub.getNetworkName(), 128);
            buffer.writeUtf(hub.getWhitelist().stream()
                    .map(id -> {
                        ServerPlayer online = player.getServer().getPlayerList().getPlayer(id);
                        return online != null ? online.getGameProfile().getName() : id.toString();
                    })
                    .sorted()
                    .collect(Collectors.joining(",")), 4096);
            buffer.writeUtf("", 128);
            buffer.writeInt(0);
            buffer.writeInt(hub.getSnapshot().totalCapacity());
            buffer.writeInt(hub.getSnapshot().wiredUsed());
            buffer.writeInt(hub.getSnapshot().wirelessUsed());
        } else if (device instanceof ChannelDistributorBlockEntity distributor) {
            buffer.writeUtf("", 128);
            buffer.writeUtf("", 4096);
            var data = HubRegistrySavedData.get(player.getServer());
            String targetName = distributor.getTargetHubId().flatMap(data::get)
                    .map(HubRegistrySavedData.HubRecord::name).orElse("");
            buffer.writeUtf(targetName, 128);
            buffer.writeInt(distributor.getPriority());
            var targetHub = distributor.getTargetHubId().flatMap(WirelessLinkManager::getLoadedHub);
            buffer.writeInt(targetHub.map(hub -> hub.getSnapshot().totalCapacity()).orElse(0));
            buffer.writeInt(targetHub.map(hub -> hub.getSnapshot().wiredUsed()).orElse(0));
            buffer.writeInt(targetHub.map(hub -> hub.getSnapshot().wirelessUsed()).orElse(0));
        }
        String names = HubRegistrySavedData.get(player.getServer()).all().stream()
                .filter(record -> player.hasPermissions(2) || record.canUse(player.getUUID()))
                .sorted(Comparator.comparing(HubRegistrySavedData.HubRecord::normalizedName))
                .map(HubRegistrySavedData.HubRecord::name)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(", "));
        buffer.writeUtf(names, 4096);
        String playerNames = player.getServer().getPlayerList().getPlayers().stream()
                .map(serverPlayer -> serverPlayer.getGameProfile().getName())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
        buffer.writeUtf(playerNames, 4096);
        boolean online = device instanceof ChannelDistributorBlockEntity distributor
                ? WirelessLinkManager.isLinked(distributor.getDistributorId())
                : device.getMainNode().isReady() && device.getMainNode().isPowered();
        buffer.writeBoolean(online);
        String details = device instanceof ChannelHubBlockEntity hub
                ? WirelessLinkManager.getConnectedDistributors(hub.getHubId()).stream()
                        .map(UUID::toString).collect(Collectors.joining(", "))
                : "";
        buffer.writeUtf(details, 4096);
    }

    public BlockPos position() {
        return position;
    }

    public boolean isHub() {
        return hub;
    }

    public String initialName() {
        return initialName;
    }

    public String initialWhitelist() {
        return initialWhitelist;
    }

    public String initialTarget() {
        return initialTarget;
    }

    public int initialPriority() {
        return initialPriority;
    }

    public int totalCapacity() {
        return totalCapacity;
    }

    public int wiredUsed() {
        return wiredUsed;
    }

    public int wirelessUsed() {
        return wirelessUsed;
    }

    public String authorizedHubs() {
        return authorizedHubs;
    }

    public String availablePlayers() {
        return availablePlayers;
    }

    public boolean online() {
        return online;
    }

    public String details() {
        return details;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5) <= 64
                && player.level().getBlockEntity(position) instanceof AbstractChannelDeviceBlockEntity;
    }
}
