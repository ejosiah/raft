package com.josiahebhomenye.raft.server.config;

import com.josiahebhomenye.raft.server.util.Timeout;
import com.typesafe.config.Config;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

@With
@RequiredArgsConstructor
public class ServerConfig {

    public final Timeout electionTimeout;
    public final Timeout heartbeatTimeout;
    public final int majority;
    public final InetSocketAddress id;
    public final List<InetSocketAddress> peers;
    public final String logPath;
    public final String statePath;

    public ServerConfig(Config config){
        electionTimeout = new ElectionTimeout(config);
        heartbeatTimeout = new HeartbeatTimeout(config);
        majority = config.getInt("raft.majority");
        id = new InetSocketAddress(config.getString("raft.server.host"), config.getInt("raft.server.port"));
        logPath = config.getString("raft.path.log");
        statePath = config.getString("raft.path.state");
        peers = config.getStringList("raft.peers")
                .stream()
                .map(it -> it.split(":"))
                .map(it -> new InetSocketAddress(it[0], Integer.parseInt(it[1]))).collect(Collectors.toList());
    }

}
