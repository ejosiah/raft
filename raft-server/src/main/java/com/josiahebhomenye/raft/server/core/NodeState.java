package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntriesReply;
import com.josiahebhomenye.raft.server.event.*;

import static com.josiahebhomenye.raft.server.core.NodeState.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public abstract class NodeState {

    static final Follower FOLLOWER = new Follower();
    static final Candidate CANDIDATE = new Candidate();
    static final Leader LEADER = new Leader();
    static final NodeState NULL_STATE = new NodeState() {};

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
        node.trigger(new ScheduleTimeoutEvent(node.id, node.nextTimeout()));
        if(event.msg().getTerm() < node.currentTerm){
            event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, false));
            return;
        }

        if(!node.log.isEmpty()) {
            LogEntry prevEntry = node.log.get(event.msg().getPrevLogIndex());
            if (prevEntry.getTerm() != event.msg().getPrevLogTerm()) {
                event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, false));
                return;
            }
        }

        if(!event.msg().getEntries().isEmpty()) {
            List<LogEntry> entries = event.msg().getEntries().stream().map(LogEntry::deserialize).collect(Collectors.toList());

            long nexEntryIndex = event.msg().getPrevLogIndex() + 1L;
            if (node.log.size() >= nexEntryIndex) {
                LogEntry entry = node.log.get(nexEntryIndex);
                if (entry.getTerm() != entries.get(0).getTerm()){
                    node.log.deleteFromIndex(nexEntryIndex);
                }
            }
            IntStream.range(0, entries.size()).forEach(i -> node.log.add(entries.get(i), nexEntryIndex + i));
        }
        node.currentTerm = event.msg().getTerm();
        if(event.msg().getLeaderCommit() > node.commitIndex){
            node.commitIndex = Math.min(event.msg().getLeaderCommit(), node.log.getLastIndex());
            if(this != FOLLOWER){
                transitionTo(FOLLOWER);
            }
        }
        event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, true));
    }

    public void init(){}

    public void handle(ElectionTimeoutEvent event){}

    public void handle(RequestVoteEvent event){}

    public void handle(RequestVoteReplyEvent event){}

    public void handle(HeartbeatTimeoutEvent heartbeatTimeoutEvent){}

    public String name(){
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return name();
    }
}
