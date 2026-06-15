package com.s0rryhorizon.appliedchannelmanagement.grid;

import java.util.ArrayList;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.pathing.ChannelMode;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.service.PathingService;

import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelHubBlockEntity;

public final class ChannelPoolService implements IChannelPoolService, IGridServiceProvider {
    private final IGrid grid;
    private ChannelPoolSnapshot snapshot = ChannelPoolSnapshot.EMPTY;

    public ChannelPoolService(IGrid grid) {
        this.grid = grid;
    }

    @Override
    public int getCapacity() {
        ChannelMode mode = ((PathingService) grid.getPathingService()).getChannelMode();
        if (mode == ChannelMode.INFINITE) {
            return Integer.MAX_VALUE;
        }
        var positions = new ArrayList<net.minecraft.core.BlockPos>();
        for (var node : grid.getMachineNodes(ControllerBlockEntity.class)) {
            if (node.getOwner() instanceof ControllerBlockEntity controller) {
                positions.add(controller.getBlockPos());
            }
        }
        return ControllerFaceCounter.countExposedFaces(positions) * 32;
    }

    @Override
    public void updateAllocation(int wired, int wireless) {
        int capacity = getCapacity();
        snapshot = new ChannelPoolSnapshot(capacity, wired, wireless, capacity == Integer.MAX_VALUE);
        for (ChannelHubBlockEntity hub : grid.getMachines(ChannelHubBlockEntity.class)) {
            hub.setSnapshot(snapshot);
        }
    }

    @Override
    public ChannelPoolSnapshot getSnapshot() {
        return snapshot;
    }
}
