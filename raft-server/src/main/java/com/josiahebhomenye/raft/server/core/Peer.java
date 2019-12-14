package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntriesReply;
import com.josiahebhomenye.raft.RequestVoteReply;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.AppendEntriesReplyEvent;
import com.josiahebhomenye.raft.server.event.PeerConnectedEvent;
import com.josiahebhomenye.raft.server.event.RequestVoteReplyEvent;
import com.josiahebhomenye.raft.server.handlers.PeerChannelInitializer;
import com.josiahebhomenye.raft.server.handlers.PeerLogger;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Getter
@RequiredArgsConstructor
public class Peer {
    long nextIndex;
    long matchIndex;
    Channel channel;

    final InetSocketAddress id;
    final Channel serverChannel;
    final EventLoopGroup group;

    final ConnectionHandler connectionHandler = new ConnectionHandler();
    final PeerLogger logger = new PeerLogger();

    public void connect(){
        Bootstrap bootstrap = new Bootstrap();
        ChannelFuture future = bootstrap
             .group(group)
             .channel(NioSocketChannel.class)
             .handler(new PeerChannelInitializer(this))
        .connect(id);

        future.addListener(f -> {
            if(!f.isSuccess()){
                connect();
            }
        });
    }

    public void send(Object msg){
        channel.writeAndFlush(msg); // TODO set timeout and retry if no reply
    }

    @ChannelHandler.Sharable
    public class ConnectionHandler extends ChannelDuplexHandler {

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            promise.addListener(f ->{
                if(f.isSuccess()){
                    Peer.this.channel = ctx.channel();
                    serverChannel.pipeline().fireUserEventTriggered(new PeerConnectedEvent(Peer.this));  // TODO trigger from node instead
                }
            });
            super.connect(ctx, remoteAddress, localAddress, promise);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(msg instanceof RequestVoteReply){
                serverChannel.pipeline().fireUserEventTriggered(new RequestVoteReplyEvent((RequestVoteReply)msg, ctx.channel()));
            }else if(msg instanceof AppendEntriesReply){
                serverChannel.pipeline().fireUserEventTriggered(new AppendEntriesReplyEvent((AppendEntriesReply)msg, Peer.this));
            }
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            super.disconnect(ctx, promise); // TODO fire Channel Disconnected event and try to restart peer
        }
    }

    public Peer set(Channel channel){
        this.channel = channel;
        return this;
    }

    @Override
    public String toString() {
        return String.format("Peer[%s]", id);
    }
}