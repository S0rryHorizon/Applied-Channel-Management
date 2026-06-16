package com.s0rryhorizon.appliedchannelmanagement.network;

import java.util.HashSet;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.s0rryhorizon.appliedchannelmanagement.blockentity.AbstractChannelDeviceBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelDistributorBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelHubBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.data.HubRegistrySavedData;
import com.s0rryhorizon.appliedchannelmanagement.runtime.WirelessLinkManager;

public final class AcmNetwork {
    private AcmNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(DeviceActionPayload.TYPE, DeviceActionPayload.STREAM_CODEC,
                AcmNetwork::handleDeviceAction);
    }

    private static void handleDeviceAction(DeviceActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || player.distanceToSqr(payload.position().getX() + 0.5, payload.position().getY() + 0.5,
                        payload.position().getZ() + 0.5) > 64
                || !(player.level().getBlockEntity(payload.position()) instanceof AbstractChannelDeviceBlockEntity device)) {
            return;
        }

        switch (payload.action()) {
            case "hub_name" -> updateHubName(player, device, payload.value());
            case "hub_acl" -> updateHubAcl(player, device, payload.value());
            case "distributor_name" -> updateDistributorName(player, device, payload.value());
            case "distributor_target" -> updateDistributorTarget(player, device, payload.value());
            case "distributor_priority" -> updateDistributorPriority(player, device, payload.value());
            default -> {
            }
        }
    }

    private static void updateHubName(ServerPlayer player, AbstractChannelDeviceBlockEntity device, String value) {
        if (!(device instanceof ChannelHubBlockEntity hub) || !hub.isOwnedByOrAdmin(player)) {
            deny(player);
            return;
        }
        String previous = hub.getNetworkName();
        if (!hub.setNetworkName(value)) {
            player.sendSystemMessage(Component.translatable("message.applied_channel_management.hub_name_invalid"));
            return;
        }
        HubRegistrySavedData data = HubRegistrySavedData.get(player.getServer());
        if (!data.update(hub)) {
            hub.setNetworkName(previous);
            data.update(hub);
            player.sendSystemMessage(Component.translatable("message.applied_channel_management.hub_name_duplicate"));
            return;
        }
        WirelessLinkManager.requestReconcile();
        player.sendSystemMessage(Component.translatable("message.applied_channel_management.hub_name_updated"));
    }

    private static void updateHubAcl(ServerPlayer player, AbstractChannelDeviceBlockEntity device, String value) {
        if (!(device instanceof ChannelHubBlockEntity hub) || !hub.isOwnedByOrAdmin(player)) {
            deny(player);
            return;
        }
        var ids = new HashSet<UUID>();
        if (!value.isBlank()) {
            for (String raw : value.split(",")) {
                String token = raw.trim();
                if (token.isEmpty()) {
                    continue;
                }
                ServerPlayer online = player.getServer().getPlayerList().getPlayerByName(token);
                if (online != null) {
                    ids.add(online.getUUID());
                    continue;
                }
                try {
                    ids.add(UUID.fromString(token));
                } catch (IllegalArgumentException exception) {
                    player.sendSystemMessage(Component.translatable(
                            "message.applied_channel_management.unknown_player_or_uuid", token));
                    return;
                }
            }
        }
        hub.setWhitelist(ids);
        HubRegistrySavedData.get(player.getServer()).update(hub);
        WirelessLinkManager.requestReconcile();
        player.sendSystemMessage(Component.translatable("message.applied_channel_management.hub_whitelist_updated"));
    }

    private static void updateDistributorName(ServerPlayer player, AbstractChannelDeviceBlockEntity device,
            String value) {
        if (!(device instanceof ChannelDistributorBlockEntity distributor) || !distributor.isOwnedByOrAdmin(player)) {
            deny(player);
            return;
        }
        if (!distributor.setDeviceName(value)) {
            player.sendSystemMessage(Component.translatable("message.applied_channel_management.distributor_name_invalid"));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.applied_channel_management.distributor_name_updated"));
    }

    private static void updateDistributorTarget(ServerPlayer player, AbstractChannelDeviceBlockEntity device,
            String value) {
        if (!(device instanceof ChannelDistributorBlockEntity distributor) || !distributor.isOwnedByOrAdmin(player)) {
            deny(player);
            return;
        }
        if (value.isBlank()) {
            distributor.bindTo(null);
            player.sendSystemMessage(Component.translatable("message.applied_channel_management.distributor_unbound"));
            return;
        }
        String normalized = ChannelHubBlockEntity.normalizeName(value);
        var target = HubRegistrySavedData.get(player.getServer()).all().stream()
                .filter(record -> record.normalizedName().equals(normalized))
                .findFirst();
        if (target.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.applied_channel_management.no_hub"));
            return;
        }
        if (!player.hasPermissions(2) && !target.get().canUse(player.getUUID())) {
            deny(player);
            return;
        }
        distributor.bindTo(target.get().id());
        player.sendSystemMessage(Component.translatable("message.applied_channel_management.distributor_target_updated"));
    }

    private static void updateDistributorPriority(ServerPlayer player, AbstractChannelDeviceBlockEntity device,
            String value) {
        if (!(device instanceof ChannelDistributorBlockEntity distributor) || !distributor.isOwnedByOrAdmin(player)) {
            deny(player);
            return;
        }
        try {
            distributor.setPriority(Integer.parseInt(value.trim()));
            player.sendSystemMessage(Component.translatable(
                    "message.applied_channel_management.distributor_priority_updated"));
        } catch (NumberFormatException exception) {
            player.sendSystemMessage(Component.translatable("message.applied_channel_management.priority_invalid"));
        }
    }

    private static void deny(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.applied_channel_management.permission_denied"));
    }
}
