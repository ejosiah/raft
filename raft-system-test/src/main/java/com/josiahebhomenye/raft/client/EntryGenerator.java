package com.josiahebhomenye.raft.client;

import com.josiahebhomenye.raft.client.config.ClientConfig;
import com.josiahebhomenye.raft.comand.*;
import com.josiahebhomenye.raft.comand.Data;
import com.typesafe.config.ConfigFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public class EntryGenerator implements Runnable {

    public static final Random RNG = new Random();

    private final Duration runTime;
    private final CountDownLatch testLatch;
    private final NodeKiller nodeKiller;
    private RaftClient<Command> client;
    private Data expected = new Data(0);

    private List<Class<? extends Command>> commandClasses = new ArrayList<Class<? extends Command>>(){
        {
            add(Set.class);
            add(Add.class);
            add(Subtract.class);
            add(Multiply.class);
            add(Divide.class);
        }
    };

    @Override
    @SneakyThrows
    public void run() {
        try {
            ArrayList<Command> commands = new ArrayList<>();
            client = new RaftClient<>(new ClientConfig(ConfigFactory.load()));
            client.start();
            long startTime = System.currentTimeMillis();
            log.info("Entry generator started");

            try {
                while(!endSim(startTime)){
                    IntStream.range(0, RNG.nextInt(10) + 1).forEach(i -> {
                        commands.add(nextCommand());
                    });
                    if(nodeKiller.getLock().tryLock()) {
                        try {
                            log.info("sending {} to {}", commands, client.channel().remoteAddress());
                            for (Command command : commands) {
                                client.send(command).get();
                            }
                            nodeKiller.allClear();
                        } finally {
                            nodeKiller.getLock().unlock();
                        }
                    }
                    TimeUnit.SECONDS.sleep(RNG.nextInt(10));
                }
            } catch (Exception e) {
                TimeUnit.SECONDS.sleep(RNG.nextInt(10));
                log.warn("send error", e);
            }
        }catch (Exception ex){
            log.error(ex.getMessage(), ex);
        }finally {
            testLatch.countDown();
            client.stop();
            log.info("Entry generator now shutdown");
        }
    }

    boolean endSim(long startTime){
        return (System.currentTimeMillis() - startTime) > runTime.toMillis();
    }

    @SneakyThrows
    private Command nextCommand(){
        int index = RNG.nextInt(commandClasses.size());
        int value = RNG.nextInt(10) + 1;

        Constructor<? extends Command> constructor = commandClasses.get(index).getDeclaredConstructor(int.class);

        return constructor.newInstance(value);
    }


}
