package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntriesReply;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.ElectionTimeoutEvent;
import com.josiahebhomenye.raft.server.event.RequestVoteEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;

public abstract class NodeState {

    static final NodeState FOLLOWER = new Follower();
    static final NodeState CANDIDATE = new Candidate();
    static final NodeState LEADER = new Leader();

    protected Node node;


    public void transitionTo(NodeState newState){
        if(newState != this) {
            newState.set(node);
            node.trigger(new StateTransitionEvent(this, newState, node.id));
        }
    }

    public void set(Node node){
        node.state = this;
        this.node = node;
    }

    public void handle(AppendEntriesEvent event){
        if(event.msg().term() < node.currentTerm){
            event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, false));
            return;
        }
        LogEntry prevEntry = node.log.get(event.msg().prevLogIndex());
        if(prevEntry.getTerm() != event.msg().prevLogTerm()){
            event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, false));
        }
    }

    public abstract void init();

    public abstract void handle(ElectionTimeoutEvent event);

    public abstract void handle(RequestVoteEvent event);

    public static final NodeState NULL_STATE = new NodeState() {

        @Override
        public void handle(AppendEntriesEvent event) {

        }

        @Override
        public void init() {

        }

        @Override
        public void handle(ElectionTimeoutEvent event) {

        }

        @Override
        public void handle(RequestVoteEvent event) {

        }
    };

    public String name(){
        return this.getClass().getSimpleName();
    }
}
