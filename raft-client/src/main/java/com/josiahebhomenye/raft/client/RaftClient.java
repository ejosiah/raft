package com.josiahebhomenye.raft.client;

import com.josiahebhomenye.raft.client.config.ClientConfig;
import com.josiahebhomenye.raft.rpc.RedirectCommand;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@ChannelHandler.Sharable
public class RaftClient<ENTRY> extends ChannelDuplexHandler {

    private final ClientConfig config;
    private final Map<InetSocketAddress, Bootstrap> bootstraps;
    private ChannelPool pool;
    private final EventLoopGroup group;
    private final EntrySerializer<ENTRY> serializer;

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public RaftClient(ClientConfig config){
        this.config = config;
        this.group = new NioEventLoopGroup(config.nThreads);
        this.bootstraps = new HashMap<>();
        config.servers.forEach(address -> {
            bootstraps.put(address,
                new Bootstrap()
                    .channel(NioSocketChannel.class)
                    .group(group)
                    .handler(this)  // TODO use Channel initializer
                    .remoteAddress(address));
        });
        serializer = (EntrySerializer<ENTRY>) config.entrySerializerClass.newInstance();
    }

    public CompletableFuture<Void> send(ENTRY entry){
        CompletableFuture<Void> promise = new CompletableFuture<>();

        Future<Channel> fc = pool.acquire();

        fc.addListener(f -> {
            if(f.isSuccess()){
                fc.get().writeAndFlush(serializer.serialize(entry)).addListener(ff ->  {
                    if(ff.isSuccess()) {
                        promise.complete(null);
                    }else{
                        promise.completeExceptionally(ff.cause());
                    }
                });
            }else{
                promise.completeExceptionally(f.cause());
            }
        });

        return promise;
    }

    public void handle(RedirectCommand command){

    }

    public void start(){

    }

    public void stop(){

    }
}
