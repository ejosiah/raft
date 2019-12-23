package com.josiahebhomenye.raft.client;

import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.event.CommitEvent;
import com.josiahebhomenye.raft.server.util.CheckedExceptionWrapper;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@ChannelHandler.Sharable
public class NodeKiller extends ChannelInboundHandlerAdapter implements Runnable, CheckedExceptionWrapper  {
    private static final Random RNG = new Random();
    final List<Node> nodes;
    final CountDownLatch latch;
    private Lock lock;
    private Condition commitCondition;
    private Condition killGuard;

    public NodeKiller(List<Node> nodes, CountDownLatch latch){
        this.nodes = new ArrayList<>(nodes);
        this.latch = latch;
        this.lock = new ReentrantLock();
        this.commitCondition = lock.newCondition();
        this.killGuard = lock.newCondition();
    }

    @Override
    @SneakyThrows
    public void run() {

        log.info("starting Node killer");

        while(latch.getCount() > 0){
            if (lock.tryLock()) {
                log.info("will wait on leader commit message");
                commitCondition.await();

                try {
                    Optional<Node> maybeLeader = nodes.stream().filter(node -> node.state().equals(NodeState.LEADER())).findFirst();

                    maybeLeader.ifPresent(node -> {
                        if(node.commitIndex() > 0) {
                            log.info("waiting for any pending messages to be sent to leader");
                           // uncheckVoid(() -> killGuard.await() );
                            log.info("taking {} offline", node);
                            uncheck(() -> node.stop().get());
                            uncheckVoid(() -> TimeUnit.SECONDS.sleep(10));
//                            log.info("restarting {}", node);
//                            node.restart();
                        }
                    });
                } catch (Exception e) {
                    log.warn(e.getMessage(), e);
                }finally {
                    lock.unlock();
                }
            }
        }
        log.info("Node killer going off line");
    }

    public void allClear(){
        killGuard.signal();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof CommitEvent){
            if(lock.tryLock()){
                try{
                    log.info("received leader commit message, proceeding on");
                    commitCondition.signal();
                }catch (IllegalMonitorStateException ex){
                    log.warn("when seem to have multiple threads owning the commit condition");
                }
                finally {
                    lock.unlock();
                }
            }
        }
        ctx.fireUserEventTriggered(ctx);
    }

    Node source(ChannelHandlerContext ctx){
        return (Node)ctx.channel().pipeline().context(Node.class).handler();
    }
}
