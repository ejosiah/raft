package com.josiahebhomenye.raft.client.handlers;

import com.josiahebhomenye.raft.client.PromiseRequest;
import com.josiahebhomenye.raft.client.Response;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class ResponseMatcher extends ChannelDuplexHandler {

    private static final String TIMEOUT_MESSAGE = "did not receive response from server after %s ms";

    private final long requestTimeout;
    private Map<String, CompletableFuture> pending = new HashMap<>();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof PromiseRequest){
            PromiseRequest req = (PromiseRequest)msg;
            pending.put(req.getId(), req.getPromise());
            ctx.channel().eventLoop().schedule(() -> {
                ctx.pipeline().fireUserEventTriggered(new RequestTimeoutEvent(req.getId()));
            }, requestTimeout, TimeUnit.MILLISECONDS);
        }
        super.write(ctx, msg, promise);
    }


    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof Response){
            Response resp = (Response)msg;
            if(pending.containsKey(resp.getCorrelationId())){
                pending.remove(resp.getCorrelationId()).complete(resp);
            }else{
                ctx.fireChannelRead(msg);
            }
        }else{
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof RequestTimeoutEvent){
            RequestTimeoutEvent event = (RequestTimeoutEvent)evt;
            if(pending.containsKey(event.id)){
                pending.remove(event.id) .completeExceptionally(new TimeoutException(String.format(TIMEOUT_MESSAGE, requestTimeout)));
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    @RequiredArgsConstructor
    public static class RequestTimeoutEvent{
        private final String id;
    }
}
