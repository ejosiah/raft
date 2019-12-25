package com.josiahebhomenye.test.support.profile;


import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LongSummaryStatistics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@Slf4j
public class Profiler {

  //  Logger log = LoggerFactory.getLogger(Profiler.class);

    private Task task;
    private int runs;
    private int warmup;
    private final ExecutorService executorService;

    public Profiler(Task task, int runs, int warmup){
        this.task = task;
        this.runs = runs;
        this.warmup = warmup;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }


    public Result run() throws Exception{
        long[] executionTimes = new long[runs];

        warmup(task);
        profile(task, executionTimes);

        executorService.shutdownNow();

        return compileStats(executionTimes);

    }

    private void warmup(Task task) throws Exception{
        if(warmup > 0) {
            log.info("warming up {} with {} runs", task.description(), warmup);
            CountDownLatch latch = new CountDownLatch(warmup);
            IntStream.rangeClosed(0, warmup).forEach(i -> {
                executorService.execute(() -> {
                    task.setup();
                    task.run();
                    task.tearDown();
                    latch.countDown();
                });
            });
            latch.await();
        }
    }

    private void profile(Task task, long[] executionTimes) throws Exception{
        log.info("profiling {}, going to run {} times", task.description(), runs);
        CountDownLatch latch = new CountDownLatch(runs);
        IntStream.range(0, runs).forEach(i -> {
            executorService.execute(() -> {
                task.setup();
                long startTime = System.nanoTime();
                task.run();
                executionTimes[i] = System.nanoTime() - startTime;
                latch.countDown();
            });
        });
        latch.await();
    }

    @Value
    @Accessors(fluent = true)
    public static class Result{
        double average;
        double stddev;
        long min;
        long max;

        public Result(double average, double stddev, long min, long max) {
            this.average = average;
            this.stddev = stddev;
            this.min = min;
            this.max = max;
        }
    }

    public static Result compileStats(long[] executionTimes) {
        LongSummaryStatistics stats = Arrays.stream(executionTimes).summaryStatistics();
        double stddev = calculateStandardDeviation(stats, executionTimes);
        return new Result(stats.getAverage(), stddev, stats.getMin(), stats.getMax());
    }

    public static double calculateStandardDeviation(LongSummaryStatistics stats, long[] executionTimes) {
        double avg = stats.getAverage();
        double n = stats.getCount();
        double variance =
                Arrays
                .stream(executionTimes)
                .mapToDouble((long l) -> (double)l ).reduce(0.0, (double acc, double x) -> acc + Math.pow(x - avg, 2) )/n;
        return Math.sqrt(variance);
    }

}
