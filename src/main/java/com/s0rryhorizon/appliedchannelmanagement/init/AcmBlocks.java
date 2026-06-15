package com.s0rryhorizon.appliedchannelmanagement.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;
import com.s0rryhorizon.appliedchannelmanagement.block.ChannelDeviceBlock;

public final class AcmBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(AppliedChannelManagement.MOD_ID);

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().strength(4.0f, 12.0f).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<Block> ME_CHANNEL_HUB = REGISTER.register("me_channel_hub",
            () -> new ChannelDeviceBlock(properties(), true));
    public static final DeferredBlock<Block> ME_CHANNEL_DISTRIBUTOR = REGISTER.register("me_channel_distributor",
            () -> new ChannelDeviceBlock(properties(), false));

    private AcmBlocks() {
    }
}
