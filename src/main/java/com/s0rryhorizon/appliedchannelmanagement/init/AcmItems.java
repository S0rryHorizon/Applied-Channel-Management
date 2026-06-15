package com.s0rryhorizon.appliedchannelmanagement.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;

public final class AcmItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(AppliedChannelManagement.MOD_ID);

    public static final DeferredItem<BlockItem> ME_CHANNEL_HUB = REGISTER.register("me_channel_hub",
            () -> new BlockItem(AcmBlocks.ME_CHANNEL_HUB.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ME_CHANNEL_DISTRIBUTOR = REGISTER.register("me_channel_distributor",
            () -> new BlockItem(AcmBlocks.ME_CHANNEL_DISTRIBUTOR.get(), new Item.Properties()));

    private AcmItems() {
    }
}
