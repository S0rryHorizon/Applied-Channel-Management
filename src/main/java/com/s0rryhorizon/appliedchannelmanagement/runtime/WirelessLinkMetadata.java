package com.s0rryhorizon.appliedchannelmanagement.runtime;

import java.util.UUID;
import java.util.Comparator;

public record WirelessLinkMetadata(UUID distributorId, UUID hubId, int priority) {
    public static final Comparator<WirelessLinkMetadata> ORDERING = Comparator
            .comparingInt(WirelessLinkMetadata::priority).reversed()
            .thenComparing(WirelessLinkMetadata::distributorId);
}
