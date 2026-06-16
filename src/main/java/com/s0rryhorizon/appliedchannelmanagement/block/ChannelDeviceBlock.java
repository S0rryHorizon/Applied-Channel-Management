package com.s0rryhorizon.appliedchannelmanagement.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.stats.Stats;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.serialization.MapCodec;

import com.s0rryhorizon.appliedchannelmanagement.blockentity.AbstractChannelDeviceBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelDistributorBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelHubBlockEntity;
import com.s0rryhorizon.appliedchannelmanagement.init.AcmBlockEntities;
import com.s0rryhorizon.appliedchannelmanagement.menu.ChannelDeviceMenu;
import com.s0rryhorizon.appliedchannelmanagement.data.HubRegistrySavedData;

public final class ChannelDeviceBlock extends BaseEntityBlock {
    private static final TagKey<Item> WRENCH = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

    private final boolean hub;

    public ChannelDeviceBlock(Properties properties, boolean hub) {
        super(properties);
        this.hub = hub;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new ChannelDeviceBlock(properties, hub));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(WRENCH)) {
            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                ItemStack drop = createSavedStack(level, state, blockEntity);
                level.levelEvent(null, 2001, pos, Block.getId(state));
                level.removeBlock(pos, false);
                player.getInventory().placeItemBackInInventory(drop);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide && hasSilkTouch(level, tool)
                && blockEntity instanceof AbstractChannelDeviceBlockEntity) {
            player.awardStat(Stats.BLOCK_MINED.get(this));
            player.causeFoodExhaustion(0.005F);
            popSavedStack(level, pos, state, blockEntity);
            return;
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    private static boolean hasSilkTouch(Level level, ItemStack tool) {
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.SILK_TOUCH), tool) > 0;
    }

    private static void popSavedStack(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        popResource(level, pos, createSavedStack(level, state, blockEntity));
    }

    private static ItemStack createSavedStack(Level level, BlockState state, @Nullable BlockEntity blockEntity) {
        ItemStack drop = new ItemStack(state.getBlock());
        if (blockEntity != null) {
            blockEntity.saveToItem(drop, level.registryAccess());
        }
        return drop;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (hub && type == AcmBlockEntities.HUB.get()) {
            return createTickerHelper(type, AcmBlockEntities.HUB.get(), ChannelHubBlockEntity::serverTick);
        }
        if (!hub && type == AcmBlockEntities.DISTRIBUTOR.get()) {
            return createTickerHelper(type, AcmBlockEntities.DISTRIBUTOR.get(),
                    ChannelDistributorBlockEntity::serverTick);
        }
        return null;
    }
}
