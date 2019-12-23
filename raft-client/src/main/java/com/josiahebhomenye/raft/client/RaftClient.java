package com.josiahebhomenye.raft.client;

import com.josiahebhomenye.raft.client.codec.PromiseToRequestEncoder;
import com.josiahebhomenye.raft.client.config.ClientConfig;
import com.josiahebhomenye.raft.client.handlers.ClientLogger;
import com.josiahebhomenye.raft.client.handlers.ResponseMatcher;
import com.josiahebhomenye.raft.codec.RedirectDecoder;
import com.josiahebhomenye.raft.codec.client.RequestEncoder;
import com.josiahebhomenye.raft.codec.client.ResponseDecoder;
import com.josiahebhomenye.raft.rpc.Redirect;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.omg.SendingContext.RunTime;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.*;

@Accessors(fluent = true)
@ChannelHandler.Sharable
public class RaftClient<ENTRY> {

    private final ClientConfig config;
    private final Map<InetSocketAddress, Bootstrap> bootstrapMap;
    private final EventLoopGroup group;
    private final EntrySerializer<ENTRY> serializer;

    private ChannelPool pool;

    @Getter
    private Channel channel;

    private Iterator<Bootstrap> bootstrapItr;
    private final RequestEncoder requestEncoder;
    private final ResponseMatcher responseMatcher;
    private final PromiseToRequestEncoder promiseToRequestEncoder;
    private final ClientLogger clientLogger;
    private final CountDownLatch startLatch;
    private CompletableFuture<Void> stopPromise;

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public RaftClient(ClientConfig config){
        this.config = config;
        this.group = new NioEventLoopGroup(config.nThreads);
        serializer = (EntrySerializer<ENTRY>) config.entrySerializerClass.newInstance();
        requestEncoder = new RequestEncoder();
        responseMatcher = new ResponseMatcher(config.requestTimeout);
        promiseToRequestEncoder = new PromiseToRequestEncoder();
        clientLogger = new ClientLogger();
        this.startLatch = new CountDownLatch(1);
        this.stopPromise = null;

        this.bootstrapMap = new HashMap<>();
        config.servers.forEach(address -> {
            bootstrapMap.put(address,
                new Bootstrap()
                    .channel(NioSocketChannel.class)
                    .group(group)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch){
                            ch.pipeline()
                                .addLast(new RedirectDecoder())
                                .addLast(new ResponseDecoder())
                                .addLast(requestEncoder)
                                .addLast(promiseToRequestEncoder)
                                .addLast(responseMatcher)
                                .addLast(new Handler())
                                .addLast(clientLogger);
                        }
                    })
                    .remoteAddress(address));
        });
        bootstrapItr = bootstrapMap.values().iterator();
    }

    public CompletableFuture<Response> send(ENTRY entry){
        CompletableFuture<Response> promise = new CompletableFuture<>();

        if (channel != null && channel.isActive()) {
            ByteBuf data = Unpooled.copiedBuffer(serializer.serialize(entry));
            channel.writeAndFlush(new PromiseRequest(promise, UUID.randomUUID().toString(), data));
        }else {
            promise.completeExceptionally(new ClosedChannelException());
        }

        return promise;
    }

    public CompletableFuture<Response> send(List<ENTRY> entry){
        return null;
    }

    public void handle(Redirect redirect){
        Bootstrap bootstrap = bootstrapMap.get(redirect.getLeaderId());

       ChannelFuture cf = bootstrap.connect();

       cf.addListener(f -> {
           if(f.isSuccess()){
               Channel oldChannel = channel;
               if(oldChannel != null) {
                   oldChannel.close();
               }
               channel = cf.channel();
               channel.writeAndFlush(redirect.getRequest());
           }else{
               String id = redirect.getRequest().getId();
                channel.pipeline().fireUserEventTriggered(new RejectRequestEvent(id, new ClosedChannelException()));
           }
       });
    }

    @SneakyThrows
    public void tryStart(){
        if (!group.isShutdown() && !group.isShutdown()) {
            if (!bootstrapItr.hasNext()) {
                bootstrapItr = bootstrapMap.values().iterator();
            }
            Bootstrap bootstrap = bootstrapItr.next();
            ChannelFuture cf = bootstrap.connect();

            cf.addListener(f -> {
                if(f.isSuccess()){
                    channel = cf.channel();
                    startLatch.countDown();
                }else{
                    tryStart();
                }
            });
        }
    }

    @SneakyThrows
    public void start(){
        tryStart();
        startLatch.await(30, TimeUnit.SECONDS); // Change to future
    }

    public void start(Bootstrap bootstrap){
        pool = new FixedChannelPool(bootstrap, NO_OP_POOL_HANDLER, config.maxConnections, config.maxPendingAcquires);
    }

    @SneakyThrows
    public CompletableFuture<Void> stop(){
        if(stopPromise == null) {
            stopPromise = new CompletableFuture<>();
            if(channel != null) {
                group.shutdownGracefully().addListener(f -> {
                    if (f.isSuccess()) {
                        stopPromise.complete(null);
                    } else {
                        stopPromise.completeExceptionally(f.cause());
                    }
                });
            }else {
                stopPromise.complete(null);
            }
        }
        return stopPromise;
    }

    @ChannelHandler.Sharable
    private class Handler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(msg instanceof Redirect){
                handle((Redirect)msg);
            }
            ctx.fireChannelRead(msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if(stopPromise == null) {
                if(channel != null && channel.equals(ctx.channel())){
                    channel.close().addListener(f -> {
                        if(f.isSuccess()){
                            channel = null;
                            tryStart();
                        }
                    });
                }else{
                    tryStart();
                }
            }
            ctx.fireChannelInactive();
        }


        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            super.channelUnregistered(ctx);
        }
    }

    private static final ChannelPoolHandler NO_OP_POOL_HANDLER = new AbstractChannelPoolHandler() {
        @Override
        public void channelCreated(Channel ch) { }
    };
}
