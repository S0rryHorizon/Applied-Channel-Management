package com.s0rryhorizon.appliedchannelmanagement.init;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.api.AECapabilities;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelDistributorBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelHubBlockEntity;

public final class AcmBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(
            BuiltInRegistries.BLOCK_ENTITY_TYPE, AppliedChannelManagement.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChannelHubBlockEntity>> HUB = REGISTER.register(
            "me_channel_hub", () -> BlockEntityType.Builder.of(ChannelHubBlockEntity::new,
                    AcmBlocks.ME_CHANNEL_HUB.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChannelDistributorBlockEntity>> DISTRIBUTOR = REGISTER
            .register("me_channel_distributor", () -> BlockEntityType.Builder.of(ChannelDistributorBlockEntity::new,
                    AcmBlocks.ME_CHANNEL_DISTRIBUTOR.get()).build(null));

    public static void registerCapabilities(IEventBus modBus) {
        modBus.addListener(AcmBlockEntities::onRegisterCapabilities);
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, HUB.get(), (be, side) -> be);
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, DISTRIBUTOR.get(), (be, side) -> be);
    }

    private AcmBlockEntities() {
    }
}
