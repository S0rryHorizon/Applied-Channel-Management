package com.s0rryhorizon.appliedchannelmanagement.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import com.s0rryhorizon.appliedchannelmanagement.blockentity.ChannelHubBlockEntity;

public final class HubRegistrySavedData extends SavedData {
    private static final String DATA_NAME = "applied_channel_management_hubs";
    private static final SavedData.Factory<HubRegistrySavedData> FACTORY = new SavedData.Factory<>(
            HubRegistrySavedData::new, HubRegistrySavedData::load, null);

    private final Map<UUID, HubRecord> hubs = new HashMap<>();

    public static HubRegistrySavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public boolean update(ChannelHubBlockEntity hub) {
        String normalized = hub.getNormalizedName();
        if (!normalized.isBlank()) {
            Optional<HubRecord> collision = hubs.values().stream()
                    .filter(record -> record.normalizedName().equals(normalized))
                    .filter(record -> !record.id().equals(hub.getHubId()))
                    .findFirst();
            if (collision.isPresent()) {
                return false;
            }
        }
        var record = new HubRecord(hub.getHubId(), hub.getNetworkName(), normalized, hub.getOwnerId(),
                hub.getWhitelist(), hub.getServerLevel().dimension().location(), hub.getBlockPos());
        if (!record.equals(hubs.put(record.id(), record))) {
            setDirty();
        }
        return true;
    }

    public Optional<HubRecord> get(UUID id) {
        return Optional.ofNullable(hubs.get(id));
    }

    public Collection<HubRecord> all() {
        return Set.copyOf(hubs.values());
    }

    public void remove(UUID id) {
        if (hubs.remove(id) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (HubRecord record : hubs.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", record.id());
            entry.putString("name", record.name());
            entry.putString("normalized_name", record.normalizedName());
            entry.putUUID("owner", record.owner());
            entry.putString("dimension", record.dimension().toString());
            entry.putLong("position", record.position().asLong());
            ListTag whitelist = new ListTag();
            record.whitelist().forEach(id -> whitelist.add(net.minecraft.nbt.NbtUtils.createUUID(id)));
            entry.put("whitelist", whitelist);
            list.add(entry);
        }
        tag.put("hubs", list);
        return tag;
    }

    private static HubRegistrySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new HubRegistrySavedData();
        for (Tag raw : tag.getList("hubs", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            if (!entry.hasUUID("id") || !entry.hasUUID("owner")) {
                continue;
            }
            var whitelist = new java.util.HashSet<UUID>();
            for (Tag player : entry.getList("whitelist", Tag.TAG_INT_ARRAY)) {
                whitelist.add(net.minecraft.nbt.NbtUtils.loadUUID(player));
            }
            var record = new HubRecord(entry.getUUID("id"), entry.getString("name"),
                    entry.getString("normalized_name"), entry.getUUID("owner"), Set.copyOf(whitelist),
                    ResourceLocation.parse(entry.getString("dimension")), BlockPos.of(entry.getLong("position")));
            result.hubs.put(record.id(), record);
        }
        return result;
    }

    public record HubRecord(UUID id, String name, String normalizedName, UUID owner, Set<UUID> whitelist,
            ResourceLocation dimension, BlockPos position) {
        public boolean canUse(UUID player) {
            return owner.equals(player) || whitelist.contains(player);
        }

        public Optional<ServerLevel> resolveLevel(MinecraftServer server) {
            return server.levelKeys().stream()
                    .filter(key -> key.location().equals(dimension))
                    .map(server::getLevel)
                    .findFirst();
        }
    }
}
