package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.server.core.Peer;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PeerChannelInitializer extends ProtocolInitializer<Channel> {

    private final Peer peer;

    @Override
    protected void initChannel(Channel ch) throws Exception {
        super.initChannel(ch);
        ch.pipeline()
          .addLast(peer.getConnectionHandler())
          .addLast(peer.getLogger());
    }
}
