package com.josiahebhomenye.raft.server.core;


import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.Data;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.*;
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

import static com.josiahebhomenye.raft.server.core.NodeState.*;

@Getter
public class Node extends ChannelDuplexHandler {
    long currentTerm;
    long commitIndex;
    long lastApplied;
    InetSocketAddress votedFor;
    Log log;
    List<Peer> peers;
    Map<InetSocketAddress, Peer> activePeers;
    Channel channel;
    NodeState state;
    EventLoopGroup group;
    EventLoopGroup clientGroup;
    ServerConfig config;
    Instant lastHeartbeat;
    InetSocketAddress id;
    InetSocketAddress leaderId;
    int votes;
    ScheduledFuture<?> scheduledElectionTimeout;
    Data data = new Data(0);

    List<ChannelDuplexHandler> preProcessInterceptors = new ArrayList<>();
    List<ChannelDuplexHandler> postProcessInterceptors = new ArrayList<>();

    public Node(ServerConfig config){
        this.state = NULL_STATE();
        this.state.set(this);
        this.config = config;
        this.id = config.id;
        this.activePeers = new HashMap<>();
        this.peers = new ArrayList<>();
        votes = 0;
        group = new NioEventLoopGroup();
        clientGroup = new NioEventLoopGroup(3);
        initStateData();
    }

    void initStateData(){
        try(DataInputStream in = new DataInputStream(new FileInputStream(config.statePath))){
            currentTerm = in.readLong();
            votedFor = (in.available() > 0) ? new InetSocketAddress(in.readUTF(), in.readInt()) : null;
        }catch(Exception ex){
            currentTerm = 0;
            votedFor = null;
        }
        log = new Log(config.logPath);
        commitIndex = 0;
        lastApplied = 0;
    }

