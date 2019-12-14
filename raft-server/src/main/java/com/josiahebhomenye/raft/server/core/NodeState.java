package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntriesReply;
import com.josiahebhomenye.raft.server.event.*;

import java.util.Objects;

public abstract class NodeState {

    // FIXME change to enum
    public static final Follower FOLLOWER(){ return new Follower(); }
    public static final Candidate CANDIDATE() {return new Candidate(); }
    public static final Leader LEADER()  { return new Leader(); };
    public static final NodeState NULL_STATE() { return new NullState(); };

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
        if(event.msg().getTerm() >= node.currentTerm) { // FIXME probably also need to check log
            transitionTo(FOLLOWER());
            node.trigger(event);
        }else{
            event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, false));
        }
    }

    public void init(){
    }

    public void handle(ElectionTimeoutEvent event){}

    public void handle(RequestVoteEvent event){
        if(event.requestVote().getTerm() > node.currentTerm){
            node.currentTerm = event.requestVote().getTerm();
            transitionTo(FOLLOWER());
            node.trigger(event);
        }
    }

    public void handle(RequestVoteReplyEvent event){}

    public void handle(HeartbeatTimeoutEvent event){}

    public void handle(PeerConnectedEvent event){

    }

    public void handle(ReceivedCommandEvent event){}

    public String name(){
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return name();
    }

    public void handle(AppendEntriesReplyEvent event){

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NodeState)) return false;
        NodeState nodeState = (NodeState) o;
        return nodeState.name().equals(name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name());
    }


    public static class NullState extends NodeState{

    }
}
