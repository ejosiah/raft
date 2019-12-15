package com.josiahebhomenye.raft.client.config;


import com.josiahebhomenye.raft.client.EntrySerializer;
import com.typesafe.config.Config;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

@With
@Builder
@RequiredArgsConstructor
public class ClientConfig {
    public final long acquireTimeout;
    public final long maxConnections;
    public final int maxPendingAcquires;
    public final List<InetSocketAddress> servers;
    public final int nThreads;
    public final Class<EntrySerializer<?>> entrySerializerClass;

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public ClientConfig(Config config){
        if(config.getBoolean("raft.connection.pooling.enabled")){
            this.maxConnections = config.getLong("raft.connection.max");
            this.acquireTimeout = config.getDuration("raft.connection.acquire.timeout").toMillis();
            this.maxPendingAcquires = config.getInt("raft.connection.acquire.max");
        }else{
            this.maxConnections = 1;
            this.acquireTimeout  = 0L;
            this.maxPendingAcquires = 0;
        }
        servers = config.getStringList("raft.peers")
                    .stream()
                    .map(it -> it.split(":"))
                    .map(it -> new InetSocketAddress(it[0], Integer.parseInt(it[1]))).collect(Collectors.toList());
        this.nThreads = config.getInt("raft.connection.nThreads");
        this.entrySerializerClass  = (Class<EntrySerializer<?>>) Class.forName(config.getString("raft.serializer"));
    }

}
