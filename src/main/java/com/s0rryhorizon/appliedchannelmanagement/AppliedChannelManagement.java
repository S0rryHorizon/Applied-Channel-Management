package com.s0rryhorizon.appliedchannelmanagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

import appeng.api.networking.GridServices;

import com.s0rryhorizon.appliedchannelmanagement.config.AcmServerConfig;
import com.s0rryhorizon.appliedchannelmanagement.grid.ChannelPoolService;
import com.s0rryhorizon.appliedchannelmanagement.grid.IChannelPoolService;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmBlockEntities;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmBlocks;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmItems;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmMenus;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmCreativeTabs;
import com.s0rryhorizon.appliedchannelmanagement.network.AcmNetwork;
import com.s0rryhorizon.appliedchannelmanagement.runtime.WirelessLinkManager;

@Mod(AppliedChannelManagement.MOD_ID)
public final class AppliedChannelManagement {
    public static final String MOD_ID = "applied_channel_management";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public AppliedChannelManagement(IEventBus modBus, ModContainer container) {
        AcmBlocks.REGISTER.register(modBus);
        AcmItems.REGISTER.register(modBus);
        AcmCreativeTabs.REGISTER.register(modBus);
        AcmBlockEntities.REGISTER.register(modBus);
        AcmMenus.REGISTER.register(modBus);
        AcmBlockEntities.registerCapabilities(modBus);
        modBus.addListener(AcmNetwork::register);
        modBus.addListener(AcmServerConfig::onLoad);
        modBus.addListener(AcmServerConfig::onReload);
        GridServices.register(IChannelPoolService.class, ChannelPoolService.class);
        NeoForge.EVENT_BUS.addListener(WirelessLinkManager::onServerTick);
        container.registerConfig(ModConfig.Type.SERVER, AcmServerConfig.SPEC);
    }
}
