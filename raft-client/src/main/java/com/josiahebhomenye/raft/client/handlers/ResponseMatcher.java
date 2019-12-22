package com.josiahebhomenye.raft.client.handlers;

import com.josiahebhomenye.raft.client.PromiseRequest;
import com.josiahebhomenye.raft.client.RejectRequestEvent;
import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.client.Response;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class ResponseMatcher extends ChannelDuplexHandler {

    private static final String TIMEOUT_MESSAGE = "did not receive response from server after %s ms";

    private final long requestTimeout;
    private Map<String, CompletableFuture> pending = new ConcurrentHashMap<>();

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof PromiseRequest){
            PromiseRequest req = (PromiseRequest)msg;

            pending.put(req.getId(), req.getPromise());

            ctx.channel().eventLoop().schedule(() -> {
                ctx.pipeline().fireUserEventTriggered(new RequestTimeoutEvent(req.getId()));
            }, requestTimeout, TimeUnit.MILLISECONDS);
        }else if(msg instanceof Request){
            ((Request) msg).getBody();
        }
        super.write(ctx, msg, promise);
    }


    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof Response){
            Response resp = (Response)msg;
            findAndRemove(resp.getCorrelationId()).ifPresent( promise -> {
                    promise.complete(resp);
            });
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof RequestTimeoutEvent){
            RequestTimeoutEvent event = (RequestTimeoutEvent)evt;
            findAndRemove(event.id).ifPresent(promise -> {
                if(!promise.isDone()) {
                    promise.completeExceptionally(new TimeoutException(String.format(TIMEOUT_MESSAGE, requestTimeout)));
                }
            });
        }else if(evt instanceof RejectRequestEvent){
            RejectRequestEvent event = (RejectRequestEvent)evt;
            findAndRemove(event.id()).ifPresent(promise -> {
                promise.completeExceptionally(event.reason());
            });
        }
        ctx.fireUserEventTriggered(evt);
    }

    Optional<CompletableFuture> find(String id){
        return Optional.ofNullable(pending.get(id));
    }

    Optional<CompletableFuture> findAndRemove(String id){
        Optional<CompletableFuture> res = find(id);
        pending.remove(id);
        return res;
    }

    @RequiredArgsConstructor
    public static class RequestTimeoutEvent{
        private final String id;
    }
}
