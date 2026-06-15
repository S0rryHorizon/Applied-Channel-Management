package com.s0rryhorizon.appliedchannelmanagement.blockentity;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.s0rryhorizon.appliedchannelmanagement.config.AcmServerConfig;
import com.s0rryhorizon.appliedchannelmanagement.grid.ChannelPoolSnapshot;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmBlockEntities;
import com.s0rryhorizon.appliedchannelmanagement.runtime.WirelessLinkManager;

public final class ChannelHubBlockEntity extends AbstractChannelDeviceBlockEntity {
    private UUID hubId = UUID.randomUUID();
    private String networkName = "";
    private final Set<UUID> whitelist = new HashSet<>();
    private ChannelPoolSnapshot snapshot = ChannelPoolSnapshot.EMPTY;

    public ChannelHubBlockEntity(BlockPos pos, BlockState state) {
        super(AcmBlockEntities.HUB.get(), pos, state, AcmServerConfig.HUB_POWER.getDefault());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChannelHubBlockEntity hub) {
        hub.mainNode.setIdlePowerUsage(AcmServerConfig.HUB_POWER.get());
        WirelessLinkManager.register(hub);
    }

    public UUID getHubId() {
        return hubId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public String getNormalizedName() {
        return normalizeName(networkName);
    }

    public Set<UUID> getWhitelist() {
        return Set.copyOf(whitelist);
    }

    public boolean canUse(UUID playerId) {
        return getOwnerId().equals(playerId) || whitelist.contains(playerId);
    }

    public boolean setNetworkName(String name) {
        var trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > AcmServerConfig.MAX_NAME_LENGTH.get()) {
            return false;
        }
        this.networkName = trimmed;
        setChanged();
        return true;
    }

    public void setWhitelist(Set<UUID> players) {
        whitelist.clear();
        whitelist.addAll(players);
        setChanged();
    }

    public ChannelPoolSnapshot getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(ChannelPoolSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public ServerLevel getServerLevel() {
        return (ServerLevel) getLevel();
    }

    @Override
    protected void loadDeviceData(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.hasUUID("hub_id")) {
            hubId = tag.getUUID("hub_id");
        }
        networkName = tag.getString("network_name");
        whitelist.clear();
        ListTag entries = tag.getList("whitelist", Tag.TAG_INT_ARRAY);
        for (Tag entry : entries) {
            whitelist.add(net.minecraft.nbt.NbtUtils.loadUUID(entry));
        }
    }

    @Override
    protected void saveDeviceData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putUUID("hub_id", hubId);
        tag.putString("network_name", networkName);
        ListTag entries = new ListTag();
        whitelist.forEach(id -> entries.add(net.minecraft.nbt.NbtUtils.createUUID(id)));
        tag.put("whitelist", entries);
    }

    @Override
    protected void unregisterRuntime() {
        WirelessLinkManager.unregister(this);
    }

    @Override
    public String getStatusText() {
        var name = networkName.isBlank() ? "<unnamed>" : networkName;
        return "ME Channel Hub " + name + ": " + snapshot.wiredUsed() + " wired + "
                + snapshot.wirelessUsed() + " wireless / " + snapshot.totalCapacity();
    }

    public static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
