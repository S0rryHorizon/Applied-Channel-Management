package com.s0rryhorizon.appliedchannelmanagement.grid;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

import com.s0rryhorizon.appliedchannelmanagement.config.AcmServerConfig;

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

    public static int countAvailableFaces(Iterable<BlockPos> controllerPositions, LevelReader level) {
        Set<BlockPos> positions = new HashSet<>();
        controllerPositions.forEach(pos -> positions.add(pos.immutable()));
        int faces = 0;
        for (BlockPos position : positions) {
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = position.relative(direction);
                if (!positions.contains(adjacent) && !isObstructed(level.getBlockState(adjacent))) {
                    faces++;
                }
            }
        }
        return faces;
    }

    private static boolean isObstructed(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (isAeBlock(state)) {
            return AcmServerConfig.AE_BLOCKS_OBSTRUCT_CONTROLLER_FACES.get();
        }
        return AcmServerConfig.BLOCKS_OBSTRUCT_CONTROLLER_FACES.get();
    }

    private static boolean isAeBlock(BlockState state) {
        return "ae2".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace());
    }
}
