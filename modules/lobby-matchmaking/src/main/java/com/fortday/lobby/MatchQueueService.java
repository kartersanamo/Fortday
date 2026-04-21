package com.fortday.lobby;

import java.util.ArrayList;
import java.util.List;

public final class MatchQueueService {
    private final List<QueueTicket> queue = new ArrayList<>();

    public void enqueue(QueueTicket ticket) {
        queue.add(ticket);
    }

    public int size() {
        return queue.size();
    }
}
