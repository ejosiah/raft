package com.josiahebhomenye.raft.client.support;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.codec.RedirectEncoder;
import com.josiahebhomenye.raft.codec.client.RequestDecoder;
import com.josiahebhomenye.raft.codec.client.ResponseEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Slf4j
@RequiredArgsConstructor
public class ServerMock {
    private MessageHandler handler = new MessageHandler();
    public final InetSocketAddress address;
    private EventLoopGroup group = new NioEventLoopGroup(1);
    public List<Object> receivedMessages = new ArrayList<>();

    @ChannelHandler.Sharable
    private class MessageHandler extends ChannelDuplexHandler{
        BiConsumer<ChannelHandlerContext, Request> onRequest = (ctx, obj) -> {};

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            log.info("remote server {} is online", ctx.channel().remoteAddress());
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            receivedMessages.add(msg);
            if(msg instanceof Request) {
                onRequest.accept(ctx, (Request) msg);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            super.userEventTriggered(ctx, evt);
        }
    }


    public void whenRequest(BiConsumer<ChannelHandlerContext, Request> thenAction){
        handler.onRequest = (ctx, msg) -> {
            log.info("processing request {} from client", msg);
            thenAction.accept(ctx, msg);
        };
    }

    public void reset(){
        handler.onRequest = (ctx, obj) -> {};
    }

    @SneakyThrows
    public void start(){
        new ServerBootstrap().channel(NioServerSocketChannel.class)
            .group(group)
            .childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline()
                        .addLast(new RequestDecoder())
                        .addLast(new ResponseEncoder())
                        .addLast(new RedirectEncoder())
                        .addLast(handler);
                }
            })
            .localAddress(address)
            .bind()
        .sync();
        log.info("server {} online", address);
    }

    @SneakyThrows
    public void stop(){
        group.shutdownGracefully().sync();
    }
}
