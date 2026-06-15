package com.s0rryhorizon.appliedchannelmanagement.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AcmServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue HUB_POWER;
    public static final ModConfigSpec.DoubleValue DISTRIBUTOR_POWER;
    public static final ModConfigSpec.DoubleValue CROSS_DIMENSION_POWER;
    public static final ModConfigSpec.IntValue MAX_NAME_LENGTH;
    public static final ModConfigSpec.BooleanValue CROSS_DIMENSION_ENABLED;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("channel_management");
        HUB_POWER = builder.defineInRange("hubPowerPerTick", 32.0, 0.0, 1_000_000.0);
        DISTRIBUTOR_POWER = builder.defineInRange("distributorPowerPerTick", 16.0, 0.0, 1_000_000.0);
        CROSS_DIMENSION_POWER = builder.defineInRange("crossDimensionPowerPerTick", 64.0, 0.0, 1_000_000.0);
        MAX_NAME_LENGTH = builder.defineInRange("maxHubNameLength", 32, 1, 128);
        CROSS_DIMENSION_ENABLED = builder.define("crossDimensionEnabled", true);
        builder.pop();
        SPEC = builder.build();
    }

    private AcmServerConfig() {
    }
}
