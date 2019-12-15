package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.rpc.AppendEntries;

public class AppendEntriesDecoder extends JsonDecoder<AppendEntries>{


    public AppendEntriesDecoder() {
        super(AppendEntries.class);
    }

}
