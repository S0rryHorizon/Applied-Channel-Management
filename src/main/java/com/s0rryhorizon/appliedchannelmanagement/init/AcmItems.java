package com.s0rryhorizon.appliedchannelmanagement.init;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import appeng.block.AEBaseBlockItem;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;

public final class AcmItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(AppliedChannelManagement.MOD_ID);

    public static final DeferredItem<AEBaseBlockItem> ME_CHANNEL_HUB = REGISTER.register("me_channel_hub",
            () -> new AEBaseBlockItem(AcmBlocks.ME_CHANNEL_HUB.get(), new Item.Properties()));
    public static final DeferredItem<AEBaseBlockItem> ME_CHANNEL_DISTRIBUTOR = REGISTER.register("me_channel_distributor",
            () -> new AEBaseBlockItem(AcmBlocks.ME_CHANNEL_DISTRIBUTOR.get(), new Item.Properties()));

    private AcmItems() {
    }
}