    @SneakyThrows
    public void start(){
        ChannelFuture cf = new ServerBootstrap()
            .group(group, clientGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new ServerChannelInitializer(this))
            .childHandler(new ServerClientChannelInitializer(this))
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
                peers = config.peers.stream().map(id -> new Peer(id, this, clientGroup)).collect(Collectors.toList());
                peers.forEach(Peer::connect);
                state.transitionTo(FOLLOWER());
            }
        });
        super.bind(ctx, localAddress, promise);
    }

    @SneakyThrows
    public void stop(){
        log.close();
        clientGroup.shutdownGracefully().sync();
        group.shutdownGracefully().sync();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Dynamic.invoke(this, "handle", evt);
        ctx.fireUserEventTriggered(evt);
    }

    public void handle(StateTransitionEvent event){
        state.init();
    }

    public void handle(AppendEntriesEvent event){
        state.handle(event);
    }

    public void handle(AppendEntriesReplyEvent event){
        state.handle(event);
    }

    public void handle(RequestVoteReplyEvent event){
        state.handle(event);
    }

    public void handle(RequestVoteEvent event){
        state.handle(event);
    }

    public void handle(ScheduleTimeoutEvent event){
        if(cancelElectionTimeOut()) {
            scheduledElectionTimeout = group.schedule(() -> trigger(new ElectionTimeoutEvent(lastHeartbeat, id))
                    , event.timeout(), TimeUnit.MILLISECONDS);
        }
    }

    public void handle(ElectionTimeoutEvent event){
        scheduledElectionTimeout = null;
        state.handle(event);
    }

    public void handle(ScheduleHeartbeatTimeoutEvent event){
          activePeers.values().forEach(peer -> peer.trigger(event));
    }

    public void handle(SendRequestVoteEvent event){
        activePeers.values().forEach(peer -> peer.send(event.requestVote()));
    }

    public void handle(HeartbeatTimeoutEvent event){
        state.handle(event);
    }

    public void handle(ReceivedCommandEvent event){
        state.handle(event);
    }

    public void handle(CommitEvent event){
        commitIndex = event.index();
        updateState();
    }

    public void handle(PeerDisconnectedEvent event) {
        activePeers.remove(event.peer().id);
        event.peer().trigger(new ConnectPeerEvent(event.peer()));
    }

    public void handle(ConnectPeerEvent event){
        event.peer().connect();
    }

    public void handle(CancelHeartbeatTimeoutEvent event){
        activePeers.values().forEach( peer -> peer.trigger(event));
    }

    public void updateState(){
        while(commitIndex > lastApplied){
            lastApplied++;
            Command command = log.get(lastApplied).getCommand();
            command.apply(data);
        }
    }

    public void handle(PeerConnectedEvent event){
        Peer peer = event.peer();
        peer.nextIndex = log.getLastIndex() + 1;
        peer.matchIndex = 0;
        activePeers.put(event.peer().id, event.peer());
        state.handle(event);
    }

    public Long nextTimeout(){
        return config.electionTimeout.get();
    }

    public boolean cancelElectionTimeOut(){
        return scheduledElectionTimeout == null || (!scheduledElectionTimeout.isCancelled()
                && scheduledElectionTimeout.isCancellable() && scheduledElectionTimeout.cancel(false));
    }

    public long prevLogIndex(){
        return log.isEmpty() ? 0 : log.getLastIndex() - 1;
    }

    public long prevLogTerm(){
        return log.isEmpty() || log.size() == 1 ? 0 : log.get(log.getLastIndex() - 1).getTerm();
    }

    public long nextHeartbeatTimeout() {
        return config.heartbeatTimeout.get();
    }

    public void addPreProcessInterceptors(ChannelDuplexHandler... interceptors){
        Arrays.stream(interceptors).filter(i -> i instanceof Interceptor).map(i -> (Interceptor)i).forEach(i -> i.node(this));
        preProcessInterceptors.addAll(Arrays.asList(interceptors));
    }

    public void addPostProcessInterceptors(ChannelDuplexHandler... interceptors){
        Arrays.stream(interceptors).filter(i -> i instanceof Interceptor).map(i -> (Interceptor)i).forEach(i -> i.node(this));
        postProcessInterceptors.addAll(Arrays.asList(interceptors));
    }

    public boolean receivedHeartbeatSinceLast(Instant heartbeat){
        return lastHeartbeat != null && heartbeat != null && heartbeat.isBefore(lastHeartbeat);
    }

    public void add(Command command) {
        log.add(new LogEntry(currentTerm, command), ++lastApplied);
    }

    public void replicate() {
        // TODO set a timer and then replicate

        long lastLogIndex = log.getLastIndex();
        activePeers.values().forEach(peer -> {
            long nextIndex = peer.nextIndex;
            if(lastLogIndex >= nextIndex){
                List<byte[]> entries = logEntriesFrom(nextIndex);
                peer.send(heartbeat(peer).withEntries(entries));
            }
        });
    }

    public AppendEntries heartbeat(Peer peer){
        long prevLogIndex = peer.nextIndex - 1;
        LogEntry previousEntry = log.get(prevLogIndex);
        prevLogIndex = previousEntry != null ? prevLogIndex: 0;
        long prevLogTerm = previousEntry != null ? previousEntry.getTerm() : 0;
        return AppendEntries.heartbeat(currentTerm, prevLogIndex, prevLogTerm, commitIndex, id);
    }
    
    protected void sendAppendEntriesTo(Peer peer, long index){
        List<byte[]> entries = logEntriesFrom(peer.nextIndex);
        LogEntry previousEntry = log.get(peer.nextIndex - 1);
        long prevLogIndex = previousEntry != null ? peer.nextIndex - 1 : 0;
        long prevLogTerm = previousEntry != null ? previousEntry.getTerm() : 0;
        AppendEntries msg = new AppendEntries(currentTerm, prevLogIndex, prevLogTerm, commitIndex, id, entries);
        peer.send(msg);
    }
    
    protected List<byte[]> logEntriesFrom(long index){
        return log.entriesFrom(index).stream().map(LogEntry::serialize).collect(Collectors.toList());
    }

    public class ChildHandler extends ChannelDuplexHandler{
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(Node.this.stopped()) return;
            if(msg instanceof AppendEntries){
                Node.this.trigger(new AppendEntriesEvent((AppendEntries)msg, ctx.channel()));
            }else if(msg instanceof RequestVote){
                Node.this.trigger(new RequestVoteEvent((RequestVote)msg, ctx.channel()));
            }
        }
    }

    private boolean stopped() {
        return group.isShutdown();
    }

    @Override
    public String toString() {
        return String.format("Node{id: %s, state: %s]", id, state);
    }
}
