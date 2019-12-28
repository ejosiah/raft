package com.josiahebhomenye.raft.server.core;


import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.event.InboundEvent;
import com.josiahebhomenye.raft.rpc.AppendEntries;
import com.josiahebhomenye.raft.rpc.AppendEntriesReply;
import com.josiahebhomenye.raft.rpc.RequestVote;
import com.josiahebhomenye.raft.StateManager;
import com.josiahebhomenye.raft.event.ApplyEntryEvent;
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
import lombok.experimental.Accessors;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import static com.josiahebhomenye.raft.server.core.NodeState.Id.*;
import static com.josiahebhomenye.raft.server.core.NodeState.NULL_STATE;

@Getter
@Accessors(fluent = true)
@ChannelHandler.Sharable
public class Node extends ChannelDuplexHandler {

    public static final String HANDLER_KEY = "NODE";

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
    EventLoopGroup backgroundGroup;
    ServerConfig config;
    Instant lastHeartbeat;
    InetSocketAddress id;

    InetSocketAddress leaderId;
    int votes;
    ScheduledFuture<?> scheduledElectionTimeout;
    StateManager<?, ?> stateManager;
    StatePersistor statePersistor;

    List<ChannelDuplexHandler> preProcessInterceptors = new ArrayList<>();
    List<ChannelDuplexHandler> postProcessInterceptors = new ArrayList<>();

    CompletableFuture<Void> stopPromise;
    boolean stopping;
    Channel sender;


    @SneakyThrows
    public Node(ServerConfig config){
        initialize(config);
    }

    void initialize(ServerConfig config){
        this.state = states.get(NOTHING);
        this.config = config;
        this.id = config.id;
        this.activePeers = new HashMap<>();
        this.peers = new ArrayList<>();
        this.votes = 0;
        this.group = new NioEventLoopGroup(1);   // TODO read from config
        this.clientGroup = new NioEventLoopGroup(3); // TODO read from config
        this.backgroundGroup = new DefaultEventLoopGroup(1); // TODO read from config
        this.statePersistor = new StatePersistor();
        this.stopPromise = null;
        this.stopping = false;
        this.lastHeartbeat = Instant.EPOCH;
        initStateManager();
        initStateData();
    }

    private void initStateData(){
        try(DataInputStream in = new DataInputStream(new FileInputStream(config.statePath))){
            currentTerm = in.readLong();
            votedFor = (in.available() > 0) ? new InetSocketAddress(in.readUTF(), in.readInt()) : null;
        }catch(Exception ex){
            currentTerm = 0;
            votedFor = null;
        }
        log = new Log(config.logPath, stateManager.entryDeserializer().entrySize());
        commitIndex = 0;
        lastApplied = 0;
    }

    @SneakyThrows
    private void initStateManager(){
        stateManager = config.stateMgrClass.newInstance();
        if(config.deserializer != null){
            stateManager.entryEntryDeserializer(config.deserializer.newInstance());
        }
    }

    @SneakyThrows
    public void start(){
        ChannelFuture cf = new ServerBootstrap()
            .group(group, clientGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new ServerChannelInitializer(this))
            .childHandler(new ServerClientChannelInitializer(this))
            .localAddress(id)
        .bind().sync(); // TODO change to future
    }

