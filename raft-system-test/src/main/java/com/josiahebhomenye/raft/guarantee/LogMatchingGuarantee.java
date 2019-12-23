package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.ApplyEntryEvent;
import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.server.core.Node;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * if two logs contain an entry with the same index and term,
 * then the logs are identical in all entries up through the given index.
 */

@ChannelHandler.Sharable
public class LogMatchingGuarantee extends Guarantee {

    public LogMatchingGuarantee(List<Node> nodes, CountDownLatch testEndLatch) {
        super(nodes, testEndLatch);
    }

    public void check(Node source, ApplyEntryEvent event) {
        long index = event.index();
        try(Log log = source.log().clone()){
            for(Node node : nodes){
                try(Log otherLog = node.log().clone()){
                    if(log.get(index).equals(otherLog.get(index))){
                        for(long i = index-1; i > 0; i--){
                            if(!log.get(i).equals(otherLog.get(i))){
                                fail();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
