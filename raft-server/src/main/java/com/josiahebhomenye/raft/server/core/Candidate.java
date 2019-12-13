package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.server.event.*;

public class Candidate extends NodeState {

    @Override
    public void init() {
        node.currentTerm++;
        node.votedFor = node.id;
        node.votes = 1;
        node.trigger(new ScheduleTimeoutEvent(node.id, node.nextTimeout()));
        long lastIndex = node.log.isEmpty() ? 0 : node.log.getLastIndex();
        long lastTerm = node.log.isEmpty() ? 0 : node.log.lastEntry().getTerm();
        node.trigger(new SendRequestVoteEvent(new RequestVote(node.currentTerm, lastIndex, lastTerm, node.id)));
    }

    @Override
    public void handle(ElectionTimeoutEvent event) {
        init();
    }

    @Override
    public void handle(RequestVoteReplyEvent event) {
        if(event.reply().isVoteGranted()){
            node.votes++;
            if(receivedMajorityVotes()){
                transitionTo(LEADER);
            }
        }
    }

    private boolean receivedMajorityVotes(){
        return node.votes >= node.config.majority || (float)(node.votes/node.activePeers.size()) >= 0.5;
    }
}
