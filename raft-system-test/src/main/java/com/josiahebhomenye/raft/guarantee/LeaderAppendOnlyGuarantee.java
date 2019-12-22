package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.ReceivedRequestEvent;
import com.josiahebhomenye.test.support.StateDataSupport;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
import java.util.stream.LongStream;

/**
 * a leader never overwrites or deletes entries in its log; it only appends new entries
 */

@Slf4j
@ChannelHandler.Sharable
public class LeaderAppendOnlyGuarantee extends Guarantee implements StateDataSupport {

    public static final String LOG_PATH = "append_only_log_check.log";

    @Getter
    @Accessors(fluent = true)
    Log logCopy;

    @Override
    public LeaderAppendOnlyGuarantee setup() {
        delete(LOG_PATH);
        logCopy = new Log(LOG_PATH, 8);
        return this;
    }

    @Override
    public LeaderAppendOnlyGuarantee tearDown() {
        if(logCopy != null) {
            logCopy.close();
        }
        delete(LOG_PATH);
        return this;
    }

    public LeaderAppendOnlyGuarantee(List<Node> nodes, CountDownLatch latch) {
        super(nodes, latch);
    }

    @Override
    protected void check(Node Source, Event event) {
        if(event instanceof ReceivedRequestEvent && Source.isLeader()){
            logCopy.add(new LogEntry(Source.currentTerm(), ((ReceivedRequestEvent) event).request().getBody()));
            receivedExpectedEvent = true;   // TODO move up to super class
            LongStream.range(0, logCopy.size()).forEach(i -> {
                long atIndex = i + 1;
                try(Log leaderLog = Source.log().clone()) {
                    LogEntry prevLeaderEntry = logCopy.get(atIndex);
                    LogEntry leaderLogEntry = leaderLog.get(atIndex);
                    if(!prevLeaderEntry.equals(leaderLogEntry)){
                        log.info("log entries at index {} don't match, {}, {}", atIndex, leaderLogEntry, prevLeaderEntry);
                        fail();
                    }
                }
            });
        }
    }
}
