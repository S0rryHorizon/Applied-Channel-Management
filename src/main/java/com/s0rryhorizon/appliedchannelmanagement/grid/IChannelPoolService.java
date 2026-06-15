package com.s0rryhorizon.appliedchannelmanagement.grid;

import appeng.api.networking.IGridService;

public interface IChannelPoolService extends IGridService {
    int getCapacity();

    void updateAllocation(int wired, int wireless);

    ChannelPoolSnapshot getSnapshot();
}
