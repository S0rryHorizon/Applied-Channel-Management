package com.s0rryhorizon.appliedchannelmanagement.grid;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class ControllerFaceCounter {
    private ControllerFaceCounter() {
    }

    public static int countExposedFaces(Iterable<BlockPos> controllerPositions) {
        Set<BlockPos> positions = new HashSet<>();
        controllerPositions.forEach(pos -> positions.add(pos.immutable()));
        int faces = 0;
        for (BlockPos position : positions) {
            for (Direction direction : Direction.values()) {
                if (!positions.contains(position.relative(direction))) {
                    faces++;
                }
            }
        }
        return faces;
    }
}
