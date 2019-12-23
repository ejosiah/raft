package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.rpc.AppendEntriesReply;
import com.josiahebhomenye.raft.rpc.RequestVoteReply;
import com.josiahebhomenye.raft.server.event.*;
import com.josiahebhomenye.raft.server.handlers.PeerChannelInitializer;
import com.josiahebhomenye.raft.server.handlers.PeerLogger;
import com.josiahebhomenye.raft.server.util.Dynamic;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@With
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
@AllArgsConstructor
public class Peer implements Cloneable  {
    long nextIndex;
    long matchIndex;
    Channel channel;

    final InetSocketAddress id;
    final Node node;
    final EventLoopGroup group;

    final ConnectionHandler connectionHandler = new ConnectionHandler();
    final PeerLogger logger = new PeerLogger(this);
    private boolean stopping;

    public void connect(){
        if(!stopped() || stopping) {
            Bootstrap bootstrap = new Bootstrap();
            ChannelFuture future = bootstrap
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new PeerChannelInitializer(this))
                    .connect(id);

            future.addListener(f -> {
                if (!f.isSuccess()) {
                    connect();
                }
            });
        }
    }

    public boolean stopped(){
        return group == null ||group.isShutdown();
    }


    public void send(Object msg){
        channel.writeAndFlush(msg); // TODO set timeout and retry if no reply
    }

    public <EVENT> void trigger(EVENT event) {
        channel.pipeline().fireUserEventTriggered(event);
    }

    public void handle(ScheduleHeartbeatTimeoutEvent event){
        channel.pipeline().addFirst(new IdleStateHandler(0, event.timeout(), 0, event.unit()));
    }

    public void handle(CancelHeartbeatTimeoutEvent event){
        try {
            channel.pipeline().remove(IdleStateHandler.class);
        }catch (Exception ex){
            // no IdleStateHandler defined
        }
    }

    public void handle(IdleStateEvent event){
        if(event.state() == IdleState.WRITER_IDLE){
            node.trigger(new HeartbeatTimeoutEvent(this));
        }
    }

    public void handle(StopEvent event){
        stopping = true;
        handle(new CancelHeartbeatTimeoutEvent(channel));
        channel.close();
    }

    @ChannelHandler.Sharable
    public class ConnectionHandler extends ChannelDuplexHandler {

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            promise.addListener(f ->{
                if(f.isSuccess()){
                    Peer.this.channel = ctx.channel();
                    node.trigger(new PeerConnectedEvent(Peer.this));  // TODO trigger from node instead
                }
            });
            super.connect(ctx, remoteAddress, localAddress, promise);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(msg instanceof RequestVoteReply){
                node.trigger(new RequestVoteReplyEvent((RequestVoteReply)msg, ctx.channel()));
            }else if(msg instanceof AppendEntriesReply){
                node.trigger(new AppendEntriesReplyEvent((AppendEntriesReply)msg, Peer.this));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if(!stopped()) {
                Dynamic.invoke(Peer.this, "handle", evt);
                ctx.fireUserEventTriggered(evt);
            }
        }


        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            if(!stopping) {
                node.trigger(new PeerDisconnectedEvent(Peer.this));
            }
            super.disconnect(ctx, promise);
        }
    }

    public Peer set(Channel channel){
        this.channel = channel;
        return this;
    }

    @Override
    protected Peer clone() {
        return new Peer(id, node, group)
                        .withMatchIndex(matchIndex)
                        .withNextIndex(nextIndex)
                        .withChannel(channel)
                        .withStopping(stopping);
    }

    @Override
    public String toString() {
        return String.format("Peer[%s:%s]", node.id, id);
    }
}