package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.client.Response;
import com.josiahebhomenye.raft.rpc.AppendEntriesReply;
import com.josiahebhomenye.raft.rpc.Redirect;
import com.josiahebhomenye.raft.rpc.RequestVoteReply;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.event.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class Follower extends NodeState {

    protected Follower(){}

    public void init() {
        super.init();
        node.votes = 0;
        node.votedFor = null;
        node.trigger(new ScheduleTimeoutEvent(node.channel, node.nextTimeout()));
    }

    public void handle(AppendEntriesEvent event){
        node.lastHeartbeat = Instant.now();
        node.trigger(new ScheduleTimeoutEvent(node.channel, node.nextTimeout()));
        if(event.msg().getTerm() < node.currentTerm){
            event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, 0, false));
            return;
        }

        if(!node.log.isEmpty()) {
            LogEntry prevEntry = node.log.get(event.msg().getPrevLogIndex());
            if (prevEntry == null || prevEntry.getTerm() != event.msg().getPrevLogTerm()) {
                event.sender().writeAndFlush(new AppendEntriesReply(node.currentTerm, 0, false));
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
        if(nextCommitIndex > node.commitIndex) {
            node.trigger(new CommitEvent(nextCommitIndex, node.channel));
        }else{
            AppendEntriesReply reply = new AppendEntriesReply(node.currentTerm, node.log.size(), true);
            log.info("node peer {}, event peer {}, reply {}", node.sender(), event.sender(), reply);
            node.sender().writeAndFlush(reply);
        }
    }

    @Override
    public void handle(ReceivedRequestEvent event) {
        if(node.leaderId != null) {
            event.sender().writeAndFlush(new Redirect(node.leaderId, event.request()));
        }else {
            super.handle(event);
        }
    }

    @Override
    public void handle(ElectionTimeoutEvent event) {
        if(node.receivedHeartbeatSinceLast(event.lastheartbeat)){
            node.trigger(new ScheduleTimeoutEvent(node.channel, node.nextTimeout()));
        }else{
            transitionTo(CANDIDATE());
        }
    }

    @Override
    public void handle(RequestVoteEvent event) {
        if(node.alreadyVoted() || node.currentTerm > event.requestVote().getTerm() || logEntryIsNotUpToDate(event)){
            event.sender().writeAndFlush(new RequestVoteReply(node.currentTerm, false));
        }else{
            node.votedFor = event.requestVote.getCandidateId();
            event.sender().writeAndFlush(new RequestVoteReply(node.currentTerm, true));
        }
    }

    private boolean logEntryIsNotUpToDate(RequestVoteEvent event){
        LogEntry lastEntry = node.log.lastEntry();
        return (lastEntry != null && event.requestVote().getLastLogTerm() < lastEntry.getTerm())
                || event.requestVote().getLastLogIndex() < node.log.getLastIndex();
    }


    @Override
    public void handle(CommitEvent event) {
        super.handle(event);
        node.sender.writeAndFlush(new AppendEntriesReply(node.currentTerm,  node.log.size(), true));
    }

    @Override
    public boolean isFollower() {
        return true;
    }

    @Override
    public Id id() {
        return Id.FOLLOWER;
    }
}
