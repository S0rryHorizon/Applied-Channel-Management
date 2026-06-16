package com.s0rryhorizon.appliedchannelmanagement.mixin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.networking.IGrid;
import appeng.me.GridConnection;
import appeng.me.pathfinding.IPathItem;
import appeng.me.pathfinding.PathingCalculation;

import com.s0rryhorizon.appliedchannelmanagement.grid.IChannelPoolService;
import com.s0rryhorizon.appliedchannelmanagement.runtime.WirelessLinkManager;
import com.s0rryhorizon.appliedchannelmanagement.runtime.WirelessLinkMetadata;

@Mixin(value = PathingCalculation.class, remap = false)
public abstract class PathingCalculationMixin {
    @Shadow
    @Final
    private IGrid grid;

    @Shadow
    @Final
    private Queue<IPathItem>[] queues;

    @Shadow
    @Final
    private Set<IPathItem> visited;

    @Shadow
    private void enqueue(IPathItem pathItem, int queueIndex) {
        throw new AssertionError();
    }

    @Shadow
    private void processQueue(Queue<IPathItem> queue, int queueIndex) {
        throw new AssertionError();
    }

    @Unique
    private final List<GridConnection> acm$wirelessRoots = new ArrayList<>();
    @Unique
    private boolean acm$wirelessPhase;
    @Unique
    private int acm$allocated;
    @Unique
    private int acm$wiredAllocated;
    @Unique
    private final Map<UUID, Integer> acm$distributorAllocations = new HashMap<>();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void acm$deferWirelessRoots(IGrid grid, CallbackInfo ci) {
        for (Queue<IPathItem> queue : queues) {
            queue.removeIf(item -> {
                if (item instanceof GridConnection connection
                        && WirelessLinkManager.getMetadata(connection) != null) {
                    acm$wirelessRoots.add(connection);
                    visited.remove(item);
                    return true;
                }
                return false;
            });
        }
    }

    @Inject(method = "compute", at = @At(value = "INVOKE", target = "Lappeng/me/pathfinding/PathingCalculation;propagateAssignments()V"))
    private void acm$allocateWirelessAfterWired(CallbackInfo ci) {
        acm$wiredAllocated = acm$allocated;
        if (acm$wirelessRoots.isEmpty()) {
            return;
        }

        acm$wirelessPhase = true;
        acm$wirelessRoots.sort(Comparator.comparing(WirelessLinkManager::getMetadata,
                WirelessLinkMetadata.ORDERING));
        for (GridConnection root : acm$wirelessRoots) {
            enqueue(root, 0);
        }
        for (int index = 0; index < queues.length; index++) {
            processQueue(queues[index], index);
        }
    }

    @Inject(method = "tryUseChannel", at = @At("HEAD"), cancellable = true)
    private void acm$enforcePoolCapacity(appeng.me.GridNode start, CallbackInfoReturnable<Boolean> cir) {
        if (acm$wirelessPhase) {
            WirelessLinkMetadata metadata = acm$findWirelessLink(start);
            if (acm$allocated >= grid.getService(IChannelPoolService.class).getCapacity()
                    || metadata != null && acm$distributorAllocations.getOrDefault(metadata.distributorId(), 0) >= 32) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "tryUseChannel", at = @At("RETURN"))
    private void acm$countAllocation(appeng.me.GridNode start, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            acm$allocated++;
            if (acm$wirelessPhase) {
                WirelessLinkMetadata metadata = acm$findWirelessLink(start);
                if (metadata != null) {
                    acm$distributorAllocations.merge(metadata.distributorId(), 1, Integer::sum);
                }
            }
        }
    }

    @Unique
    private WirelessLinkMetadata acm$findWirelessLink(IPathItem start) {
        IPathItem current = start;
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<IPathItem, Boolean>());
        while (current != null && seen.add(current)) {
            if (current instanceof GridConnection connection) {
                WirelessLinkMetadata metadata = WirelessLinkManager.getMetadata(connection);
                if (metadata != null) {
                    return metadata;
                }
            }
            current = current.getControllerRoute();
        }
        return null;
    }

    @Inject(method = "compute", at = @At("TAIL"))
    private void acm$publishSnapshot(CallbackInfo ci) {
        grid.getService(IChannelPoolService.class).updateAllocation(acm$wiredAllocated,
                Math.max(0, acm$allocated - acm$wiredAllocated));
    }
}
