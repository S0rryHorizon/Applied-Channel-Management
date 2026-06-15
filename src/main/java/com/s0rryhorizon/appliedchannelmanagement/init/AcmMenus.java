package com.s0rryhorizon.appliedchannelmanagement.init;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;
import com.s0rryhorizon.appliedchannelmanagement.menu.ChannelDeviceMenu;

public final class AcmMenus {
    public static final DeferredRegister<MenuType<?>> REGISTER = DeferredRegister.create(
            BuiltInRegistries.MENU, AppliedChannelManagement.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ChannelDeviceMenu>> CHANNEL_DEVICE = REGISTER.register(
            "channel_device", () -> IMenuTypeExtension.create(ChannelDeviceMenu::new));

    private AcmMenus() {
    }
}
