package com.s0rryhorizon.appliedchannelmanagement.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import appeng.block.AEBaseEntityBlock;

import com.s0rryhorizon.appliedchannelmanagement.blockentity.AbstractChannelDeviceBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelDistributorBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelHubBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmBlockEntities;
import com.s0rryhorizon.appliedchannelmanagement.menu.ChannelDeviceMenu;
import com.s0rryhorizon.appliedchannelmanagement.data.HubRegistrySavedData;

public final class ChannelDeviceBlock extends AEBaseEntityBlock<AbstractChannelDeviceBlockEntity> {
    private final boolean hub;

    public ChannelDeviceBlock(Properties properties, boolean hub) {
        super(properties);
        this.hub = hub;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(state.getBlock()) && level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof ChannelHubBlockEntity hub) {
            HubRegistrySavedData.get(serverLevel.getServer()).remove(hub.getHubId());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return hub ? new ChannelHubBlockEntity(pos, state) : new ChannelDistributorBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player
                && level.getBlockEntity(pos) instanceof AbstractChannelDeviceBlockEntity device) {
            if (device.getOwnerId().equals(new java.util.UUID(0, 0))) {
                device.setOwner(player.getUUID());
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AbstractChannelDeviceBlockEntity device) {
            serverPlayer.openMenu(device, buffer -> ChannelDeviceMenu.writeOpeningData(buffer, device, serverPlayer));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return 0;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (hub && type == AcmBlockEntities.HUB.get()) {
            return (tickerLevel, tickerPos, tickerState, tickerBlockEntity) ->
                    ChannelHubBlockEntity.serverTick(tickerLevel, tickerPos, tickerState,
                            (ChannelHubBlockEntity) tickerBlockEntity);
        }
        if (!hub && type == AcmBlockEntities.DISTRIBUTOR.get()) {
            return (tickerLevel, tickerPos, tickerState, tickerBlockEntity) ->
                    ChannelDistributorBlockEntity.serverTick(tickerLevel, tickerPos, tickerState,
                            (ChannelDistributorBlockEntity) tickerBlockEntity);
        }
        return null;
    }
}
