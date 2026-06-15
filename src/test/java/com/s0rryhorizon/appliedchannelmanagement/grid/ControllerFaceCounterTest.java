package com.s0rryhorizon.appliedchannelmanagement.grid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class ControllerFaceCounterTest {
    @Test
    void singleControllerHasSixFaces() {
        assertThat(ControllerFaceCounter.countExposedFaces(List.of(BlockPos.ZERO))).isEqualTo(6);
    }

    @Test
    void twoControllerLineRemovesInternalPair() {
        assertThat(ControllerFaceCounter.countExposedFaces(List.of(BlockPos.ZERO, new BlockPos(1, 0, 0))))
                .isEqualTo(10);
    }

    @Test
    void twoByTwoPlaneHasSixteenFaces() {
        assertThat(ControllerFaceCounter.countExposedFaces(List.of(
                new BlockPos(0, 0, 0), new BlockPos(1, 0, 0),
                new BlockPos(0, 1, 0), new BlockPos(1, 1, 0)))).isEqualTo(16);
    }

    @Test
    void cornerCountsOnlyControllerAdjacency() {
        assertThat(ControllerFaceCounter.countExposedFaces(List.of(
                BlockPos.ZERO, new BlockPos(1, 0, 0), new BlockPos(0, 1, 0)))).isEqualTo(14);
    }

    @Test
    void maximumSevenBlockLineHasThirtyFaces() {
        assertThat(ControllerFaceCounter.countExposedFaces(List.of(
                BlockPos.ZERO,
                new BlockPos(1, 0, 0), new BlockPos(2, 0, 0), new BlockPos(3, 0, 0),
                new BlockPos(4, 0, 0), new BlockPos(5, 0, 0), new BlockPos(6, 0, 0)))).isEqualTo(30);
    }
}
