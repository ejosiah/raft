package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.rpc.AppendEntries;
import com.josiahebhomenye.raft.rpc.RequestVote;
import com.josiahebhomenye.raft.rpc.RequestVoteReply;
import com.josiahebhomenye.raft.server.handlers.ProtocolInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class RemotePeerMock {

    private MessageHandler handler = this.new MessageHandler();

    public final InetSocketAddress address;
    public final InetSocketAddress nodeAddress;
    private EventLoopGroup group = new NioEventLoopGroup();
    private EventLoopGroup clientGroup = new NioEventLoopGroup();
    private Channel channel;
    private static final Logger logger = LoggerFactory.getLogger("RemotePeerMock");
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private List<ScheduledFuture<?>> scheduled = new ArrayList<>();
    public List<Object> receivedMessages = new ArrayList<>();

    @ChannelHandler.Sharable
    private class MessageHandler extends ChannelDuplexHandler {

        BiConsumer<ChannelHandlerContext, AppendEntries> onAppendEntries = (ctx, obj) -> {};
        BiConsumer<ChannelHandlerContext, RequestVote> onRequestVote = (ctx, obj) -> {};
        BiConsumer<ChannelHandlerContext, RequestVoteReply> onRequestVoteReply = (ctx, obj) -> {};

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            RemotePeerMock.logger.info("remote peer {} received connection from {}", RemotePeerMock.this.address, ctx.channel().remoteAddress());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            RemotePeerMock.this.receivedMessages.add(msg);
            if (msg instanceof AppendEntries) onAppendEntries.accept(ctx, (AppendEntries) msg);
            else if (msg instanceof RequestVote) onRequestVote.accept(ctx, (RequestVote) msg);
            else if (msg instanceof RequestVoteReply) onRequestVoteReply.accept(ctx, (RequestVoteReply) msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if(!(cause instanceof ConnectException)) ctx.fireExceptionCaught(cause);
        }
    }

    public void whenAppendEntriesThen(BiConsumer<ChannelHandlerContext, AppendEntries> action){
        handler.onAppendEntries = (ctx, entries) -> {
            logger.info("remote peer {} handling message {} from node {}", address, entries, ctx.channel().remoteAddress());
            action.accept(ctx, entries);
        };
    }

    public void whenRequestVote(BiConsumer<ChannelHandlerContext, RequestVote> action){
        handler.onRequestVote = (ctx, vote) -> {
            logger.info("remote peer {} handling message {} from node {}", address, vote, ctx.channel().remoteAddress());
            action.accept(ctx, vote);
        };
    }

    public void whenRequestVoteReply(BiConsumer<ChannelHandlerContext, RequestVoteReply> action){
        handler.onRequestVoteReply =  (ctx, reply) -> {
            logger.info("remote peer {} handling message {} from node {}", address, reply, ctx.channel().remoteAddress());
            action.accept(ctx, reply);
        };
    }

    public void verifyMessageReceived(Object msg){
        if(!receivedMessages.contains(msg)) throw new RuntimeException(String.format("mock[%s] did not receive message [%s]", address, msg));
    }

    public void reset(){
        handler.onAppendEntries = (ctx, obj) -> {};
        handler.onRequestVote = (ctx, obj) -> {};
        handler.onRequestVoteReply = (ctx, obj) -> {};
    }

    @SneakyThrows
    public void start(){
        startServer();
    }

    @SneakyThrows
    public void startServer(){
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
    public void startClient(){
        Bootstrap bootstrap = new Bootstrap();

        ChannelFuture cf =
                bootstrap
                    .channel(NioSocketChannel.class)
                    .group(clientGroup)
                    .handler(new ProtocolInitializer<Channel>(){
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            super.initChannel(ch);
                            ch.pipeline().addLast(handler);
                        }
                    }).connect(nodeAddress);

        channel = cf.sync().channel();
        logger.info("remote peer {} connected to Node {}", address, cf.channel().remoteAddress());
    }

    public void send(Object msg){
        send(msg, null);
    }

    public void send(Object msg, Long repeat){
        if(repeat != null){
            scheduled.add(
                scheduler.scheduleAtFixedRate(() -> {
                    channel.writeAndFlush(msg);
                  //  logger.info("sending message {} to server", msg);
                }, 0, repeat, TimeUnit.MILLISECONDS)
            );
        }else {
            channel.writeAndFlush(msg);
        }
    }

    @SneakyThrows
    public void stop(){
        scheduled.forEach(s -> s.cancel(true));
        group.shutdownGracefully().sync();
        clientGroup.shutdownGracefully().sync();
        logger.info("remote peer {} going offline", address);
        reset();
    }
}
