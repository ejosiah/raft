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
import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ChannelHandler.Sharable
public class RaftClient<ENTRY> extends ChannelDuplexHandler {

    private final ClientConfig config;
    private final Map<InetSocketAddress, Bootstrap> bootstrapMap;
    private final EventLoopGroup group;
    private final EntrySerializer<ENTRY> serializer;

    private ChannelPool pool;
    private Channel channel;
    private Iterator<Bootstrap> bootstrapItr;
    private final RequestEncoder requestEncoder;
    private final ResponseMatcher responseMatcher;
    private final PromiseToRequestEncoder promiseToRequestEncoder;
    private final ClientLogger clientLogger;
    private final CountDownLatch startLatch;

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

//        Future<Channel> fc = pool.acquire();
//
//        fc.addListener(f -> {
//            if(f.isSuccess()){
//                ByteBuf data = Unpooled.copiedBuffer(serializer.serialize(entry));
//                fc.get().write(new PromiseRequest(promise, UUID.randomUUID().toString(), data));
//            }else{
//                promise.completeExceptionally(f.cause());
//            }
//        });
        ByteBuf data = Unpooled.copiedBuffer(serializer.serialize(entry));
        channel.writeAndFlush(new PromiseRequest(promise, UUID.randomUUID().toString(), data));

        return promise;
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        promise.addListener(f -> {
            if(f.isSuccess()){
                channel = ctx.channel();
            }
        });
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }

    public void handle(Redirect redirect){
        Bootstrap bootstrap = bootstrapMap.get(redirect.getLeaderId());

       ChannelFuture cf = bootstrap.connect();

       cf.addListener(f -> {
           if(f.isSuccess()){
               channel.close();
               channel = cf.channel();
               channel.writeAndFlush(redirect.getRequest());
           }
       });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
    }

    @SneakyThrows
    public void tryStart(){
        if (!bootstrapItr.hasNext()) {
            bootstrapItr = bootstrapMap.values().iterator();
        }
        if(config.poolingEnabled) {
            if (pool != null) {
                pool.close();
            }
            start(bootstrapItr.next());
        }else{
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
        startLatch.await(30, TimeUnit.SECONDS);
    }

    public void start(Bootstrap bootstrap){
        pool = new FixedChannelPool(bootstrap, NO_OP_POOL_HANDLER, config.maxConnections, config.maxPendingAcquires);
    }

    @SneakyThrows
    public void stop(){
        group.shutdownGracefully().sync();
    }

    @Sharable
    private class Handler extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if(msg instanceof Redirect){
                handle((Redirect)msg);
            }else {
                ctx.fireChannelRead(msg);
            }
        }
    }

    private static final ChannelPoolHandler NO_OP_POOL_HANDLER = new AbstractChannelPoolHandler() {
        @Override
        public void channelCreated(Channel ch) throws Exception {

        }
    };
}
