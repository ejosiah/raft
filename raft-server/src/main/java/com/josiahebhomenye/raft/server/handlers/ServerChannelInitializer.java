package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.server.core.Node;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class ServerChannelInitializer extends ProtocolInitializer<NioServerSocketChannel> {

    private final Node node;

    @Override
    protected void initChannel(NioServerSocketChannel ch) throws Exception {
        super.initChannel(ch);

        ChannelPipeline pipeline = ch.pipeline();

        node.preProcessInterceptors().forEach(pipeline::addFirst);
        pipeline
            .addLast(Node.HANDLER_KEY, node)
            .addLast("state-manager", node.stateManager())
            .addLast(node.backgroundGroup(), "state-persistor", node.statePersistor())
            .addLast("logger", new ServerLogger(node));
        node.postProcessInterceptors().forEach(pipeline::addLast);
    }
}
