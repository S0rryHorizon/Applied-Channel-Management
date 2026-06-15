package com.s0rryhorizon.appliedchannelmanagement.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;

public record DeviceActionPayload(BlockPos position, String action, String value) implements CustomPacketPayload {
    public static final Type<DeviceActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AppliedChannelManagement.MOD_ID, "device_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeviceActionPayload> STREAM_CODEC = StreamCodec.ofMember(
            DeviceActionPayload::write, DeviceActionPayload::new);

    private DeviceActionPayload(RegistryFriendlyByteBuf buffer) {
        this(buffer.readBlockPos(), buffer.readUtf(32), buffer.readUtf(4096));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(position);
        buffer.writeUtf(action, 32);
        buffer.writeUtf(value, 4096);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
