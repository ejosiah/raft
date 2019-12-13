package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.Acknowledgement;
import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.server.event.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class Leader extends NodeState {

    @Override
    public void init() {
        sendHeartbeat();
    }


    @Override
    public void handle(ReceivedCommandEvent event) {
        node.add(event.command());
        event.sender().writeAndFlush(Acknowledgement.successful());
        node.replicate();   // TODO don't send if previously sent pending response
    }

    @Override
    public void handle(AppendEntriesReplyEvent event) {
        if(event.msg().isSuccess()){
            event.sender().nextIndex = node.log.getLastIndex() + 1;
            event.sender().matchIndex = node.log.getLastIndex();

            long lastCommitIndex = node.commitIndex;
            long lastLogIndex = node.log.getLastIndex(); // TODO cache this

            long nextCommitIndex = 0;
            for(long n = lastCommitIndex + 1;  n <= lastLogIndex; n++){
                if(n > lastCommitIndex && majorityMatchIndexGreaterThanOrEqualTo(n)  && node.log.get(n).getTerm() == node.currentTerm){
                    nextCommitIndex = Math.max(nextCommitIndex, n);
                }
            }
            if(nextCommitIndex > 0){
                node.trigger(new CommitEvent(nextCommitIndex, node.id));
            }
        }else{
            event.sender().nextIndex--;
            node.sendAppendEntriesTo(event.sender(), event.sender().nextIndex);
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
        return replicated >= node.config.majority || (float)(replicated/node.activePeers.size()) >= 0.5;
    }

    @Override
    public void handle(HeartbeatTimeoutEvent heartbeatTimeoutEvent) {
        sendHeartbeat();
    }


    // TODO, we may need to do this on a different thread, if  we get too much traffic from clients
    // we may not be able to send heartbeat in time
    private void sendHeartbeat(){
        AppendEntries heartbeat = AppendEntries.heartbeat(node.currentTerm, node.prevLogIndex(), node.prevLogTerm(), node.commitIndex, node.id);
        node.activePeers.values().forEach(peer -> peer.send(heartbeat));
        node.trigger(new ScheduleHeartbeatTimeoutEvent(node.id, node.nextHeartbeatTimeout()));
    }
}
