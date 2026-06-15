package com.s0rryhorizon.appliedchannelmanagement.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.pathing.ControllerState;
import appeng.blockentity.networking.ControllerBlockEntity;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelDistributorBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelHubBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.config.AcmServerConfig;
import com.s0rryhorizon.appliedchannelmanagement.data.HubRegistrySavedData;

public final class WirelessLinkManager {
    private static final Map<UUID, ChannelHubBlockEntity> HUBS = new HashMap<>();
    private static final Map<UUID, ChannelDistributorBlockEntity> DISTRIBUTORS = new HashMap<>();
    private static final Map<UUID, ActiveLink> ACTIVE_LINKS = new HashMap<>();
    private static final Map<IGridConnection, WirelessLinkMetadata> CONNECTION_METADATA = new IdentityHashMap<>();
    private static boolean reconcileRequested = true;
    private static long lastReconcileTick;

    private WirelessLinkManager() {
    }

    public static void register(ChannelHubBlockEntity hub) {
        HUBS.put(hub.getHubId(), hub);
    }

    public static void register(ChannelDistributorBlockEntity distributor) {
        DISTRIBUTORS.put(distributor.getDistributorId(), distributor);
    }

    public static void unregister(ChannelHubBlockEntity hub) {
        HUBS.remove(hub.getHubId(), hub);
        requestReconcile();
    }

    public static void unregister(ChannelDistributorBlockEntity distributor) {
        DISTRIBUTORS.remove(distributor.getDistributorId(), distributor);
        disconnect(distributor.getDistributorId());
    }

    public static void requestReconcile() {
        reconcileRequested = true;
    }

    public static boolean isLinked(UUID distributorId) {
        return ACTIVE_LINKS.containsKey(distributorId);
    }

    public static Optional<ChannelHubBlockEntity> getLoadedHub(UUID hubId) {
        return Optional.ofNullable(HUBS.get(hubId));
    }

    public static List<UUID> getConnectedDistributors(UUID hubId) {
        return ACTIVE_LINKS.values().stream()
                .filter(link -> link.metadata().hubId().equals(hubId))
                .map(link -> link.metadata().distributorId())
                .sorted()
                .toList();
    }

    public static String describe(ChannelHubBlockEntity hub, ChannelDistributorBlockEntity distributor) {
        String hubState = hub.getMainNode().isReady()
                ? "ready,powered=" + hub.getMainNode().isPowered() + ",controller="
                        + hub.getMainNode().getGrid().getPathingService().getControllerState()
                : "not-ready";
        String distributorState = distributor.getMainNode().isReady()
                ? "ready,powered=" + distributor.getMainNode().isPowered() + ",controller="
                        + distributor.getMainNode().getGrid().getPathingService().getControllerState()
                : "not-ready";
        return "hub{" + hubState + ",registered=" + HUBS.containsKey(hub.getHubId()) + "}, distributor{"
                + distributorState + ",registered=" + DISTRIBUTORS.containsKey(distributor.getDistributorId())
                + ",target=" + distributor.getTargetHubId().orElse(null) + "}";
    }

    public static WirelessLinkMetadata getMetadata(IGridConnection connection) {
        return CONNECTION_METADATA.get(connection);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long tick = server.getTickCount();
        if (!reconcileRequested && tick - lastReconcileTick < 10) {
            return;
        }
        reconcileRequested = false;
        lastReconcileTick = tick;
        reconcile(server);
    }

    private static void reconcile(MinecraftServer server) {
        HUBS.values().removeIf(hub -> hub.isRemoved() || hub.getLevel() == null);
        DISTRIBUTORS.values().removeIf(distributor -> distributor.isRemoved() || distributor.getLevel() == null);

        HubRegistrySavedData data = HubRegistrySavedData.get(server);
        var validHubs = new HashMap<UUID, ChannelHubBlockEntity>();
        for (ChannelHubBlockEntity hub : HUBS.values()) {
            if (hub.getMainNode().isReady() && hub.getMainNode().isPowered() && data.update(hub)
                    && !hub.getNetworkName().isBlank() && isSelectedHubForGrid(hub)) {
                validHubs.put(hub.getHubId(), hub);
            }
        }

        for (var entry : new ArrayList<>(ACTIVE_LINKS.entrySet())) {
            ChannelDistributorBlockEntity distributor = DISTRIBUTORS.get(entry.getKey());
            ChannelHubBlockEntity hub = validHubs.get(entry.getValue().metadata().hubId());
            if (distributor == null || hub == null || !isStillValid(distributor, hub, entry.getValue())) {
                disconnect(entry.getKey());
            } else {
                distributor.setLinkPowerUsage(isCrossDimension(distributor, hub));
            }
        }

        var candidates = DISTRIBUTORS.values().stream()
                .filter(distributor -> !ACTIVE_LINKS.containsKey(distributor.getDistributorId()))
                .sorted(Comparator.comparingInt(ChannelDistributorBlockEntity::getPriority).reversed()
                        .thenComparing(ChannelDistributorBlockEntity::getDistributorId))
                .toList();
        var originalRemoteGrids = new HashMap<UUID, IGrid>();
        for (ChannelDistributorBlockEntity distributor : candidates) {
            if (distributor.getMainNode().isReady()) {
                originalRemoteGrids.put(distributor.getDistributorId(), distributor.getMainNode().getGrid());
            }
        }
        var claimedRemoteGrids = new IdentityHashMap<IGrid, Boolean>();
        for (ChannelDistributorBlockEntity distributor : candidates) {
            distributor.getTargetHubId().map(validHubs::get).ifPresent(hub -> {
                IGrid remoteGrid = originalRemoteGrids.get(distributor.getDistributorId());
                if (remoteGrid != null && canConnect(distributor, hub) && !claimedRemoteGrids.containsKey(remoteGrid)) {
                    claimedRemoteGrids.put(remoteGrid, Boolean.TRUE);
                    connect(distributor, hub);
                }
            });
        }
    }

