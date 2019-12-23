package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.LongStream;

/**
 * if a log entry is committed in a given term,
 * then that entry will be present in the logs of the leaders for all higher-numbered terms
 */

@ChannelHandler.Sharable
public class LeaderCompletenessGuarantee extends Guarantee {

    Node prevLeader;

    private static final Logger logger = LoggerFactory.getLogger(LeaderCompletenessGuarantee.class);

    public LeaderCompletenessGuarantee(List<Node> nodes, CountDownLatch testEndLatch) {
        super(nodes, testEndLatch);
    }

    public void check(Node source, StateTransitionEvent event) {
        if(event.newState().isLeader()){
            if (prevLeader != null) {
                try(Log prevLeaderLog = prevLeader.log().clone()) {
                    Node newLeader = event.oldState().node();
                    try(Log newLeaderLog = newLeader.log().clone() ) {
                        long committed = prevLeader.commitIndex();
                        for(long i = 1; i <= committed; i++){
                            if(!prevLeaderLog.get(i).equals(newLeaderLog.get(i))){
                                logger.info("guarantee failed previous {} log entry {} not present in current leader {} entry {}",
                                        prevLeader, prevLeaderLog.get(i), newLeader, newLeaderLog.get(i));
                                fail();
                                break;
                            }
                        }
                    }
                }
            }
            prevLeader = event.oldState().node();
        }
    }
}
