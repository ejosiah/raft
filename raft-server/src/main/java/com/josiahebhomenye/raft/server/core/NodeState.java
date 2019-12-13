package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntriesReply;
import com.josiahebhomenye.raft.server.event.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class NodeState {

    public static final Follower FOLLOWER = new Follower();
    public static final Candidate CANDIDATE = new Candidate();
    public static final Leader LEADER = new Leader();
    public static final NodeState NULL_STATE = new NodeState() {};

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

    public void handle(AppendEntriesEvent event) {
        if(event.msg().getTerm() >= node.currentTerm) {
            transitionTo(FOLLOWER);
            node.trigger(event);
        }else{
            event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, false));
        }
    }

    public void init(){}

    public void handle(ElectionTimeoutEvent event){}

    public void handle(RequestVoteEvent event){
        if(event.requestVote().getTerm() > node.currentTerm){
            node.currentTerm = event.requestVote().getTerm();
            transitionTo(FOLLOWER);
            node.trigger(event);
        }
    }

    public void handle(RequestVoteReplyEvent event){}

    public void handle(HeartbeatTimeoutEvent heartbeatTimeoutEvent){}

    public void handle(ReceivedCommandEvent event){};

    public String name(){
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return name();
    }

    public void handle(AppendEntriesReplyEvent event){

    }
}
