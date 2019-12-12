package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.server.core.Node;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerClientChannelInitializer extends ProtocolInitializer<NioSocketChannel> {

    final Node node;

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        super.initChannel(ch);
        ch.pipeline()
          .addLast(node.new ChildHandler())
          .addLast(new ServerClientLogger());
    }
}
