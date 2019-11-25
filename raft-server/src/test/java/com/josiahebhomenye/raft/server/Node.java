package com.josiahebhomenye.raft.server;


import io.netty.channel.Channel;

import java.net.InetSocketAddress;

public class Node {
    long currentTerm;
    long commitIndex;
    long lastApplied;
    InetSocketAddress votedFor;
    Log log;
    long[] nextIndex;
    long[] matchIndex;
    Peer[] peers;

    private static class Peer{
        long nextIndex;
        long matchIndex;
        Channel channel;
    }
}
