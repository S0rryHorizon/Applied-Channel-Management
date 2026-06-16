package com.s0rryhorizon.appliedchannelmanagement.blockentity;

import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import com.s0rryhorizon.appliedchannelmanagement.config.AcmServerConfig;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmBlockEntities;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmItems;
import com.s0rryhorizon.appliedchannelmanagement.runtime.WirelessLinkManager;

public final class ChannelDistributorBlockEntity extends AbstractChannelDeviceBlockEntity {
    private UUID distributorId = UUID.randomUUID();
    private String deviceName = "";
    @Nullable
    private UUID targetHubId;
    private int priority;

    public ChannelDistributorBlockEntity(BlockPos pos, BlockState state) {
        super(AcmBlockEntities.DISTRIBUTOR.get(), pos, state, 0);
        mainNode.setVisualRepresentation(AcmItems.ME_CHANNEL_DISTRIBUTOR.get());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChannelDistributorBlockEntity distributor) {
        WirelessLinkManager.register(distributor);
    }

    public UUID getDistributorId() {
        return distributorId;
    }

    public Optional<UUID> getTargetHubId() {
        return Optional.ofNullable(targetHubId);
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean setDeviceName(String name) {
        String trimmed = name.trim();
        if (trimmed.length() > AcmServerConfig.MAX_NAME_LENGTH.get()) {
            return false;
        }
        deviceName = trimmed;
        setChanged();
        WirelessLinkManager.requestReconcile();
        return true;
    }

    public void bindTo(@Nullable UUID hubId) {
        targetHubId = hubId;
        setChanged();
        WirelessLinkManager.requestReconcile();
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
        setChanged();
        WirelessLinkManager.requestReconcile();
    }

    public void setLinkPowerUsage(ChannelHubBlockEntity hub) {
        mainNode.setIdlePowerUsage(AcmServerConfig.getDistributorPower(this, hub));
    }

    public void clearLinkPowerUsage() {
        mainNode.setIdlePowerUsage(0);
    }

    @Override
    protected void loadDeviceData(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.hasUUID("distributor_id")) {
            distributorId = tag.getUUID("distributor_id");
        }
        deviceName = tag.getString("device_name");
        targetHubId = tag.hasUUID("target_hub") ? tag.getUUID("target_hub") : null;
        priority = tag.getInt("priority");
    }

    @Override
    protected void saveDeviceData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putUUID("distributor_id", distributorId);
        tag.putString("device_name", deviceName);
        if (targetHubId != null) {
            tag.putUUID("target_hub", targetHubId);
        }
        tag.putInt("priority", priority);
    }

    @Override
    protected void unregisterRuntime() {
        WirelessLinkManager.unregister(this);
    }

    @Override
    public String getStatusText() {
        return "ME Channel Distributor: " + (WirelessLinkManager.isLinked(distributorId) ? "online" : "offline")
                + ", priority " + priority;
    }
}
