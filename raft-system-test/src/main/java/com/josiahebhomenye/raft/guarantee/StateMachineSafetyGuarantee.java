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

@ChannelHandler.Sharable
public class StateMachineSafetyGuarantee extends Guarantee {

    public StateMachineSafetyGuarantee(List<Node> nodes, CountDownLatch testEndLatch) {
        super(nodes, testEndLatch);
    }

    @Override
    protected void check(ChannelHandlerContext ctx, Event evt) {
        if(evt instanceof ApplyEntryEvent){
            receivedExpectedEvent = true;
            ApplyEntryEvent event = evt.as(ApplyEntryEvent.class);
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
