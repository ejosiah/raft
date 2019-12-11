package com.josiahebhomenye.raft.server.core;


import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.ElectionTimeoutEvent;
import com.josiahebhomenye.raft.server.event.HeartbeatTimeoutEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import com.josiahebhomenye.raft.server.handlers.ServerChannelInitializer;
import com.josiahebhomenye.raft.server.handlers.ServerClientChannelInitializer;
import com.josiahebhomenye.raft.server.util.Dynamic;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.josiahebhomenye.raft.server.core.NodeState.FOLLOWER;
import static com.josiahebhomenye.raft.server.core.NodeState.NULL_STATE;

@Getter
public class Node extends ChannelDuplexHandler {
    long currentTerm;
    long commitIndex;
    long lastApplied;
    InetSocketAddress votedFor;
    Log log;
    long[] nextIndex;
    long[] matchIndex;
    List<Peer> peers;
    Map<InetSocketAddress, Peer> activePeers;
    Channel channel;
    NodeState state;
    EventLoopGroup group;
    EventLoopGroup clientGroup;
    ServerConfig config;
    Instant lastheartbeat;
    InetSocketAddress id;
    int votes;
    ScheduledFuture<?> scheduledElectionTimeout;

    public Node(ServerConfig config){
        this.state = NULL_STATE;
        this.state.set(this);
        this.config = config;
        this.id = config.id;
        this.activePeers = new HashMap<>();
        this.peers = new ArrayList<>();
        votes = 0;
        initStateData();
    }

    void initStateData(){
        try(DataInputStream in = new DataInputStream(new FileInputStream("state.dat"))){
            currentTerm = in.readInt();
            votedFor = new InetSocketAddress(in.readUTF(), in.readInt());
        }catch(Exception ex){
            currentTerm = 0;
            votedFor = null;
        }
        log = new Log("log.dat");
        commitIndex = 0;
        lastApplied = 0;
    }

    @SneakyThrows
    public void start(){
        group = new NioEventLoopGroup();
        clientGroup = new NioEventLoopGroup(3);

        ChannelFuture cf = new ServerBootstrap()
            .group(group, clientGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new ServerChannelInitializer(this))
            .childHandler(new ServerClientChannelInitializer())
            .localAddress(id)
        .bind().sync();
    }

    public boolean alreadyVoted(){
        return votedFor != null;
    }

    public <EVENT> void trigger(EVENT event){
        channel.pipeline().fireUserEventTriggered(event);
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        promise.addListener(f -> {
            if(f.isSuccess()){
                channel = ctx.channel();
                peers = config.peers.stream().map(id -> new Peer(id, channel, clientGroup)).collect(Collectors.toList());
                peers.forEach(Peer::connect);
                state.transitionTo(FOLLOWER);
            }
        });
        super.bind(ctx, localAddress, promise);
    }

    @SneakyThrows
    public void stop(){
        if(group.isShutdown()) return;
        group.shutdownGracefully().sync();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Optional<?> invoked = Dynamic.invoke(this, "handle", evt);
        if(!invoked.isPresent()){
            ctx.fireUserEventTriggered(evt);
        }
    }

    public void handle(StateTransitionEvent event){
        event.newState().init();
    }

    public void scheduleElectionTimeout(){
        if(cancelElectionTimeOut()) {
            scheduledElectionTimeout = group.schedule(() -> trigger(new ElectionTimeoutEvent(lastheartbeat, id))
                    , config.electionTimeout.get(), TimeUnit.MILLISECONDS);
        }
    }

    public Long nextTimeout(){
        return config.electionTimeout.get();
    }

    private boolean cancelElectionTimeOut(){
        return scheduledElectionTimeout == null || (!scheduledElectionTimeout.isCancelled()
                && scheduledElectionTimeout.isCancellable() && scheduledElectionTimeout.cancel(false));
    }

    public long prevLogIndex(){
        return log.isEmpty() ? 0 : log.getLastIndex() - 1;
    }

    public long prevLogTerm(){
        return log.isEmpty() ? 0 : log.get(log.getLastIndex() - 1).getTerm();
    }

    public long nextHeartbeatTimeout() {
        return config.heartbeatTimeout.get();
    }
}
