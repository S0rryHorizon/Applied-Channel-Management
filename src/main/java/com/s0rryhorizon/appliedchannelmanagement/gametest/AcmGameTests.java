package com.s0rryhorizon.appliedchannelmanagement.gametest;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;
import com.s0rryhorizon.appliedchannelmanagement.grid.ControllerFaceCounter;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelDistributorBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelHubBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmBlocks;
import com.s0rryhorizon.appliedchannelmanagement.runtime.WirelessLinkManager;

import appeng.core.definitions.AEBlocks;

@GameTestHolder(AppliedChannelManagement.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AcmGameTests {
    private AcmGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void controllerFaceGeometryRunsOnDedicatedServer(GameTestHelper helper) {
        int faces = ControllerFaceCounter.countExposedFaces(List.of(
                BlockPos.ZERO, new BlockPos(1, 0, 0), new BlockPos(0, 1, 0)));
        if (faces != 14) {
            throw new AssertionError("Expected 14 exposed faces, got " + faces);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void distributorLinksPoweredRemoteSubnet(GameTestHelper helper) {
        BlockPos controllerPos = new BlockPos(1, 1, 1);
        BlockPos hubPos = new BlockPos(2, 1, 1);
        BlockPos sourcePowerPos = new BlockPos(1, 1, 2);
        BlockPos distributorPos = new BlockPos(4, 1, 1);
        BlockPos remoteDevicePos = new BlockPos(5, 1, 1);

        helper.setBlock(controllerPos, AEBlocks.CONTROLLER.block());
        helper.setBlock(sourcePowerPos, AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(hubPos, AcmBlocks.ME_CHANNEL_HUB.get());
        helper.setBlock(distributorPos, AcmBlocks.ME_CHANNEL_DISTRIBUTOR.get());
        helper.setBlock(remoteDevicePos, AEBlocks.INTERFACE.block());

        var hub = (ChannelHubBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(hubPos));
        var distributor = (ChannelDistributorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(distributorPos));
        helper.assertTrue(hub != null, "Hub block entity was not created");
        helper.assertTrue(distributor != null, "Distributor block entity was not created");
        UUID owner = UUID.randomUUID();
        hub.setOwner(owner);
        hub.setNetworkName("gt-" + hub.getHubId().toString().substring(0, 8));
        distributor.setOwner(owner);
        distributor.bindTo(hub.getHubId());

        helper.succeedWhen(() -> {
            helper.assertTrue(WirelessLinkManager.isLinked(distributor.getDistributorId()),
                    "Distributor did not establish a virtual controller link: "
                            + WirelessLinkManager.describe(hub, distributor));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void plainAe2NetworkRepathsWithoutAcmDevices(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), AEBlocks.CONTROLLER.block());
        helper.setBlock(new BlockPos(1, 1, 2), AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(new BlockPos(2, 1, 1), AEBlocks.DRIVE.block());
        helper.setBlock(new BlockPos(3, 1, 1), AEBlocks.INTERFACE.block());

        helper.runAfterDelay(60, helper::succeed);
    }
}
