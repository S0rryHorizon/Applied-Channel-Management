package com.s0rryhorizon.appliedchannelmanagement.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;

public final class AcmCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AppliedChannelManagement.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTER.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.applied_channel_management.main"))
                    .icon(() -> AcmItems.ME_CHANNEL_HUB.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(AcmItems.ME_CHANNEL_HUB.get());
                        output.accept(AcmItems.ME_CHANNEL_DISTRIBUTOR.get());
                    })
                    .build());

    private AcmCreativeTabs() {
    }
}
