package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.client.Response;
import com.josiahebhomenye.raft.rpc.AppendEntries;
import com.josiahebhomenye.raft.rpc.AppendEntriesReply;
import com.josiahebhomenye.raft.server.event.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Leader extends NodeState {

    @Override
    public void init() {
        node.activePeers.values().forEach(node::heartbeat);
        node.trigger(new ScheduleHeartbeatTimeoutEvent(node.channel, node.nextHeartbeatTimeout()));
    }

    @Override
    public void handle(PeerConnectedEvent event) {
        event.peer().trigger(new ScheduleHeartbeatTimeoutEvent(node.channel, node.nextHeartbeatTimeout()));
    }

    @Override
    public void transitionTo(NodeState newState) {
        if(!this.equals(newState)) {
            node.trigger(new CancelHeartbeatTimeoutEvent());
            super.transitionTo(newState);
        }
    }


    @Override
    public void handle(ReceivedRequestEvent event) {
        node.add(event.request().getBody());
        event.sender().writeAndFlush(Response.empty(event.request().getId(), true)); // TODO Don't reply the peer reply downstream
        node.replicate();   // TODO don't send if previously sent pending response
    }

    @Override
    public void handle(HeartbeatTimeoutEvent event) {
        AppendEntries heartbeat = node.heartbeat(event.peer());
        event.peer().send(heartbeat);
    }

    @Override
    public void handle(AppendEntriesReplyEvent event) {
        Peer peer = event.sender();
        AppendEntriesReply reply = event.msg();
        if(reply.isSuccess()){
            long lastLogIndex = node.log.getLastIndex(); // TODO cache this
            peer.matchIndex =  Math.max(peer.matchIndex, reply.getLastApplied());
            peer.nextIndex =  Math.max(peer.matchIndex + 1, lastLogIndex + 1);

            long lastCommitIndex = node.commitIndex;

            long nextCommitIndex = 0;
            for(long n = lastCommitIndex + 1;  n <= lastLogIndex; n++){
                if(n > lastCommitIndex && majorityMatchIndexGreaterThanOrEqualTo(n)  && node.log.get(n).getTerm() == node.currentTerm){
                    nextCommitIndex = Math.max(nextCommitIndex, n);
                }
            }
            if(nextCommitIndex > 0){
                node.trigger(new CommitEvent(nextCommitIndex, node.channel));
            }
        }else{
            peer.nextIndex--;
            node.sendAppendEntriesTo(peer);
        }
    }

    private boolean majorityMatchIndexGreaterThanOrEqualTo(long n) {
        int replicated = node.log.hasEntryAt(n) ? 1 : 0;

        for(Peer peer : node.activePeers.values()){
            if(peer.matchIndex >= n) replicated++;
        }
        return replicatedOnMajority(replicated);
    }

    private long majorityMatchIndex(long n){
        Collection<List<Peer>> peersList =  node.activePeers.values().stream().collect(Collectors.groupingBy(peer -> peer.matchIndex)).values();
        for(List<Peer> peers : peersList){
            int replicatedHere = node.log.hasEntryAt(peers.get(0).matchIndex) ? 1 : 0;
            int size = peers.size() + replicatedHere;
            if(size >= node.config.majority || (float)(size/node.activePeers.size()) >= 0.5) return peers.get(0).matchIndex;
        }
        return 0;
    }

    private boolean replicatedOnMajority(int replicated){
        return replicated >= node.config.majority;
    }

    @Override
    public boolean isLeader() {
        return true;
    }

    @Override
    public Id id() {
        return Id.LEADER;
    }
}
