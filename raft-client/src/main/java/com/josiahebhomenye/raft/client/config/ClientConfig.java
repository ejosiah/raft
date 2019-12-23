package com.josiahebhomenye.raft.client.config;


import com.josiahebhomenye.raft.client.EntrySerializer;
import com.typesafe.config.Config;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.With;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@With
@Builder
@RequiredArgsConstructor
public class ClientConfig {
    public final long acquireTimeout;
    public final int maxConnections;
    public final int maxPendingAcquires;
    public final List<InetSocketAddress> servers;
    public final int nThreads;
    public final boolean poolingEnabled;
    public final Class<? extends EntrySerializer<?>> entrySerializerClass;
    public final Long requestTimeout;

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public ClientConfig(Config config){
        this.poolingEnabled = config.getBoolean("raft.client.connection.pooling");
        this.requestTimeout = config.getDuration("raft.client.connection.request.timeout").toMillis();
        if(poolingEnabled){
            this.maxConnections = config.getInt("raft.client.connection.max");
            this.acquireTimeout = config.getDuration("raft.client.connection.acquire.timeout").toMillis();
            this.maxPendingAcquires = config.getInt("raft.client.connection.acquire.max");
        }else{
            this.maxConnections = 1;
            this.acquireTimeout  = 300L;
            this.maxPendingAcquires = 1;
        }
        servers = config.getStringList("raft.client.servers")
                    .stream()
                    .map(it -> it.split(":"))
                    .map(it -> new InetSocketAddress(it[0], Integer.parseInt(it[1]))).collect(Collectors.toList());
        this.nThreads = config.getInt("raft.client.connection.nThreads");
        this.entrySerializerClass  = (Class<? extends EntrySerializer<?>>) Class.forName(config.getString("raft.client.serializer"));
    }

    public <T> Optional<T> toOptional(Supplier<T> supplier){
        try{
            return Optional.ofNullable(supplier.get());
        }catch (Exception ex){
            return Optional.empty();
        }
    }

}
