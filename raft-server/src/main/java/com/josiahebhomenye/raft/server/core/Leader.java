package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.server.event.*;

public class Leader extends NodeState {

    @Override
    public void init() {
        sendHeartbeat();
    }

    @Override
    public void handle(ElectionTimeoutEvent event) {

    }

    @Override
    public void handle(RequestVoteEvent event) {

    }

    @Override
    public void handle(RequestVoteReplyEvent event) {

    }

    @Override
    public void handle(HeartbeatTimeoutEvent heartbeatTimeoutEvent) {
        sendHeartbeat();
    }

    private void sendHeartbeat(){
        AppendEntries heartbeat = AppendEntries.heartbeat(node.currentTerm, node.prevLogIndex(), node.prevLogTerm(), node.commitIndex, node.id);
        node.activePeers.values().forEach(peer -> peer.send(heartbeat));
        node.trigger(new ScheduleHeartbeatTimeoutEvent(node.id, node.nextHeartbeatTimeout()));
    }
}
