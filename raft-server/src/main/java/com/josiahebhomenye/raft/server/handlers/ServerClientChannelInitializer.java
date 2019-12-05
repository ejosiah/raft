package com.josiahebhomenye.raft.server.handlers;

import io.netty.channel.socket.nio.NioSocketChannel;

public class ServerClientChannelInitializer extends ProtocolInitializer<NioSocketChannel> {

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        super.initChannel(ch);
        ch.pipeline()
          .addLast(new ServerClientLogger());
    }
}
