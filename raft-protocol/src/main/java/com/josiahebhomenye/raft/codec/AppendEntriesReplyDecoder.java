package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.rpc.AppendEntriesReply;

public class AppendEntriesReplyDecoder extends JsonDecoder<AppendEntriesReply> {
    public AppendEntriesReplyDecoder() {
        super(AppendEntriesReply.class);
    }
}
