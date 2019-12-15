package com.josiahebhomenye.test.support;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class UserEventCapture extends ChannelDuplexHandler {

    private List<Object> events = new ArrayList<>();
    private List<Class<?>> ignoreList = new ArrayList<>();

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if(!ignore(evt)) {
            events.add(evt);
        }
        ctx.fireUserEventTriggered(evt);
    }

    private boolean ignore(Object event){
        return ignoreList.contains(event.getClass());
    }

    public void ignore(Class<?>... classes){
        ignoreList.addAll(Arrays.asList(classes));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int index){
        return (T) (events.size() > index ? events.get(index) : null);
    }

    public void clear(){
        events.clear();
    }

    public int captured(){
        return events.size();
    }

    @SuppressWarnings("unchecked")
    public  <T> Optional<T> get(Class<T> eventClass) {
        return events.stream().filter(e -> e.getClass() == eventClass).findFirst().map(e -> (T)e);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> eventClass, int pos) {
        if(events.size() <= pos) return null;
        Object event = events.get(pos);
        return event.getClass() == eventClass ? (T)event : null;
    }
}
