package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.RequestVoteReply;
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
        channel.writeAndFlush(msg);
    }

    @ChannelHandler.Sharable
    public class ConnectionHandler extends ChannelDuplexHandler {

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            promise.addListener(f ->{
                if(f.isSuccess()){
                    Peer.this.channel = ctx.channel();
                    serverChannel.pipeline().fireUserEventTriggered(new PeerConnectedEvent(Peer.this));
                }
            });
            super.connect(ctx, remoteAddress, localAddress, promise);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(msg instanceof RequestVoteReply){
                serverChannel.pipeline().fireUserEventTriggered(new RequestVoteReplyEvent((RequestVoteReply)msg, ctx.channel()));
            }
        }
    }

    @Override
    public String toString() {
        return String.format("Peer[%s]", id);
    }
}