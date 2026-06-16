package com.s0rryhorizon.appliedchannelmanagement.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelDistributorBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelHubBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.runtime.WirelessLinkManager;

public final class AcmServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue HUB_POWER;
    public static final ModConfigSpec.DoubleValue DISTRIBUTOR_POWER;
    public static final ModConfigSpec.DoubleValue DISTRIBUTOR_DISTANCE_BASE_POWER;
    public static final ModConfigSpec.DoubleValue DISTRIBUTOR_POWER_PER_BLOCK;
    public static final ModConfigSpec.DoubleValue CROSS_DIMENSION_POWER;
    public static final ModConfigSpec.EnumValue<DistributorPowerMode> DISTRIBUTOR_POWER_MODE;
    public static final ModConfigSpec.BooleanValue BLOCKS_OBSTRUCT_CONTROLLER_FACES;
    public static final ModConfigSpec.BooleanValue AE_BLOCKS_OBSTRUCT_CONTROLLER_FACES;
    public static final ModConfigSpec.IntValue MAX_DISTRIBUTOR_CHANNELS;
    public static final ModConfigSpec.IntValue MAX_NAME_LENGTH;
    public static final ModConfigSpec.BooleanValue CROSS_DIMENSION_ENABLED;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("channel_management");
        HUB_POWER = builder
                .comment("Base hub energy draw in AE/t.")
                .defineInRange("hubPowerPerTick", 32.0, 0.0, 1_000_000.0);
        DISTRIBUTOR_POWER_MODE = builder
                .comment("How same-dimension distributor links draw power. FIXED uses distributorPowerPerTick. DISTANCE uses distributorDistanceBasePowerPerTick + distance * distributorPowerPerBlock.")
                .defineEnum("distributorPowerMode", DistributorPowerMode.FIXED);
        DISTRIBUTOR_POWER = builder
                .comment("Fixed same-dimension distributor link energy draw in AE/t.")
                .defineInRange("distributorPowerPerTick", 16.0, 0.0, 1_000_000.0);
        DISTRIBUTOR_DISTANCE_BASE_POWER = builder
                .comment("Base same-dimension distributor link energy draw in AE/t when distributorPowerMode is DISTANCE.")
                .defineInRange("distributorDistanceBasePowerPerTick", 16.0, 0.0, 1_000_000.0);
        DISTRIBUTOR_POWER_PER_BLOCK = builder
                .comment("Additional AE/t per block of same-dimension link distance when distributorPowerMode is DISTANCE.")
                .defineInRange("distributorPowerPerBlock", 0.05, 0.0, 1_000_000.0);
        CROSS_DIMENSION_POWER = builder
                .comment("Fixed distributor link energy draw in AE/t for cross-dimension links.")
                .defineInRange("crossDimensionPowerPerTick", 64.0, 0.0, 1_000_000.0);
        BLOCKS_OBSTRUCT_CONTROLLER_FACES = builder
                .comment("When true, ordinary blocks adjacent to an ME Controller face prevent the hub from counting that face as available channel capacity.")
                .define("blocksObstructControllerFaces", false);
        AE_BLOCKS_OBSTRUCT_CONTROLLER_FACES = builder
                .comment("When true, AE2 machines and cable blocks adjacent to an ME Controller face prevent the hub from counting that face as available channel capacity.")
                .define("aeBlocksObstructControllerFaces", false);
        MAX_DISTRIBUTOR_CHANNELS = builder
                .comment("Maximum channels a single ME Channel Distributor can allocate across all attached sides.")
                .defineInRange("maxDistributorChannels", 32, 1, 1024);
        MAX_NAME_LENGTH = builder
                .comment("Maximum saved name length for hubs and distributors.")
                .defineInRange("maxNameLength", 32, 1, 128);
        CROSS_DIMENSION_ENABLED = builder
                .comment("Whether distributors may link to hubs in another dimension.")
                .define("crossDimensionEnabled", true);
        builder.pop();
        SPEC = builder.build();
    }

    private AcmServerConfig() {
    }

    public static double getDistributorPower(ChannelDistributorBlockEntity distributor, ChannelHubBlockEntity hub) {
        if (distributor.getLevel() != hub.getLevel()) {
            return CROSS_DIMENSION_POWER.get();
        }
        if (DISTRIBUTOR_POWER_MODE.get() == DistributorPowerMode.FIXED) {
            return DISTRIBUTOR_POWER.get();
        }
        var from = distributor.getBlockPos();
        var to = hub.getBlockPos();
        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        double dz = from.getZ() - to.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return DISTRIBUTOR_DISTANCE_BASE_POWER.get() + distance * DISTRIBUTOR_POWER_PER_BLOCK.get();
    }

    public static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(AppliedChannelManagement.MOD_ID)) {
            WirelessLinkManager.requestReconcile();
        }
    }

    public static void onReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getModId().equals(AppliedChannelManagement.MOD_ID)) {
            WirelessLinkManager.requestReconcile();
        }
    }

    public enum DistributorPowerMode {
        FIXED,
        DISTANCE
    }
}
