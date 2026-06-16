package com.s0rryhorizon.appliedchannelmanagement.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.s0rryhorizon.appliedchannelmanagement.AppliedChannelManagement;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmMenus;

@EventBusSubscriber(modid = AppliedChannelManagement.MOD_ID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public final class AcmClient {
    private AcmClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AcmMenus.CHANNEL_DEVICE.get(), ChannelDeviceScreen::new);
    }

    public static void registerConfigScreen(ModContainer container) {
        IConfigScreenFactory factory = (modContainer, parent) -> new ConfigurationScreen(modContainer, parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }
}