    public String name(){
        return String.format("%s:%s", id.getHostName(), id.getPort());
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
                ctx.pipeline().fireUserEventTriggered(new BindEvent(ctx.channel()));
                peers = config.peers.stream().map(id -> new Peer(id, this, clientGroup)).collect(Collectors.toList());
                peers.forEach(Peer::connect);
                state.transitionTo(FOLLOWER);
            }
        });
        super.bind(ctx, localAddress, promise);
    }

    @SneakyThrows
    public CompletableFuture<Void> stop(){
        if(stopPromise == null) {
            stopPromise = new CompletableFuture<>();
            trigger(new StopEvent(channel));
        }
        return stopPromise;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(!stopping) {
            if(evt instanceof InboundEvent){
                sender = ((InboundEvent)evt).sender();
            }
            Dynamic.invoke(this, "handle", evt);
            ctx.fireUserEventTriggered(evt);
        }
    }

    public void handle(BindEvent event){
        channel = event.channel();
    }

    public void handle(StateTransitionEvent event){
        event.newState().set(this);
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
        cancelElectionTimeOut();
        Instant prevLastHeartbeat = this.lastHeartbeat;
        scheduledElectionTimeout = group.schedule(() -> trigger(new ElectionTimeoutEvent(prevLastHeartbeat, channel))
                , event.timeout(), event.unit());
    }

    public void handle(ElectionTimeoutEvent event){
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

    public void handle(ReceivedRequestEvent event){
        state.handle(event);
    }

    public void handle(CommitEvent event){
        state.handle(event);
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

    public void applyLogEntries(){
        while(commitIndex > lastApplied){   // TODO apply the log entries in node.handle(ApplyEntryEvent)
            lastApplied++;
            LogEntry entry = log.get(lastApplied);
            trigger(new ApplyEntryEvent(lastApplied, entry, channel));
        }
    }

    public void handle(PeerConnectedEvent event){
        Peer peer = event.peer();
        peer.nextIndex = log.getLastIndex() + 1;
        activePeers.put(event.peer().id, peer);
        state.handle(event);
    }

    public void handle(StopEvent event){
        stopping = true;
        cancelElectionTimeOut();
        broadcast(event);
        activePeers.clear();
        log.close();
        shutdown();

    }

    private void shutdown(){
        clientGroup.shutdownGracefully().addListener(f -> {
            if (f.isSuccess()) {
                group.shutdownGracefully().addListener(ff -> {
                    if(ff.isSuccess()){
                        stopPromise.complete(null);
                    }else{
                        stopPromise.completeExceptionally(ff.cause());
                    }
                });
            } else {
                stopPromise.completeExceptionally(f.cause());
            }
        });
    }

    public void broadcast(Event event){
        activePeers.values().forEach(peer -> {
            try{
                peer.trigger(event);
            }catch (Exception e){
                channel.pipeline().fireExceptionCaught(new Exception(String.format("error sending %s to %s", event, peer)));
            }
        });
    }

    public Long nextTimeout(){
        return config.electionTimeout.get();
    }

    public void cancelElectionTimeOut(){
        if(scheduledElectionTimeout != null){
            scheduledElectionTimeout.cancel(true);
            scheduledElectionTimeout = null;
        }
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
        addPreProcessInterceptors(Arrays.asList(interceptors));
    }

    public void addPreProcessInterceptors(List<? extends ChannelDuplexHandler> interceptors){
        preProcessInterceptors.addAll(interceptors);
    }

    public void addPostProcessInterceptors(ChannelDuplexHandler... interceptors){
        addPostProcessInterceptors(Arrays.asList(interceptors));
    }

    public void addPostProcessInterceptors(List<? extends ChannelDuplexHandler> interceptors){
        postProcessInterceptors.addAll(interceptors);
    }

    public boolean receivedHeartbeatSinceLast(Instant heartbeat){
        Instant prevLastHeartbeat = ensureValue(heartbeat);
        Instant curLastHeartbeat = ensureValue(lastHeartbeat);
        return prevLastHeartbeat.isBefore(curLastHeartbeat);
    }

    private Instant ensureValue(Instant instant){
        return instant == null ? Instant.EPOCH : instant;
    }

    public void add(byte[] entry) {
        log.add(new LogEntry(currentTerm, entry));
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
    
    protected void sendAppendEntriesTo(Peer peer){
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

    public boolean stopping(){
        return stopPromise != null && !stopPromise.isDone();
    }

    public boolean stopped() {
        return stopPromise != null && stopPromise.isDone();
    }

    public void restart() {
        initialize(config);
        start();
    }

    @Override
    public String toString() {
        return String.format("Node{id: %s, state: %s, term: %s commitIndex: %s, offline: %b]"
                , id, state, currentTerm, commitIndex, stopped() );
    }

    public boolean isFollower(){
        return state.isFollower();
    }

    public boolean isCandidate(){
        return state.isCandidate();
    }

    public boolean isLeader() {
        return state.isLeader();
    }

    @Sharable
    public class ChildHandler extends ChannelDuplexHandler{
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if(Node.this.stopped()) return;
            if(msg instanceof AppendEntries){
                trigger(new AppendEntriesEvent((AppendEntries)msg, ctx.channel()));
            }else if(msg instanceof RequestVote){
                trigger(new RequestVoteEvent((RequestVote)msg, ctx.channel()));
            }else if(msg instanceof Request){
                trigger(new ReceivedRequestEvent((Request)msg, ctx.channel()));
            }
            else {
                ctx.fireUserEventTriggered(new UnhandledMessageEvent(msg, ctx.channel()));
            }
        }
    }

    @Sharable
    private class StatePersistor extends ChannelInboundHandlerAdapter{

        private InetSocketAddress prevVotedFor;
        private long prevTerm;

        public class StatePersistException extends RuntimeException{
            public StatePersistException(Throwable cause){
                super("unable to persist node state", cause);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if(shouldPersist(evt)){
                prevTerm = currentTerm;
                prevVotedFor = votedFor;
                try(DataOutputStream out = new DataOutputStream(new FileOutputStream(config.statePath))){
                    out.writeLong(currentTerm);
                    if(votedFor != null) {
                        out.writeUTF(votedFor.getHostName());
                        out.writeInt(votedFor.getPort());
                    }
                }catch (Exception ex){
                    channel.pipeline().fireExceptionCaught(new StatePersistException(ex));
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        private boolean shouldPersist(Object evt){
            if(currentTerm == 0 || votedFor == null) return false;
            if(prevTerm == currentTerm && votedFor.equals(prevVotedFor)) return false;
            if(evt instanceof StateTransitionEvent){
                StateTransitionEvent event = (StateTransitionEvent)evt;
                if(event.newState().equals(CANDIDATE)) return true;
            }
            if(evt instanceof AppendEntriesEvent) return true;
            if(evt instanceof RequestVoteEvent) return true;
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return currentTerm == node.currentTerm &&
                (votedFor != null &&votedFor.equals(node.votedFor)) &&
                log.equals(node.log) &&
                id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentTerm, votedFor, log, id);
    }

    public NodeState get(NodeState.Id stateId){
        return states.get(stateId);
    }

    Map<NodeState.Id, NodeState> states = new HashMap<>();

    {
        states.put(NOTHING, new NodeState.NullState().set(this));
        states.put(FOLLOWER, new Follower().set(this));
        states.put(CANDIDATE, new Candidate().set(this));
        states.put(LEADER, new Leader().set(this));
    }

}
