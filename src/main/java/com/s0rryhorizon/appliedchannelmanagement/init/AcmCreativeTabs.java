package com.s0rryhorizon.appliedchannelmanagement.init;

import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import appeng.api.ids.AECreativeTabIds;

public final class AcmCreativeTabs {
    private AcmCreativeTabs() {
    }

    public static void addItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == AECreativeTabIds.MAIN) {
            event.accept(AcmItems.ME_CHANNEL_HUB.get());
            event.accept(AcmItems.ME_CHANNEL_DISTRIBUTOR.get());
        }
    }
}
