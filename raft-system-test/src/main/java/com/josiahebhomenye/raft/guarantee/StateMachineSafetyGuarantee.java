package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.ApplyEntryEvent;
import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.core.Node;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * if a server has applied a log entry at a given index to its state machine,
 * no other server will ever apply a different log entry for the same index
 */

@ChannelHandler.Sharable
public class StateMachineSafetyGuarantee extends Guarantee {

    public StateMachineSafetyGuarantee(List<Node> nodes, CountDownLatch testEndLatch) {
        super(nodes, testEndLatch);
    }

    @Override
    protected void check(Node source, Event evt) {
        if(evt instanceof ApplyEntryEvent){
            receivedExpectedEvent = true;
            ApplyEntryEvent event = evt.as(ApplyEntryEvent.class);
            nodes.stream().filter(node -> !node.equals(source)).forEach(node -> {

            });
            for(Node node : nodes){
                try(Log log = node.log().clone()) {
                    LogEntry entry = log.get(event.index());
                    if(entry != null && !entry.equals(event.entry())){
                        fail();
                        break;
                    }
                }
            }
        }
    }
}
