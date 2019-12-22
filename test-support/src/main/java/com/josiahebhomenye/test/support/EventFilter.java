package com.josiahebhomenye.test.support;

import com.josiahebhomenye.raft.event.Event;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import com.josiahebhomenye.raft.server.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ChannelHandler.Sharable
public class EventFilter extends ChannelDuplexHandler {

    public static final EventFilter BLOACK_TIMEOUT_EVENTS_FILTER = new EventFilter(
            ScheduleTimeoutEvent.class,
            ScheduleHeartbeatTimeoutEvent.class,
            ElectionTimeoutEvent.class,
            HeartbeatTimeoutEvent.class
    );

    private final List<Class<? extends Event>>  blockList = new ArrayList<>();

    public EventFilter(Class<? extends Event>... eventsClasses){
        block(eventsClasses);
    }

    public void block(Class<? extends Event>... eventsClasses){
        blockList.addAll(Arrays.asList(eventsClasses));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(!blockList.contains(evt.getClass())){
            ctx.fireUserEventTriggered(evt);
        }
    }
}
