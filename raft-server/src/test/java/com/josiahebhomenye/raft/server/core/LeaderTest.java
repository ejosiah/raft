package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.server.event.HeartbeatTimeoutEvent;
import com.josiahebhomenye.raft.server.event.ScheduleHeartbeatTimeoutEvent;
import com.josiahebhomenye.raft.server.event.ScheduleTimeoutEvent;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.*;
import static com.josiahebhomenye.raft.server.core.NodeState.*;

public class LeaderTest extends NodeStateTest {

    Leader leader;

    @Before
    public void setup(){
        node.activePeers.put(new InetSocketAddress("localhost", 9000), new Peer(null, null, null));
        node.activePeers.put(new InetSocketAddress("localhost", 9001), new Peer(null, null, null));
        node.activePeers.put(new InetSocketAddress("localhost", 9002), new Peer(null, null, null));
        node.activePeers.put(new InetSocketAddress("localhost", 9003), new Peer(null, null, null));
        node.activePeers.values().forEach(peer -> peer.channel = channel);
    }

    @Override
    public NodeState initializeState() {
        leader =  LEADER;
        leader.set(node);
        return leader;
    }

    @Test
    public void send_heartbeat_on_initialization(){
        node.currentTerm = 1;
        leader.init();

        AppendEntries expected = AppendEntries.heartbeat(node.currentTerm, 0L, 0L, 0L, node.id);

        assertEquals(expected, channel.readOutbound());
        assertEquals(expected, channel.readOutbound());
        assertEquals(expected, channel.readOutbound());
        assertEquals(expected, channel.readOutbound());

        ScheduleHeartbeatTimeoutEvent event = userEventCapture.get(0);

        assertEquals(50L, event.timeout());
    }

    @Test
    public void send_heartbeat_on_heartbeat_timeout_event(){
        node.currentTerm = 1;
        leader.handle(new HeartbeatTimeoutEvent(node.id));

        AppendEntries expected = AppendEntries.heartbeat(node.currentTerm, 0L, 0L, 0L, node.id);

        assertEquals(expected, channel.readOutbound());
        assertEquals(expected, channel.readOutbound());
        assertEquals(expected, channel.readOutbound());
        assertEquals(expected, channel.readOutbound());

        ScheduleHeartbeatTimeoutEvent event = userEventCapture.get(0);

        assertEquals(50L, event.timeout());
    }
}
