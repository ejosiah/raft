package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.client.EntryDeserializer;
import com.josiahebhomenye.raft.event.StateUpdatedEvent;
import com.josiahebhomenye.raft.event.ApplyEntryEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(fluent = true)
public abstract class StateManager<ENTRY, STATE> extends ChannelDuplexHandler{

    protected STATE state;

    @Getter
    protected EntryDeserializer<ENTRY> entryDeserializer;

    public StateManager(EntryDeserializer<ENTRY> entryDeserializer){
        this(entryDeserializer, null);
    }

    public StateManager(EntryDeserializer<ENTRY> entryDeserializer, STATE state) {
        this.entryDeserializer = entryDeserializer;
        this.state = state;
    }

    @SuppressWarnings("unchecked")
    public void entryEntryDeserializer(EntryDeserializer<?> entryDeserializer){
        this.entryDeserializer = (EntryDeserializer<ENTRY> )entryDeserializer;
    }

    abstract STATE handle(ApplyEntryEvent event);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if(evt instanceof ApplyEntryEvent){
            STATE state = handle((ApplyEntryEvent)evt);
            ctx.pipeline().fireUserEventTriggered(new StateUpdatedEvent(state, ctx.channel()));
        }
        ctx.fireUserEventTriggered(evt);
    }
}
