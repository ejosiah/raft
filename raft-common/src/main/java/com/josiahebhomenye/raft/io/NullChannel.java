package com.josiahebhomenye.raft.io;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.SocketAddress;

public class NullChannel implements Channel {

    public static final NullChannel NULL_CHANNEL = new NullChannel();

    private static final ChannelId ID = DefaultChannelId.newInstance();
    private static final EventLoop group = new DefaultEventLoop();
    private static final ChannelMetadata METADATA = new ChannelMetadata(false);
    private static final SocketAddress SOCKET_ADDRESS = new LocalAddress("NullChannel");
    private static  final DefaultChannelPromise NULL_FUTURE = new DefaultChannelPromise(NULL_CHANNEL);
    private static final ChannelPipeline CHANNEL_PIPELINE = new DefaultChannelPipeline(NULL_CHANNEL){};

    static {
        NULL_FUTURE.setSuccess();
    }

    private NullChannel(){}

    @Override
    public ChannelId id() {
        return ID;
    }

    @Override
    public EventLoop eventLoop() {
        return group;
    }

    @Override
    public Channel parent() {
        return null;
    }

    @Override
    public ChannelConfig config() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isRegistered() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public SocketAddress localAddress() {
        return SOCKET_ADDRESS;
    }

    @Override
    public SocketAddress remoteAddress() {
        return SOCKET_ADDRESS;
    }

    @Override
    public ChannelFuture closeFuture() {
        return NULL_FUTURE;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public long bytesBeforeUnwritable() {
        return 0;
    }

    @Override
    public long bytesBeforeWritable() {
        return 0;
    }

    @Override
    public Unsafe unsafe() {
        return null;
    }

    @Override
    public ChannelPipeline pipeline() {
        return CHANNEL_PIPELINE;
    }

    @Override
    public ByteBufAllocator alloc() {
        return UnpooledByteBufAllocator.DEFAULT;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture disconnect() {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture close() {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture deregister() {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return NULL_FUTURE;
    }

    @Override
    public Channel read() {
        return this;
    }

    @Override
    public ChannelFuture write(Object msg) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return NULL_FUTURE;
    }

    @Override
    public Channel flush() {
        return this;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return NULL_FUTURE;
    }

    @Override
    public ChannelPromise newPromise() {
        return NULL_FUTURE;
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return null;
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return NULL_FUTURE;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return new DefaultChannelPromise(NULL_CHANNEL).setFailure(cause);
    }

    @Override
    public ChannelPromise voidPromise() {
        return  new DefaultChannelPromise(NULL_CHANNEL);
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return null;
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return false;
    }

    @Override
    public int compareTo(Channel o) {
        return ID.compareTo(o.id());
    }
}
