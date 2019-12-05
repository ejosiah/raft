package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.RequestVoteReply;
import com.josiahebhomenye.raft.server.event.ElectionTimeoutEvent;
import com.josiahebhomenye.raft.server.event.RequestVoteEvent;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class Follower extends NodeState {

    public void init() {
        scheduleElectionTimeout();
    }

    @Override
    public void handle(ElectionTimeoutEvent event) {
        if(receivedHeartbeatSinceLast(event.lastheartbeat)){
            scheduleElectionTimeout();
        }else{
            transitionTo(CANDIDATE);
        }
    }

    @Override
    public void handle(RequestVoteEvent event) {
        if(node.alreadyVoted() || node.currentTerm > event.requestVote().getTerm() || logEntryIsNotUpToDate(event)){
            event.sender().writeAndFlush(new RequestVoteReply(node.currentTerm, false));
        }else{
            event.sender().writeAndFlush(new RequestVoteReply(node.currentTerm, true));
        }
    }

    private boolean logEntryIsNotUpToDate(RequestVoteEvent event){
        return event.requestVote().getLastLogTerm() < node.log.lastEntry().getTerm()
                || event.requestVote().getLastLogIndex() < node.log.getLastIndex();
    }


    private void scheduleElectionTimeout(){
        node.group.schedule(() -> node.trigger(new ElectionTimeoutEvent(node.lastheartbeat, node.id))
                , node.config.electionTimeout.get(), TimeUnit.MILLISECONDS);
    }

    private boolean receivedHeartbeatSinceLast(Instant heartbeat){
        return heartbeat != node.lastheartbeat && heartbeat.isBefore(node.lastheartbeat);
    }
}