    private static boolean isSelectedHubForGrid(ChannelHubBlockEntity hub) {
        IGrid grid = hub.getMainNode().getGrid();
        if (grid.getPathingService().getControllerState() != ControllerState.CONTROLLER_ONLINE) {
            return false;
        }
        return HUBS.values().stream()
                .filter(other -> other.getMainNode().isReady())
                .filter(other -> other.getMainNode().getGrid() == grid)
                .map(ChannelHubBlockEntity::getHubId)
                .min(UUID::compareTo)
                .map(hub.getHubId()::equals)
                .orElse(false);
    }

    private static boolean canConnect(ChannelDistributorBlockEntity distributor, ChannelHubBlockEntity hub) {
        if (!distributor.getMainNode().isReady() || !distributor.getMainNode().isPowered()
                || !hub.canUse(distributor.getOwnerId())) {
            return false;
        }
        if (distributor.getMainNode().getGrid().getPathingService().getControllerState() != ControllerState.NO_CONTROLLER) {
            return false;
        }
        if (!distributor.getMainNode().getGrid().getMachines(ChannelHubBlockEntity.class).isEmpty()) {
            return false;
        }
        boolean crossDimension = isCrossDimension(distributor, hub);
        return !crossDimension || AcmServerConfig.CROSS_DIMENSION_ENABLED.get();
    }

    private static void connect(ChannelDistributorBlockEntity distributor, ChannelHubBlockEntity hub) {
        IGridNode controller = findControllerNode(hub.getMainNode().getGrid());
        IGridNode distributorNode = distributor.getMainNode().getNode();
        if (controller == null || distributorNode == null) {
            return;
        }
        try {
            IGridConnection connection = GridHelper.createConnection(controller, distributorNode);
            var metadata = new WirelessLinkMetadata(distributor.getDistributorId(), hub.getHubId(),
                    distributor.getPriority());
            CONNECTION_METADATA.put(connection, metadata);
            ACTIVE_LINKS.put(distributor.getDistributorId(), new ActiveLink(connection, metadata));
            distributor.setLinkPowerUsage(isCrossDimension(distributor, hub));
        } catch (IllegalStateException exception) {
            AppliedChannelManagement.LOGGER.debug("Unable to establish wireless channel link", exception);
        }
    }

    private static IGridNode findControllerNode(IGrid grid) {
        return java.util.stream.StreamSupport.stream(grid.getMachineNodes(ControllerBlockEntity.class).spliterator(), false)
                .filter(node -> node.getOwner() instanceof ControllerBlockEntity)
                .min(Comparator.comparingLong(node -> ((ControllerBlockEntity) node.getOwner()).getBlockPos().asLong()))
                .orElse(null);
    }

    private static boolean isStillValid(ChannelDistributorBlockEntity distributor, ChannelHubBlockEntity hub,
            ActiveLink link) {
        return distributor.getTargetHubId().filter(link.metadata().hubId()::equals).isPresent()
                && distributor.getPriority() == link.metadata().priority()
                && distributor.getMainNode().isReady() && distributor.getMainNode().isPowered()
                && hub.getMainNode().isReady() && hub.getMainNode().isPowered()
                && hub.canUse(distributor.getOwnerId())
                && (!isCrossDimension(distributor, hub) || AcmServerConfig.CROSS_DIMENSION_ENABLED.get());
    }

    private static boolean isCrossDimension(ChannelDistributorBlockEntity distributor, ChannelHubBlockEntity hub) {
        return distributor.getLevel() != hub.getLevel();
    }

    private static void disconnect(UUID distributorId) {
        ActiveLink link = ACTIVE_LINKS.remove(distributorId);
        if (link != null) {
            CONNECTION_METADATA.remove(link.connection());
            link.connection().destroy();
        }
        ChannelDistributorBlockEntity distributor = DISTRIBUTORS.get(distributorId);
        if (distributor != null) {
            distributor.setLinkPowerUsage(false);
        }
    }

    private record ActiveLink(IGridConnection connection, WirelessLinkMetadata metadata) {
    }
}
