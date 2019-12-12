package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.RequestVoteReply;
import com.josiahebhomenye.raft.server.event.*;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class Follower extends NodeState {

    protected Follower(){}

    public void init() {
        node.trigger(new ScheduleTimeoutEvent(node.id, node.nextTimeout()));
    }

    @Override
    public void handle(ElectionTimeoutEvent event) {
        if(node.receivedHeartbeatSinceLast(event.lastheartbeat)){
            node.trigger(new ScheduleTimeoutEvent(node.id, node.nextTimeout()));
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
}
