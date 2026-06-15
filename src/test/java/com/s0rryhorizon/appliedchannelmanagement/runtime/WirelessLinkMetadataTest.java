package com.s0rryhorizon.appliedchannelmanagement.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WirelessLinkMetadataTest {
    @Test
    void ordersHigherPriorityFirstThenStableUuid() {
        UUID hub = UUID.randomUUID();
        UUID first = new UUID(0, 1);
        UUID second = new UUID(0, 2);
        var links = new ArrayList<>(List.of(
                new WirelessLinkMetadata(second, hub, 10),
                new WirelessLinkMetadata(UUID.randomUUID(), hub, -5),
                new WirelessLinkMetadata(first, hub, 10)));

        links.sort(WirelessLinkMetadata.ORDERING);

        assertThat(links).extracting(WirelessLinkMetadata::distributorId)
                .containsExactly(first, second, links.get(2).distributorId());
    }
}
