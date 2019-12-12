package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.server.core.Node;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerChannelInitializer extends ProtocolInitializer<NioServerSocketChannel> {

    private final Node node;

    @Override
    protected void initChannel(NioServerSocketChannel ch) throws Exception {
        super.initChannel(ch);

        ChannelPipeline pipeline = ch.pipeline();

        node.getPreProcessInterceptors().forEach(pipeline::addLast);
        pipeline
            .addLast(node)
            .addLast(new ServerLogger(node));
        node.getPostProcessInterceptors().forEach(pipeline::addLast);
    }
}
