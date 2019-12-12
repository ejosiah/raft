package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.server.handlers.ProtocolInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class RemotePeerMock {

    private MessageHandler handler = this.new MessageHandler();

    private final InetSocketAddress address;
    private EventLoopGroup group = new NioEventLoopGroup();
    private static final Logger logger = LoggerFactory.getLogger("RemotePeerMock");

    private class MessageHandler extends ChannelInboundHandlerAdapter {

        BiConsumer<ChannelHandlerContext, AppendEntries> onAppendEntries = (ctx, obj) -> {};
        BiConsumer<ChannelHandlerContext, RequestVote> onRequestVote = (ctx, obj) -> {};

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            RemotePeerMock.logger.info("remote peer {} received connection from {}", RemotePeerMock.this.address, ctx.channel().remoteAddress());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            RemotePeerMock.logger.info("remote peer {} handling message {} from node {}", address, msg, ctx.channel().remoteAddress());
            if(msg instanceof  AppendEntries) onAppendEntries.accept(ctx, (AppendEntries)msg);
            else if (msg instanceof RequestVote) onRequestVote.accept(ctx, (RequestVote)msg);
        }
    }

    public void whenAppendEntriesThen(BiConsumer<ChannelHandlerContext, AppendEntries> action){
        handler.onAppendEntries = action;
    }

    public void whenRequestVote(BiConsumer<ChannelHandlerContext, RequestVote> action){
        handler.onRequestVote = action;
    }

    @SneakyThrows
    public void start(){
        ServerBootstrap bootstrap = new ServerBootstrap();

        ChannelFuture cf = bootstrap
            .channel(NioServerSocketChannel.class)
            .group(group)
            .childHandler(new ProtocolInitializer<Channel>(){
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    super.initChannel(ch);
                    ch.pipeline().addLast(handler);
                }
            })
            .localAddress(address)
        .bind().sync();

        logger.info("remote peer started on {}", cf.channel().localAddress());
    }

    @SneakyThrows
    public void stop(){
        group.shutdownGracefully().sync();
        logger.info("remote peer {} going offline", address);
    }
}
