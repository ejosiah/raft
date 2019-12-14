package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntriesReply;
import com.josiahebhomenye.raft.RedirectCommand;
import com.josiahebhomenye.raft.RequestVoteReply;
import com.josiahebhomenye.raft.server.event.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Follower extends NodeState {

    protected Follower(){}

    public void init() {
        node.votes = 0;
        node.votedFor = null;
        node.trigger(new ScheduleTimeoutEvent(node.id, node.nextTimeout()));
    }

    public void handle(AppendEntriesEvent event){
        node.lastHeartbeat = Instant.now();
        node.trigger(new ScheduleTimeoutEvent(node.id, node.nextTimeout()));
        if(event.msg().getTerm() < node.currentTerm){
            event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, false));
            return;
        }

        if(!node.log.isEmpty()) {
            LogEntry prevEntry = node.log.get(event.msg().getPrevLogIndex());
            if (prevEntry != null && prevEntry.getTerm() != event.msg().getPrevLogTerm()) {
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

        node.leaderId = event.msg().getLeaderId();
        long nextCommitIndex = Math.min(event.msg().getLeaderCommit(), node.log.getLastIndex());
        event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, true));
        if(nextCommitIndex > node.commitIndex) {
            node.trigger(new CommitEvent(nextCommitIndex, node.id));
        }
    }

    @Override
    public void handle(ReceivedCommandEvent event) {
        event.sender().writeAndFlush(new RedirectCommand(node.leaderId, event.command()));
    }

    @Override
    public void handle(ElectionTimeoutEvent event) {
        if(node.receivedHeartbeatSinceLast(event.lastheartbeat)){
            node.trigger(new ScheduleTimeoutEvent(node.id, node.nextTimeout()));
        }else{
            transitionTo(CANDIDATE());
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
        LogEntry lastEntry = node.log.lastEntry();
        return (lastEntry != null && event.requestVote().getLastLogTerm() < lastEntry.getTerm())
                || event.requestVote().getLastLogIndex() < node.log.getLastIndex();
    }
}
