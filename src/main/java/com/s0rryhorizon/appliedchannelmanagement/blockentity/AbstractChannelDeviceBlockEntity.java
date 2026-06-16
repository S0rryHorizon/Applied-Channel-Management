package com.s0rryhorizon.appliedchannelmanagement.blockentity;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.CustomData;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.util.SettingsFrom;

import com.s0rryhorizon.appliedchannelmanagement.menu.ChannelDeviceMenu;

public abstract class AbstractChannelDeviceBlockEntity extends AEBaseBlockEntity
        implements IInWorldGridNodeHost, MenuProvider {
    private static final IGridNodeListener<AbstractChannelDeviceBlockEntity> NODE_LISTENER =
            new IGridNodeListener<>() {
                @Override
                public void onSaveChanges(AbstractChannelDeviceBlockEntity owner, IGridNode node) {
                    owner.setChanged();
                }

                @Override
                public void onGridChanged(AbstractChannelDeviceBlockEntity owner, IGridNode node) {
                    owner.onGridChanged();
                }

                @Override
                public void onStateChanged(AbstractChannelDeviceBlockEntity owner, IGridNode node, State state) {
                    owner.setChanged();
                    com.s0rryhorizon.appliedchannelmanagement.runtime.WirelessLinkManager.requestReconcile();
                }
            };

    protected final IManagedGridNode mainNode;
    private UUID ownerId = new UUID(0, 0);

    protected AbstractChannelDeviceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            double idlePower) {
        super(type, pos, state);
        this.mainNode = GridHelper.createManagedNode(this, NODE_LISTENER)
                .setInWorldNode(true)
                .setExposedOnSides(EnumSet.allOf(Direction.class))
                .setFlags(GridFlags.DENSE_CAPACITY)
                .setIdlePowerUsage(idlePower)
                .setTagName("acm_node");
    }

    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwner(UUID ownerId) {
        this.ownerId = ownerId;
        setChanged();
    }

    public boolean isOwnedByOrAdmin(net.minecraft.server.level.ServerPlayer player) {
        return ownerId.equals(player.getUUID()) || player.hasPermissions(2);
    }

    @Nullable
    @Override
    public IGridNode getGridNode(Direction direction) {
        return mainNode.getNode();
    }

    @Override
    public AECableType getCableConnectionType(Direction direction) {
        return AECableType.SMART;
    }

    public Set<Direction> getExposedSides() {
        return EnumSet.allOf(Direction.class);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) {
            GridHelper.onFirstTick(this, device -> device.mainNode.create(device.level, device.worldPosition));
        }
    }

    @Override
    public void setRemoved() {
        unregisterRuntime();
        mainNode.destroy();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unregisterRuntime();
        mainNode.destroy();
        super.onChunkUnloaded();
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        mainNode.loadFromNBT(tag);
        loadPortableData(tag, registries);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        mainNode.saveToNBT(tag);
        savePortableData(tag, registries);
    }

    @Override
    public void exportSettings(SettingsFrom source, DataComponentMap.Builder output, Player player) {
        super.exportSettings(source, output, player);
        if (source == SettingsFrom.DISMANTLE_ITEM && getLevel() != null) {
            CompoundTag tag = new CompoundTag();
            savePortableData(tag, getLevel().registryAccess());
            BlockEntity.addEntityType(tag, getType());
            output.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
    }

    private void loadPortableData(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.hasUUID("owner")) {
            ownerId = tag.getUUID("owner");
        }
        loadDeviceData(tag, registries);
    }

    private void savePortableData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putUUID("owner", ownerId);
        saveDeviceData(tag, registries);
    }

    protected void onGridChanged() {
    }

    protected abstract void loadDeviceData(CompoundTag tag, HolderLookup.Provider registries);

    protected abstract void saveDeviceData(CompoundTag tag, HolderLookup.Provider registries);

    protected abstract void unregisterRuntime();

    public abstract String getStatusText();

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ChannelDeviceMenu(containerId, inventory, getBlockPos());
    }
}
