package com.s0rryhorizon.appliedchannelmanagement.grid;

public record ChannelPoolSnapshot(int totalCapacity, int wiredUsed, int wirelessUsed, boolean infinite) {
    public static final ChannelPoolSnapshot EMPTY = new ChannelPoolSnapshot(0, 0, 0, false);

    public int remaining() {
        return infinite ? Integer.MAX_VALUE : Math.max(0, totalCapacity - wiredUsed - wirelessUsed);
    }
}
