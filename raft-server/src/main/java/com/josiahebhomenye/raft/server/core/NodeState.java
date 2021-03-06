package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.client.Response;
import com.josiahebhomenye.raft.rpc.AppendEntriesReply;
import com.josiahebhomenye.raft.rpc.RequestVoteReply;
import com.josiahebhomenye.raft.server.event.*;
import lombok.Getter;
import lombok.experimental.Accessors;
import static com.josiahebhomenye.raft.server.core.NodeState.Id.*;
import java.util.Objects;

public abstract class NodeState {

    public enum Id{FOLLOWER, CANDIDATE, LEADER, NOTHING}

    public static final NodeState NULL_STATE = new NullState();

    @Getter
    @Accessors(fluent = true)
    protected Node node;

    public void transitionTo(NodeState.Id stateId){
        NodeState newState = this.node.states.get(stateId);
        if(!newState.equals(this)) {
            newState.set(node); // FIXME remove this, Node handler of state transition will take care of this
            node.trigger(new StateTransitionEvent(this, newState, node.channel));
        }
    }

    public NodeState set(Node node){
        node.state = this;
        this.node = node;
        return this;
    }

    public void handle(AppendEntriesEvent event) {
        if(event.msg().getTerm() >= node.currentTerm) { // FIXME probably also need to check log
            transitionTo(FOLLOWER);
            node.trigger(event);
        }else{
            event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, 0, false));
        }
    }

    public void init(){
    }

    public void handle(ElectionTimeoutEvent event){}

    public void handle(RequestVoteEvent event){
        if(event.requestVote().getTerm() > node.currentTerm){
            node.votedFor = null;
            transitionTo(FOLLOWER);
            node.trigger(event);
        }else{
            event.sender().writeAndFlush(new RequestVoteReply(node.currentTerm, false));
        }
    }

    public void handle(CommitEvent event){
        node.commitIndex = event.index();
        node.applyLogEntries();
    }

    public void handle(RequestVoteReplyEvent event){}

    public void handle(HeartbeatTimeoutEvent event){}

    public void handle(PeerConnectedEvent event){

    }

    public void handle(ReceivedRequestEvent event){
        if(node.leaderId == null){
            event.sender().writeAndFlush(Response.fail(event.request().getId(), "no leader elected yet".getBytes()));
        }
    }

    public String name(){
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return name();
    }

    public void handle(AppendEntriesReplyEvent event){

    }

    public Id id(){
        return Id.NOTHING;
    }

    public boolean isFollower(){
        return false;
    }

    public boolean isCandidate(){
        return false;
    }

    public boolean isLeader(){
        return false;
    }

    public static NodeState get(Id id){
        switch (id){
            case FOLLOWER: return Follower.getInstance();
            case CANDIDATE: return Candidate.getInstance();
            case LEADER : return Leader.getInstance();
            default: return NULL_STATE;
        }
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
