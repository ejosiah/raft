package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.Environment;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.test.support.LogDomainSupport;
import com.josiahebhomenye.test.support.UserEventCapture;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class PeerTest implements LogDomainSupport {

    Peer peer;
    Node node;
    ServerConfig config;
    EmbeddedChannel channel;
    UserEventCapture userEventCapture;

    @Before
    public void setup(){
        config = new ServerConfig(ConfigFactory.load());
        node = new Node(config);
        peer = new Peer(config.peers.get(1), node, node.clientGroup);
        userEventCapture = new UserEventCapture();
        channel = new EmbeddedChannel(userEventCapture, peer.connectionHandler);
        node.channel = channel;
        peer.channel = channel;
    }


    @Test
    public void test_me(){
        log.info(Environment.CURRENT.name());
    }

}